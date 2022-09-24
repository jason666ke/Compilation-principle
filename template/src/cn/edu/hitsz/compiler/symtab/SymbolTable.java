package cn.edu.hitsz.compiler.symtab;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 符号表
 * <br>
 * 由于源语言比较简易, 加之 Java 中具有非常好用的通用数据结构类型, 本项目其实并不一定需要一个集中的 "符号表" 来存储源语言中的
 * <b>所有符号的所有信息</b>. 但为了切合理论课程教学, 提高实验实践技能的通用性, 我们按照一般编译器项目中符号表的设计设计了该符号表.
 * 其在代码中的作用可能并不明显, 但我们希望同学们可以借此体验符号表的设计思想.
 */
public class SymbolTable {

    Map<String, SourceCodeType> symbolTable = new HashMap<>();

    /**
     * 获取符号表中已有的条目
     *
     * @param text 符号的文本表示
     * @return 该符号在符号表中的条目
     * @throws RuntimeException 该符号在表中不存在
     */
    public SymbolTableEntry get(String text) {
        // 表中没有就抛异常
        if (!has(text)) {
            throw new RuntimeException("No such symbol in symbol table!");
        }
        else {
            return new SymbolTableEntry(text);
        }
    }

    /**
     * 在符号表中新增条目
     *
     * @param text 待加入符号表中的新符号的文本表示
     * @throws RuntimeException 该符号已在表中存在
     */
    public void add(String text) {
        // 如果已经在表中存在
        if (has(text)) {
            throw new RuntimeException();
        }
        else {
            SymbolTableEntry symbolTableEntry = new SymbolTableEntry(text);
            symbolTable.put(symbolTableEntry.getText(), symbolTableEntry.getType());
        }
    }

    /**
     * 判断符号表中有无条目
     *
     * @param text 待判断符号的文本表示
     * @return 该符号的条目是否位于符号表中
     */
    public boolean has(String text) {
        return symbolTable.containsKey(text);
    }

    /**
     * 获得符号表的所有条目以供 {@code dumpTable} 使用
     *
     * @return 符号表的所有条目
     */
    private Map<String, SymbolTableEntry> getAllEntries() {
        Map<String, SymbolTableEntry> symbolTableEntryMap = new HashMap<>();
        for (String key : symbolTable.keySet()) {
            SymbolTableEntry symbolTableEntry = new SymbolTableEntry(key);
            symbolTableEntryMap.put(key, symbolTableEntry);
        }
        return symbolTableEntryMap;
    }

    /**
     * 将符号表按格式输出
     *
     * @param path 输出文件路径
     */
    public void dumpTable(String path) {
        final var entriesInOrder = new ArrayList<>(getAllEntries().values());
        entriesInOrder.sort(Comparator.comparing(SymbolTableEntry::getText));

        final var lines = new ArrayList<String>();
        for (final var entry : entriesInOrder) {
            // null in %s will be "null"
            lines.add("(%s, %s)".formatted(entry.getText(), entry.getType()));
        }

        FileUtils.writeLines(path, lines);
    }
}

