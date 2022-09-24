package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    // 缓冲区，按行输入到这个缓冲区中
    private List<String> textContent = new LinkedList<>();

    /**
     * 从给予的路径中读取并加载文件内容
     * 按行读取文件内容
     * @param path 路径
     */
    public void loadFile(String path) throws IOException {
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法

        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        // 文本文件中每行数据
        String line;
        while ((line = br.readLine()) != null) {
            textContent.add(line);
        }
        br.close();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public static List<List<TokenKind>> tokenKindList = new ArrayList<>();
    public void run() {
        for (String text : textContent){
            // 利用写好的自动机对每一行文本进行分析
            // 返回行文本对应的tokenKind
            tokenKindList.add(LexicalAnalysisAutomata.textAnalyze(text));
        }
    }


    public static List<Token> tokenList = new ArrayList<>();
    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        for (int index = 0; index < tokenKindList.size(); index ++) {

            String text = textContent.get(index);
            List<TokenKind> textTokenKind = tokenKindList.get(index);

            // text中是按照空格将不同类型的文本隔开的
            String[] wordArray = StringUtils.split(text, " ;");

            for (int i = 0; i < wordArray.length; i++) {
                TokenKind curTokenKind = textTokenKind.get(i);
                Token curToken;
                // 如果是标识符或常数需要生成复杂的token
                if (curTokenKind.getTermName().equals("id") || curTokenKind.getTermName().equals("IntConst")) {
                    curToken= Token.normal(curTokenKind, wordArray[i]);
                    // 如果是标识符，更新符号表
                    if (curTokenKind.getTermName().equals("id")) {
                        updateSymbolTable(wordArray[i]);
                    }
                }
                // 否则则生成简单的token
                else {
                    curToken = Token.simple(curTokenKind);
                }
                tokenList.add(curToken);
            }
            // 还需要加上分号
            tokenList.add(Token.simple(textTokenKind.get(wordArray.length)));
        }
        // 最后加上eof
        tokenList.add(Token.eof());

        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }

    /**
     * 更新符号表
     * @param text 对应的标识符
     */
    public void updateSymbolTable(String text) {
        try {
            symbolTable.add(text);
        }catch (RuntimeException e) {
            System.out.println("Symbol Table Already Contains that key!");
        }
    }


}
