package com.moredrowsy.cfg;

public class Token {
    public final int index;
    public final int type;
    public final String sequence;

    public Token(int index, int type, String sequence) {
        super();
        this.index = index;
        this.type = type;
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return "i:" + this.index + "|t:" + this.type + "|s:" + this.sequence;
    }
}
