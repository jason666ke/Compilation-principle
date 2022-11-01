package cn.edu.hitsz.compiler.ir;

/**
 * IR 中的立即数
 */
public class IRImmediate implements IRValue {
    public static IRImmediate of(int value) {
        return new IRImmediate(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    /**
     * 为了代码简洁，在IRvalue中加入了抽象方法，这里只需实现空方法即可
     */
    public String getName() {
        return null;
    }

    private final int value;

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    private IRImmediate(int value) {
        this.value = value;
    }
}
