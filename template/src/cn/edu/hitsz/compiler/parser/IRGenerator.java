package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    // 存中间代码的List
    private List<Instruction> IRList = new ArrayList<>();

    // 存放value属性的栈
    private ArrayDeque<IRValue> valueStack = new ArrayDeque<>();

    // 定义空元素
    private IRVariable emptyElement = IRVariable.named("empty");

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // 将Token的text属性移入
        String variableName = currentToken.getText();
        // 如果是数字，则压入立即数变量
        if (StringUtils.isNumeric(variableName)) {
            valueStack.push(IRImmediate.of(Integer.parseInt(variableName)));
        } else {
            // 否则压入IR variable 变量
            valueStack.push(IRVariable.named(variableName));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 6 -> { // S -> id = E
                /* 生成赋值语句中间代码 */
                // 变量值
                IRValue value = valueStack.pop();
                // 跳过中间的空元素
                valueStack.pop();
                // 待赋值变量
                IRVariable result = (IRVariable) valueStack.pop();
                // 生成中间代码并加入IRList
                IRList.add(Instruction.createMov(result, value));
            }
            case 8 -> { // E -> E + A
                // 生成右操作数
                IRValue rhs = valueStack.pop();
                // 跳过空元素
                valueStack.pop();
                // 生成左操作数
                IRValue lhs = valueStack.pop();
                // 中间代码生成并加入IRList
                IRVariable temp = IRVariable.temp();
                IRList.add(Instruction.createAdd(temp, lhs, rhs));
                // 将中间代码计算的值入栈
                valueStack.push(temp);
            }
            case 9 -> { // E -> E - A
                // 生成右操作数
                IRValue rhs = valueStack.pop();
                // 跳过空元素
                valueStack.pop();
                // 生成左操作数
                IRValue lhs = valueStack.pop();

                // 中间代码生成并加入IRList
                IRVariable temp = IRVariable.temp();
                IRList.add(Instruction.createSub(temp, lhs, rhs));
                // 将中间代码计算的值入栈
                valueStack.push(temp);
            }
            case 11 -> { // A -> A * B
                // 生成右操作数
                IRValue rhs = valueStack.pop();
                // 跳过空元素
                valueStack.pop();
                // 生成左操作数
                IRValue lhs = valueStack.pop();
                // 中间代码生成并加入IRList
                IRVariable temp = IRVariable.temp();
                IRList.add(Instruction.createMul(temp, lhs, rhs));
                // 将中间代码计算的值入栈
                valueStack.push(temp);
            }
            case 10, 12, 15, 14 -> {
                // E -> A, A -> B, B-> IntConst
                // B -> id
                // 获得操作数 or 需要保留id的名字
                IRValue value = valueStack.pop();
                // 将值压回栈中
                valueStack.push(value);
            }
            case 7 -> { // S -> return E
                // 获得操作数的值
                IRValue result = valueStack.pop();
                // 中间代码生成并加入IRList
                IRList.add(Instruction.createRet(result));
            }
            case 13 -> { // B -> ( E )
                // 弹出右括号对应空元素
                valueStack.pop();
                // 记录对应的value值
                IRValue value = valueStack.pop();
                // 弹出左括号对应元素
                valueStack.pop();
                // 将value压回栈中
                valueStack.push(value);
            }
            default -> {
                // 弹出产生式body长度的空记录
                for (Term term : production.body()) {
                    // 当栈为空时，说明中间代码生成已经完成了，直接break即可
                    if (valueStack.size() == 0) {
                        break;
                    }
                    valueStack.pop();
                }
                // 压入产生式左部的空记录
                valueStack.push(emptyElement);
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // accept状态时什么都不用做
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // 用不到符号表
    }

    public List<Instruction> getIR() {
        // 返回生成好的IRList即可
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

