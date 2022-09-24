package cn.edu.hitsz.compiler.lexer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LexicalAnalysisAutomata {

    private static final List<String> keyWordList = new LinkedList<>();

    enum State {
        // 自动机开始状态
        start,
        // 标识符状态
        letter, idToken, keyWordToken,
        // Int型状态
        digit, intToken,
        // 乘号与立方状态
        mul, powerToken, mulToken,
        // 赋值与判断是否相等
        assign, assignToken, equalToken,
        // 常量状态
        leftQuotation, rightQuotation, constToken,
        // 左括号状态
        leftParenthesisToken,
        // 右括号状态
        rightParenthesisToken,
        // 分号状态
        semicolonToken,
        // 加号
        addToken,
        // 减号
        minusToken,
        // 除号
        divToken,
        // 逗号
        commaToken
    }

    // 判断自动机是否进入终止状态的标志变量
    private static boolean endFlag;

    // 当前状态
    private static State currentState;

    // 匹配的关键字
    private static String currentLetter = "";

    /**
     * 状态转换函数
     * @param cur_word 当前读入的字
     * @param next_word 当前字的下一个字
     */
    private static void stateTransition(char cur_word, char next_word) {

        switch (currentState) {
            /*
              第一列 三个状态
              start, letter, idToken
             */
            case start -> {
                // 字符状态
                if (Character.isLetter(cur_word)) {
                    // 单个字的标识符（单独处理）
                    if (Character.isDigit(next_word) || Character.isLetter(next_word)) {
                        currentState = State.letter;
                        // 关键字判断
                        currentLetter += cur_word;
                    }
                    else {
                        currentState = State.idToken;
                        endFlag = true;
                    }
                }
                // 数字状态
                else if (Character.isDigit(cur_word)) {
                    // 如果下一个还是数字，则说明数字没有读取完
                    if (Character.isDigit(next_word)) {
                        currentState = State.digit;
                    }
                    // 如果下一个是数字，则说明已经读取完成
                    else {
                        currentState = State.intToken;
                        endFlag = true;
                    }
                }
                else if (cur_word == '*') {
                    if (next_word == '*') {
                        currentState = State.powerToken;
                    }
                    else {
                        currentState = State.mulToken;
                    }
                    endFlag = true;
                }
                else if (cur_word == '=') {
                    if (next_word == '=') {
                        currentState = State.equalToken;
                    }
                    else {
                        currentState = State.assignToken;
                    }
                    endFlag = true;
                }
                // 左引号
                else if (cur_word == '"') {
                    currentState = State.leftQuotation;
                }
                else if (cur_word == '(') {
                    currentState = State.leftParenthesisToken;
                    endFlag = true;
                }
                else if (cur_word == ')') {
                    currentState = State.rightParenthesisToken;
                    endFlag = true;
                }
                else if (cur_word == ':') {
                    currentState = State.semicolonToken;
                    endFlag = true;
                }
                else if (cur_word == '+') {
                    currentState = State.addToken;
                    endFlag = true;
                }
                else if (cur_word == '-') {
                    currentState = State.minusToken;
                    endFlag = true;
                }
                else if (cur_word == '/') {
                    currentState = State.divToken;
                    endFlag = true;
                }
                else if (cur_word == ',') {
                    currentState = State.commaToken;
                    endFlag = true;
                }
                else if (cur_word == ';') {
                    currentState = State.semicolonToken;
                    endFlag = true;
                }
                // 其余读取到的其余字符则保持在start状态
                else {
                    currentState = State.start;
                }
            }
            case letter -> {
                // 不改变当前状态
                if (Character.isLetter(cur_word) || Character.isDigit(cur_word)) {
                    currentLetter += cur_word;
                    // 如果下一个字符还是字母或数字，则不进行关键字匹配
                    if (Character.isLetter(next_word) || Character.isDigit(next_word)) {
                        currentState = State.letter;
                    }
                    // 如果下一个字符既不是字母也不是数字（意味着标识符或关键字输入已经结束）
                    else{
                        // 先进行关键字匹配
                        for (String keyWord : keyWordList) {
                            if (keyWord.equals(currentLetter)) {
                                currentState = State.keyWordToken;
                                break;
                            }
                        }
                        // 若未匹配上，则说明是标识符
                        currentState = (currentState == State.keyWordToken) ? State.keyWordToken : State.idToken;
                        endFlag = true;
                    }
                }
            }
            /*
              第二列 两个状态
              digit intToken
             */
            case digit -> {
                if (Character.isDigit(cur_word)) {
                    currentState = State.digit;
                }
                else {
                    currentState = State.intToken;
                    endFlag = true;
                }
            }
            /*
              第三列 三个状态
              plus powerToken plusToken
             */
            case mul -> {
                if (cur_word == '*') {
                    currentState = State.powerToken;
                }
                else {
                    currentState = State.mulToken;
                }
                endFlag = true;
            }
            /*
              第四列 三个状态
              assign equalToke assignToken
             */
            case assign -> {
                if (cur_word == '=') {
                    currentState = State.equalToken;
                }
                else {
                    currentState = State.assignToken;
                }
                endFlag = true;
            }
            /*
              常量列 三个状态
             */
            case leftQuotation -> {
                if (cur_word == '"') {
                    currentState = State.rightQuotation;
                }
            }
            case rightQuotation -> {
                if (cur_word == '"') {
                    currentState = State.constToken;
                    endFlag = true;
                }
            }

            /*
              默认状态 保持当前状态不变
             */
            default -> {
            }
        }
    }

    /**
     * 关键字判断函数，如果有新的关键字加入，则直接在这里加就行了
     */
    public static void setKeyWordList() {
        if (keyWordList.isEmpty()) {
            keyWordList.add("int");
            keyWordList.add("return");
        }
    }

    /**
     * 完成词法分析过程的自动机
     * @param text 读进来的每一行文字
     * @return 对应的TokenList
     */
    public static List<TokenKind> textAnalyze(String text) {
        setKeyWordList();
        // 关键字在表中对应的id
        String id;
        // 状态初始化
        currentState = State.start;
        // 将字符串转化为一个一个单个的字母
        char[] wordArray = text.toCharArray();
        // 当前句子的分析结果
        List<TokenKind> tokenKindList = new ArrayList<>();
        // 遍历每一个字母
        for (int word_index = 0; word_index < wordArray.length; word_index++) {
            char cur_word;
            char next_word;
            // 如果还没遍历到最后一个字母
            if (word_index < wordArray.length - 1) {
                // 当前字母和下一个字母
                cur_word = wordArray[word_index];
                next_word = wordArray[word_index + 1];
            }
            else {
                cur_word = wordArray[word_index];  // 当前为最后一个字母
                next_word = wordArray[word_index]; // 同样为最后一个字母
            }

            // 当到达结束状态时
            if (endFlag) {
                // 判断结束状态种类
                id = judgeCurrentId(currentState);

                // 状态机初始化
                initializeAutomata();

                // 返回对应的TokenKind
                TokenKind curTokenKind = TokenKind.fromString(String.valueOf(id));
                tokenKindList.add(curTokenKind);

            }
            // 否则继续运行自动机
            stateTransition(cur_word, next_word);
        }

        // 单独判断最后一个字
        id = judgeCurrentId(currentState);
        initializeAutomata();

        // 返回对应的TokenKind
        TokenKind curTokenKind = TokenKind.fromString(String.valueOf(id));
        tokenKindList.add(curTokenKind);
        return tokenKindList;
    }

    /**
     * 判断最终输出的ID
     * @param currentState 当前状态
     * @return 返回对应的TokenKind
     */
    public static String judgeCurrentId(State currentState) {
        String id = null;
        // 判断结束状态种类
        switch (currentState) {
            case keyWordToken -> id = currentLetter;
            case equalToken -> id = "==";
            case commaToken -> id = ",";
            case semicolonToken -> id = "Semicolon";
            case addToken -> id = "+";
            case minusToken -> id = "-";
            case mulToken -> id = "*";
            case divToken -> id = "/";
            case leftParenthesisToken -> id = "(";
            case rightParenthesisToken -> id = ")";
            // 判断为标识符
            case idToken -> id = "id";
            // 常数
            case intToken -> id = "IntConst";
            case assignToken -> id = "=";
        }
        return id;
    }

    /**
     * 初始化自动机，每次进行完一次判断对自动机进行复位
     */
    public static void initializeAutomata() {
        currentState = State.start;
        currentLetter = "";
        endFlag = false;
    }

}
