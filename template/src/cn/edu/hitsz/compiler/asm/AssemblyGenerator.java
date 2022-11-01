package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    // 经过预处理的中间代码
    private List<Instruction> preProcessedInstructions = new ArrayList<>();
    public void loadIR(List<Instruction> originInstructions) {
        for (Instruction instruction : originInstructions) {
            InstructionKind kind = instruction.getKind();
            switch (kind) {
                case MOV -> {
                    // 对于MOV操作，不用修改
                    preProcessedInstructions.add(instruction);
                }
                case ADD -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    // 如果两个操作数都是立即数
                    if (lhs.isImmediate() && rhs.isImmediate()) {
                        IRValue sum = IRImmediate.of(Integer.parseInt(lhs.toString()) + Integer.parseInt(rhs.toString()));
                        preProcessedInstructions.add(Instruction.createMov(instruction.getResult(), sum));
                    }
                    // 如果是左操作数为立即数，右操作数是变量,交换位置
                    else if (lhs.isImmediate() && rhs.isIRVariable()) {
                        preProcessedInstructions.add(Instruction.createAdd(instruction.getResult(), rhs, lhs));
                    }
                    else {
                        preProcessedInstructions.add(instruction);
                    }
                }
                case SUB -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    // 如果两个操作数都是立即数
                    if (lhs.isImmediate() && rhs.isImmediate()) {
                        IRValue sub = IRImmediate.of(Integer.parseInt(lhs.toString()) - Integer.parseInt(rhs.toString()));
                        preProcessedInstructions.add(Instruction.createMov(instruction.getResult(), sub));
                    }
                    // 左立即数减法
                    else if (lhs.isImmediate() && rhs.isIRVariable()) {
                        // 前插一条指令 MOV a, imm
                        IRVariable temp = IRVariable.named("temp");
                        preProcessedInstructions.add(Instruction.createMov(temp, lhs));
                        preProcessedInstructions.add(Instruction.createSub(instruction.getResult(), temp, rhs));
                    }
                    else {
                        preProcessedInstructions.add(instruction);
                    }
                }
                case MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    // 如果两个操作数都是立即数
                    if (lhs.isImmediate() && rhs.isImmediate()) {
                        IRValue mul = IRImmediate.of(Integer.parseInt(lhs.toString()) * Integer.parseInt(rhs.toString()));
                        preProcessedInstructions.add(Instruction.createMov(instruction.getResult(), mul));
                    }
                    // 左立即数乘法
                    else if (lhs.isImmediate() && rhs.isIRVariable()) {
                        // 前插一条指令 MOV a, imm
                        IRVariable temp = IRVariable.named("temp");
                        preProcessedInstructions.add(Instruction.createMov(temp, lhs));
                        preProcessedInstructions.add(Instruction.createMul(instruction.getResult(), temp, rhs));
                    }
                    else {
                        preProcessedInstructions.add(instruction);
                    }
                }
                // 遇到RET直接舍弃后续所有指令
                case RET -> {
                    preProcessedInstructions.add(instruction);
                    return;
                }
            }
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    // 当前正在执行的指令
    private Instruction curExecutingInstruction;
    // 生成的汇编指令集合
    private List<AssemblyLanguage> assemblyLanguageList = new ArrayList<>();
    public void run() {
        for (Instruction instruction : preProcessedInstructions) {
            curExecutingInstruction = instruction;
            switch (instruction.getKind()) {
                case MOV -> {
                    IRValue from = instruction.getFrom().isImmediate() ? ((IRImmediate) instruction.getFrom()) : ((IRVariable)instruction.getFrom());
                    IRVariable result = instruction.getResult();
                    // MOV语句的右操作数为立即数
                    if (from.isImmediate()) {
                        Register resultReg = registerSelect(result, from.getValue());
                        // 生成汇编
                        assemblyLanguageList.add(AssemblyLanguage.createLi(resultReg, from));
                    }
                    // MOV语句的右操作数为变量
                    else {
                        // 找到存放from的reg
                        Register fromReg = Register.findByName(from.getName());
                        // 为result选择寄存器
                        Register resultReg = registerSelect(result, fromReg.getValue());
                        // 生成汇编
                        assemblyLanguageList.add(AssemblyLanguage.createMv(resultReg, fromReg));
                    }
                }
                case ADD, SUB, MUL-> {
                    // 经过预处理，左操作数一定是IR variable
                    IRVariable lhs = (IRVariable) instruction.getLHS();
                    IRValue rhs = instruction.getRHS().isImmediate() ? ((IRImmediate)instruction.getRHS()) : ((IRVariable) instruction.getRHS());
                    IRVariable result = instruction.getResult();
                    int value = 0;
                    // 如果右操作数是立即数
                    if (rhs.isImmediate()) {
                        // 左寄存器值 + 右立即数值
                        switch (instruction.getKind()) {
                            case ADD -> {
                                value = Register.findByName(lhs.getName()).getValue() + rhs.getValue();
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register resultReg = registerSelect(result, value);
                                assemblyLanguageList.add(AssemblyLanguage.createAddi(resultReg, lhsReg, rhs));
                            }
                            case SUB -> {
                                value = Register.findByName(lhs.getName()).getValue() - rhs.getValue();
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register resultReg = registerSelect(result, value);
                                assemblyLanguageList.add(AssemblyLanguage.createSubi(resultReg, lhsReg, rhs));
                            }
                            case MUL -> {
                                value = Register.findByName(lhs.getName()).getValue() * rhs.getValue();
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register resultReg = registerSelect(result, value);
                                assemblyLanguageList.add(AssemblyLanguage.createMuli(resultReg, lhsReg, rhs));
                            }
                        }
                    }
                    // 右操作数为变量
                    else {
                        // 左寄存器值 + 右寄存器值
                        switch (instruction.getKind()) {
                            case ADD -> {
                                value = Register.findByName(lhs.getName()).getValue() + Register.findByName(rhs.getName()).getValue();
                                Register resultReg = registerSelect(result, value);
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register rhsReg = Register.findByName(rhs.getName());
                                assemblyLanguageList.add(AssemblyLanguage.createAdd(resultReg, lhsReg, rhsReg));
                            }
                            case SUB -> {
                                value = Register.findByName(lhs.getName()).getValue() - Register.findByName(rhs.getName()).getValue();
                                Register resultReg = registerSelect(result, value);
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register rhsReg = Register.findByName(rhs.getName());
                                assemblyLanguageList.add(AssemblyLanguage.createSub(resultReg, lhsReg, rhsReg));
                            }
                            case MUL -> {
                                value = Register.findByName(lhs.getName()).getValue() * Register.findByName(rhs.getName()).getValue();
                                Register resultReg = registerSelect(result, value);
                                Register lhsReg = Register.findByName(lhs.getName());
                                Register rhsReg = Register.findByName(rhs.getName());
                                assemblyLanguageList.add(AssemblyLanguage.createMul(resultReg, lhsReg, rhsReg));
                            }
                        }
                    }
                }
                case RET -> {
                    Register returnReg = Register.findByName(instruction.getReturnValue().getName());
                    IRVariable result = IRVariable.named("result");
                    Register resultReg = registerSelect(result, returnReg.getValue());
                    assemblyLanguageList.add(AssemblyLanguage.createMv(resultReg, returnReg));
                }
            }
        }
    }

    /**
     * 为当前变量选取寄存器
     * @param variable 需要被存入寄存器的变量
     * @param value 变量对应取值
     */
    private Register registerSelect(IRVariable variable, int value) {
        // 对于返回语句
        if (curExecutingInstruction.getKind().equals(InstructionKind.RET)) {
            setRegister(Register.a0, variable, value);
            return Register.a0;
        }
        // 如果有空闲寄存器，则选择空闲寄存器
        for (Register reg : Register.values()) {
            // a0寄存器只用来存最终结果
            if (reg.equals(Register.a0)) {
                continue;
            }
            if (reg.isAvailability()) {
                setRegister(reg, variable, value);
                return reg;
            }
        }

        // 如果没有空闲寄存器，选择不再使用的变量所占有的寄存器

        // 记录当前正在执行的指令下标
        int curIndex = preProcessedInstructions.indexOf(curExecutingInstruction);
        // 每个寄存器中存放的变量进行后续语句的判断
        // 当找到一个后续语句都没有使用到的寄存器时，则可以夺取该寄存器
        for (Register reg : Register.values()) {
            // 可否夺取的标志变量
            boolean takingCapability = true;
            // 存放的变量名
            String curRegName = reg.getName();
            for (int i = curIndex; i < preProcessedInstructions.size(); i++) {
                Instruction judgeInstruction = preProcessedInstructions.get(i);
                switch (judgeInstruction.getKind()) {
                    case MOV -> {
                        IRValue from = judgeInstruction.getFrom();
                        String fromName = from.toString();
                        // 后续语句中有使用该变量，说明该寄存器不能替换
                        if (fromName.equals(curRegName)) {
                            takingCapability = false;
                        }
                    }
                    case ADD, SUB, MUL -> {
                        IRValue lhs = judgeInstruction.getLHS();
                        IRValue rhs = judgeInstruction.getRHS();
                        String lhsName = lhs.toString();
                        String rhsName = rhs.toString();
                        if (lhsName.equals(curRegName) || rhsName.equals(curRegName)) {
                            takingCapability = false;
                        }
                    }
                    case RET -> {
                        // 对于return语句，由于后面的语句都要丢弃，因此无需操作
                    }
                }
                // 若不可抢占，则不用判断后续语句了
                if (!takingCapability) {
                    break;
                }
            }
            // 判断完所有语句发现可抢占，则直接抢占
            if (takingCapability) {
                setRegister(reg, variable, value);
                return reg;
            }
        }
        throw new RuntimeException();
    }

    /**
     * 修改寄存器状态
     * @param register 待修改的寄存器
     * @param variable 待写入的变量
     * @param value 待写入的变量取值
     */
    private void setRegister(Register register, IRVariable variable, int value) {
        // 该寄存器存的变量名称
        register.setName(variable.getName());
        // 存入变量的值
        register.setValue(value);
        // 修改寄存器状态
        register.setAvailability(false);
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path, assemblyLanguageList.stream().map(AssemblyLanguage::toString).toList());
    }
}

