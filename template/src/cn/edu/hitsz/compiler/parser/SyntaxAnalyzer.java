package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    private Iterator<Token> tokenIterator;
    public void loadTokens(Iterable<Token> tokens) {

        // 将所有的token存入tokenList中
        List<Token> tokenList = new ArrayList<>();
        for (Token token : tokens) {
            tokenList.add(token);
        }

        // 将tokenList 转化为tokenIterator
        tokenIterator = tokenList.listIterator();

    }

    // 定义状态栈
    private ArrayDeque<Status> statusStack = new ArrayDeque<>();
    // 定义符号栈
    private ArrayDeque<Term> termStack = new ArrayDeque<>();

    // LR 分析表
    private LRTable table;

    // 当前状态与当前符号
    private Status currentStatus;
    private Term currentTerm;

    public void loadLRTable(LRTable table) {

        // 将LR table存到这个类中并将initStatus存起来
        this.table = table;
        statusStack.push(table.getInit());

        // 初始状态
        Status initStatus = table.getInit();

    }

    public void run() {
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作

        // 用于判断是否可以开始判断下一个token的标记变量
        boolean nextValid = true;

        // 当前状态
        Token currentToken = null;

        while (tokenIterator.hasNext()) {
            // 查了Goto表后缓冲区并没有压进栈，因此不能下一个
            if (nextValid) {
                currentToken = tokenIterator.next();
            }

            // 更新当前状态和当前符号
            updateCurrentStatusAndCurrentTerm();

            Action nextAction = table.getAction(currentStatus, currentToken);

            // 归约
            if (nextAction.getKind() == Action.ActionKind.Reduce) {

                // 更新符号栈
                Production production = nextAction.getProduction();
                // 将产生式右部的字符都弹出栈, 将产生式左部的字符压入栈
                updateTermStack(production);

                // 更新状态栈
                updateStatusStack(nextAction);

                // 更新当前状态和当前符号
                updateCurrentStatusAndCurrentTerm();

                // 调用观察者函数
                callWhenInReduce(currentStatus, production);

                // 更新状态变量
                nextValid = false;
            }

            // 移进
            else if (nextAction.getKind() == Action.ActionKind.Shift) {

                // 更新符号栈（将对应符号写入）
                termStack.push(currentToken.getKind());

                // 更新状态栈（将对应状态压入栈中）
                statusStack.push(nextAction.getStatus());

                // 更新当前状态和当前符号
                updateCurrentStatusAndCurrentTerm();

                // 调用观察者函数
                callWhenInShift(currentStatus, currentToken);

                // 更新标记变量
                nextValid = true;
            }
            else if (nextAction.getKind() == Action.ActionKind.Accept) {

                // 更新符号栈和状态站
                termStack.pop();

                // 更新当前状态和当前符号
                updateCurrentStatusAndCurrentTerm();

                callWhenInAccept(currentStatus);

                // 更新标记变量
                nextValid = true;
                break;
            }
        }
    }

    /**
     * 更新当前状态和当前符号
     */
    private void updateCurrentStatusAndCurrentTerm() {
        // 当前状态为状态栈的栈顶元素
        currentStatus = statusStack.peek();
        // 当前符号为符号栈的栈顶元素
        currentTerm = termStack.peek();
    }

    /**
     * 更新符号栈
     */
    private void updateTermStack(Production production) {
        // 将产生式右部的字符都弹出栈
        for (Term term: production.body()) {
            termStack.pop();
        }
        // 将产生式左部的字符压入栈
        termStack.push(production.head());

        // 更新状态站和符号栈
        updateCurrentStatusAndCurrentTerm();
    }

    /**
     * 更新状态栈
     */
    private void updateStatusStack(Action action) {
        Production production = action.getProduction();
        // 弹出对应状态
        for (Term term : production.body()) {
            statusStack.pop();
        }

        // 更新当前状态和当前栈顶状态
        updateCurrentStatusAndCurrentTerm();

        // 加入新添加的状态
        Status nextStatus = table.getGoto(currentStatus, (NonTerminal) currentTerm);
        statusStack.push(nextStatus);

        // 更新当前状态和当前栈顶状态
        updateCurrentStatusAndCurrentTerm();
    }
}
