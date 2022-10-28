package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    // 存放符号type属性
    private ArrayDeque<String> typeStack = new ArrayDeque<>();

    // 定义空元素为-1
    private int emptyElement = -1;
    // 符号表
    private SymbolTable symbolTable = null;

    @Override
    public void whenAccept(Status currentStatus) {
        // 当接受时，符号表已经更新完了，什么都不做即可
    }

    // 语义栈栈顶元素
    private String curType;
    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 4 -> { // S -> D id;
                /* 更新符号表中相应变量的type信息 */

                // 先出栈右侧符号的属性
                for (int i = 0; i < production.body().size() - 1; i++) {
                    typeStack.pop();
                }

                // 此时符号表对应变量的type属性应该在栈顶
                curType = typeStack.pop();

                if (curType.equals("int")) {
                    String symbolName = symbolTable.getFirstNullTypeSymbolName();
                    symbolTable.setSelectedSymbolType(symbolName, SourceCodeType.Int);
                }
                typeStack.push(String.valueOf(emptyElement));
            }
            case 5 -> { // D -> int
                /* 将D的属性type入栈 */
                String type = typeStack.pop();
                typeStack.push(type);
            }
            default -> {
                /* 其他产生式，直接压入空记录占位 */
                for (Term body: production.body()) {
                    typeStack.pop();
                }
                typeStack.push(String.valueOf(emptyElement));
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // 当做移进动作时，直接向符号栈内压入Token对应符号
        Token curToken = currentToken;
        typeStack.push(String.valueOf(currentToken.getKind()));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储

        // 首先将外部的symbolTable存进来
        // 这个时候的symbolTable是词法分析得到的old_symbol_table
        if (symbolTable == null) {
            symbolTable = table;
        }
    }
}

