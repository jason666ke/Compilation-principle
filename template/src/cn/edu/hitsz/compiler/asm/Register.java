package cn.edu.hitsz.compiler.asm;

import java.util.List;

public enum Register {
    t0("t0", 0,true),
    t1("t1", 0,true),
    t2("t2", 0,true),
    t3("t3", 0,true),
    t4("t4", 0,true),
    t5("t5", 0,true),
    t6("t6", 0,true),
    a0("a0", 0,true);


    private String name;
    private int value;
    private boolean availability;

    Register(String name, int value, boolean availability) {
        this.name = name;
        this.value = value;
        this.availability = availability;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isAvailability() {
        return availability;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    public static Register findByName(String name) {
        for (Register reg : Register.values()) {
            if (reg.name.equals(name)) {
                return reg;
            }
        }
        throw new NullPointerException();
    }

}
