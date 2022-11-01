package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AssemblyLanguage {

    // 汇编属性：指令种类，指令结果寄存器，操作数
    private AssemblyLanguageKind kind;
    private Register resultReg;
    private List<Register> operandsReg;

    // 专门用于存在立即数的指令
    private IRValue imm;

    // 判断指令是否含立即数
    private boolean containImm;

    // 构造函数
    private AssemblyLanguage(AssemblyLanguageKind kind, Register resultReg, List<Register> operandsReg) {
        this.kind = kind;
        this.resultReg = resultReg;
        this.operandsReg = operandsReg;
    }

    // 用于LI
    private AssemblyLanguage(AssemblyLanguageKind kind, Register resultReg, IRValue imm) {
        this.kind = kind;
        this.resultReg = resultReg;
        this.imm = imm;
        this.containImm = true;
    }

    // 用于存在立即数的二元函数
    private AssemblyLanguage(AssemblyLanguageKind kind, Register resultReg, Register lhs, IRValue imm) {
        this.kind = kind;
        this.resultReg = resultReg;
        this.operandsReg = List.of(lhs);
        this.imm = imm;
        this.containImm = true;
    }

    public static AssemblyLanguage createAdd(Register resultRegister, Register lhs, Register rhs) {
        return new AssemblyLanguage(AssemblyLanguageKind.add, resultRegister, List.of(lhs, rhs));
    }

    public static AssemblyLanguage createAddi(Register result, Register lhs, IRValue imm) {
        return new AssemblyLanguage(AssemblyLanguageKind.addi, result, lhs, imm);
    }

    public static AssemblyLanguage createSub(Register result, Register lhs, Register rhs) {
        return new AssemblyLanguage(AssemblyLanguageKind.sub, result, List.of(lhs, rhs));
    }

    public static AssemblyLanguage createSubi(Register result, Register lhs, IRValue imm) {
        return new AssemblyLanguage(AssemblyLanguageKind.subi, result, lhs, imm);
    }

    public static AssemblyLanguage createMul(Register result, Register lhs, Register rhs) {
        return new AssemblyLanguage(AssemblyLanguageKind.mul, result, List.of(lhs, rhs));
    }

    public static AssemblyLanguage createMuli(Register result, Register lhs, IRValue imm) {
        return new AssemblyLanguage(AssemblyLanguageKind.muli, result, lhs, imm);
    }

    public static AssemblyLanguage createLi(Register result, IRValue from) {
        return new AssemblyLanguage(AssemblyLanguageKind.li, result, from);
    }

    public static AssemblyLanguage createMv(Register result, Register from) {
        return new AssemblyLanguage(AssemblyLanguageKind.mv, result, List.of(from));
    }

    @Override
    public String toString() {
        final var kindString = kind.toString();
        final var resultString = resultReg.toString();
        final String operandsString;
        // 指令中存在立即数
        if (containImm) {
            if (kind.equals(AssemblyLanguageKind.li)) {
                operandsString = Integer.toString(imm.getValue());
            }
            // 一个寄存器，一个立即数
            else {
                operandsString = operandsReg.get(0).toString() + ", " + imm.toString();
            }
        }
        // 指令不存在立即数
        else {
            operandsString = operandsReg.stream().map(Objects::toString).collect(Collectors.joining(", "));
        }
        return "%s %s, %s".formatted(kindString, resultString, operandsString);
    }
}
