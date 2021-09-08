package com.moredrowsy.cfg;

import java.util.ArrayList;

public class Node<T> {
    T val;
    String type;
    ArrayList<Token> tokens;
    ArrayList<Node<T>> children;
    ArrayList<Node<T>> parents;

    Node() {
        this.val = null;
        this.type = null;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    Node(T val) {
        this.val = val;
        this.type = null;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    Node(T val, String type) {
        this.val = val;
        this.type = type;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    Node(T val, Node<T> node, ArrayList<Node<T>> parents) {
        this.val = val;
        this.type = null;
        this.tokens = new ArrayList<>();
        this.children = new ArrayList<>();
        this.children.add(node);
        this.parents = parents;
    }

    Node(T val, ArrayList<Node<T>> children, ArrayList<Node<T>> parents) {
        this.val = val;
        this.type = null;
        this.tokens = new ArrayList<>();
        this.children = children;
        this.parents = parents;
    }

    @Override
    public String toString() {
        return "v: " + this.val + ", t: " + this.type + ", s: " + this.tokensToString();
    }

    private String tokensToString() {
        String str;
        int size = this.tokens.size();

        if (size == 0)
            str = "NaN";
        else
            str = "" + this.tokens.get(0);

        return str;
    }
}
