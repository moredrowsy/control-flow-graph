package com.moredrowsy.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Parser {
    private static enum TokenStates {
        ERROR, INIT_START, FUNCTION, STATEMENT, SEMICOLON, WHILE, DO, FOR, IF, ELSE, PAREN_OPEN, PAREN_CLOSE, BRACE_OPEN, BRACE_CLOSE, LAMBDA,
    }

    private static enum FSMStates {
        ERROR,

        INIT_START,

        // STATEMENT STATES
        STATEMENT_START, STATEMENT_MID, STATEMENT_END,

        // IF-THEN-ELSE
        IF_START, IF_PAREN_OPEN, IF_PAREN_STATEMENT, IF_PAREN_CLOSE, IF_THEN_BRACE_OPEN, IF_THEN_STATEMENT, IF_THEN_SINGLE_STATEMENT, IF_THEN_END, //
        ELSE_IF_STATEMENT, ELSE_IF_END, //
        IF_ELSE, IF_ELSE_BRACE_OPEN, IF_ELSE_STATEMENT, IF_ELSE_SINGLE_STATEMENT, IF_ELSE_END, //

        // WHILE STATES
        WHILE_START, WHILE_PAREN_OPEN, WHILE_PAREN_STATEMENT, WHILE_PAREN_CLOSE, WHILE_BRACE_OPEN, WHILE_STATEMENT, WHILE_SINGLE_STATEMENT, WHILE_END,

        // DO_WHILE STATES
        DO_WHILE_START, DO_WHILE_BRACE_OPEN, DO_WHILE_STATEMENT, DO_WHILE_BRACE_CLOSE, DO_WHILE_KEYWORD, DO_WHILE_PAREN_OPEN, DO_WHILE_PAREN_STATEMENT, DO_WHILE_PAREN_CLOSE, DO_WHILE_END,

        // FOR LOOPS
        FOR_START, FOR_PAREN_OPEN, FOR_INIT, FOR_INIT_END, FOR_COND, FOR_COND_END, FOR_MODIFY, FOR_PAREN_CLOSE, FOR_BRACE_OPEN, FOR_STATEMENT, FOR_SINGLE_STATEMENT, FOR_END,

        // FUNCTION STATES
        FUNC_START, FUNC_BRACE_OPEN, FUNC_STATEMENT, FUNC_END,
    }

    private static enum DecompStates {
        C1, C1_END, // CASE
        D0, D0_END, // IF-THEN
        D1, D1_END, // IF-THEN-ELSE
        D2, D2_END, // WHILE-DO
        D3, D3_END, // DO-WHILE
        F1, F1_END, // FUNCTION
        P1, // STATEMENT
    }

    private Tokenizer tokenizer; // Tokenize the strings
    private LinkedList<Token> tokens; // Tokens from tokenizer's result
    private ArrayList<String> strings; // List of strings to tokenize
    private ArrayList<Node<Integer>> nodes; // List of tree Nodes parsed from tokens
    private int[][] states;

    Parser() {
        tokenizer = new Tokenizer();
        tokens = new LinkedList<>();
        strings = new ArrayList<>();
        nodes = new ArrayList<>();
        initTokenizer();
        initStates();
    }

    private void initTokenizer() {
        // Add rules to tokenizer
        tokenizer.add(";", TokenStates.SEMICOLON.ordinal());
        tokenizer.add("if", TokenStates.IF.ordinal());
        tokenizer.add("else", TokenStates.ELSE.ordinal());
        tokenizer.add("while", TokenStates.WHILE.ordinal());
        tokenizer.add("do", TokenStates.DO.ordinal());
        tokenizer.add("for", TokenStates.FOR.ordinal());
        tokenizer.add("[^\\(\\)\\;\\{\\}]*[\\s]*[^\\(\\)\\;\\{\\}]+\\([^\\(\\)\\;\\{\\}]*\\)",
                TokenStates.FUNCTION.ordinal());
        tokenizer.add("\\(", TokenStates.PAREN_OPEN.ordinal());
        tokenizer.add("\\)", TokenStates.PAREN_CLOSE.ordinal());
        tokenizer.add("\\{", TokenStates.BRACE_OPEN.ordinal());
        tokenizer.add("\\}", TokenStates.BRACE_CLOSE.ordinal());
        tokenizer.add("[^\\(\\)\\{\\}\\;]+", TokenStates.STATEMENT.ordinal());
    }

    private void initStates() {
        // Init 2d states for Finite State Machine
        states = new int[FSMStates.values().length][TokenStates.values().length];

        // Mark initial start states
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.ERROR.ordinal(),
                FSMStates.ERROR.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.STATEMENT_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.STATEMENT_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.WHILE_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.DO_WHILE_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FOR_START.ordinal());
        addStateRules(FSMStates.INIT_START.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FUNC_START.ordinal());

        // Mark Finite State Table
        initStatementStates();
        initWhileStates();
        initDoWhileStates();
        initIfStates();
        initForStates();
        initFunctionStates();
    }

    private void initStatementStates() {
        // STATEMENT_START -> STATEMENT -> STATEMENT_MID
        addStateRules(FSMStates.STATEMENT_START.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.STATEMENT_MID.ordinal());
        // STATEMENT_MID -> STATEMENT -> STATEMENT_MID
        addStateRules(FSMStates.STATEMENT_MID.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.STATEMENT_MID.ordinal());
        // STATEMENT_MID -> SEMICOLON -> STATEMENT_END
        addStateRules(FSMStates.STATEMENT_MID.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.STATEMENT_END.ordinal());
        // STATEMENT_START -> SEMICOLON -> STATEMENT_END
        addStateRules(FSMStates.STATEMENT_START.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.STATEMENT_END.ordinal());
        // STATEMENT_END -> STATEMENT -> STATEMENT_MID
        addStateRules(FSMStates.STATEMENT_END.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.STATEMENT_MID.ordinal());
        // STATEMENT_END -> SEMICOLON -> STATEMENT_MID
        addStateRules(FSMStates.STATEMENT_END.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.STATEMENT_END.ordinal());
    }

    private void initWhileStates() {
        // WHILE_START -> PAREN_OPEN -> WHILE_PAREN_OPEN
        addStateRules(FSMStates.WHILE_START.ordinal(), TokenStates.PAREN_OPEN.ordinal(),
                FSMStates.WHILE_PAREN_OPEN.ordinal());
        // WHILE_PAREN_OPEN -> STATEMENT -> WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.WHILE_PAREN_STATEMENT.ordinal());
        // WHILE_PAREN_STATEMENT -> STATEMENT -> WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.WHILE_PAREN_STATEMENT.ordinal());
        // WHILE_PAREN_STATEMENT -> STATEMENT -> WHILE_PAREN_CLOSE
        addStateRules(FSMStates.WHILE_PAREN_STATEMENT.ordinal(), TokenStates.PAREN_CLOSE.ordinal(),
                FSMStates.WHILE_PAREN_CLOSE.ordinal());
        // WHILE_PAREN_STATEMENT -> DO -> WHILE_PAREN_CLOSE
        addStateRules(FSMStates.WHILE_PAREN_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.WHILE_PAREN_CLOSE.ordinal());
        // WHILE_PAREN_CLOSE -> BRACE_OPEN -> WHILE_BRACE_OPEN
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.WHILE_BRACE_OPEN.ordinal());
        // WHILE_PAREN_CLOSE -> SEMICOLON -> WHILE_END
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.WHILE_END.ordinal());
        // WHILE_PAREN_CLOSE -> STATEMENT -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_PAREN_CLOSE -> IF -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_PAREN_CLOSE -> WHILE -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_PAREN_CLOSE -> DO -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_PAREN_CLOSE -> FOR -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_PAREN_CLOSE -> FUNCTION -> WHILE_SINGLE_STATEMENT
        addStateRules(FSMStates.WHILE_PAREN_CLOSE.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.WHILE_SINGLE_STATEMENT.ordinal());
        // WHILE_SINGLE_STATEMENT -> LAMBDA -> WHILE_END
        addStateRules(FSMStates.WHILE_SINGLE_STATEMENT.ordinal(), TokenStates.LAMBDA.ordinal(),
                FSMStates.WHILE_END.ordinal());
        // WHILE_BRACE_OPEN -> BRACE_CLOSE -> WHILE_END
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.WHILE_END.ordinal());
        // WHILE_BRACE_OPEN -> SEMICOLON -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> STATEMENT -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> IF -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> WHILE -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> DO -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> FOR -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_BRACE_OPEN -> FUNCTION -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> SEMICOLON -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> STATEMENT -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> IF -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> WHILE -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> DO -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> FOR -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> FUNCTION -> WHILE_STATEMENT
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.WHILE_STATEMENT.ordinal());
        // WHILE_STATEMENT -> BRACE_CLOSE -> WHILE_END
        addStateRules(FSMStates.WHILE_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.WHILE_END.ordinal());
    }

    private void initDoWhileStates() {
        // DO_WHILE_START -> BRACE_OPEN -> DO_WHILE_BRACE_OPEN
        addStateRules(FSMStates.DO_WHILE_START.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.DO_WHILE_BRACE_OPEN.ordinal());
        // DO_WHILE_BRACE_OPEN -> BRACE_CLOSE -> DO_WHILE_BRACE_CLOSE
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.DO_WHILE_BRACE_CLOSE.ordinal());
        // DO_WHILE_BRACE_OPEN -> SEMICOLON -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> BRACE_OPEN -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> IF -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> WHILE -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> DO -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> FOR -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_BRACE_OPEN -> FUNCTION -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> SEMICOLON -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> STATEMENT -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> IF -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> WHILE -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> DO -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> FOR -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> FUNCTION -> DO_WHILE_STATEMENT
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.DO_WHILE_STATEMENT.ordinal());
        // DO_WHILE_STATEMENT -> BRACE_CLOSE -> DO_WHILE_BRACE_CLOSE
        addStateRules(FSMStates.DO_WHILE_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.DO_WHILE_BRACE_CLOSE.ordinal());
        // DO_WHILE_BRACE_CLOSE -> WHILE -> DO_WHILE_KEYWORD
        addStateRules(FSMStates.DO_WHILE_BRACE_CLOSE.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.DO_WHILE_KEYWORD.ordinal());
        // DO_WHILE_KEYWORD -> PAREN_OPEN -> DO_WHILE_PAREN_OPEN
        addStateRules(FSMStates.DO_WHILE_KEYWORD.ordinal(), TokenStates.PAREN_OPEN.ordinal(),
                FSMStates.DO_WHILE_PAREN_OPEN.ordinal());
        // DO_WHILE_PAREN_OPEN -> SEMICOLON -> DO_WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.DO_WHILE_PAREN_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal());
        // DO_WHILE_PAREN_OPEN -> STATEMENT -> DO_WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.DO_WHILE_PAREN_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal());
        // DO_WHILE_PAREN_STATEMENT -> SEMICOLON -> DO_WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal());
        // DO_WHILE_PAREN_STATEMENT -> STATEMENT -> DO_WHILE_PAREN_STATEMENT
        addStateRules(FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal());
        // DO_WHILE_PAREN_STATEMENT -> PAREN_CLOSE -> DO_WHILE_PAREN_CLOSE
        addStateRules(FSMStates.DO_WHILE_PAREN_STATEMENT.ordinal(),
                TokenStates.PAREN_CLOSE.ordinal(), FSMStates.DO_WHILE_PAREN_CLOSE.ordinal());
        // DO_WHILE_PAREN_CLOSE -> DO_WHILE_PAREN_STATEMENT -> DO_WHILE_END
        addStateRules(FSMStates.DO_WHILE_PAREN_CLOSE.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.DO_WHILE_END.ordinal());
    }

    private void initIfStates() {
        // PART 1 - IF_THEN

        // IF_START -> PAREN_OPEN -> IF_PAREN_OPEN
        addStateRules(FSMStates.IF_START.ordinal(), TokenStates.PAREN_OPEN.ordinal(),
                FSMStates.IF_PAREN_OPEN.ordinal());
        // IF_START -> SEMICOLON -> IF_THEN_END
        addStateRules(FSMStates.IF_START.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_THEN_END.ordinal());
        // IF_PAREN_OPEN -> STATEMENT -> IF_PAREN_STATEMENT
        addStateRules(FSMStates.IF_PAREN_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_PAREN_STATEMENT.ordinal());
        // IF_PAREN_STATEMENT -> STATEMENT -> IF_PAREN_STATEMENT
        addStateRules(FSMStates.IF_PAREN_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_PAREN_STATEMENT.ordinal());
        // IF_PAREN_STATEMENT -> PAREN_CLOSE -> IF_PAREN_CLOSE
        addStateRules(FSMStates.IF_PAREN_STATEMENT.ordinal(), TokenStates.PAREN_CLOSE.ordinal(),
                FSMStates.IF_PAREN_CLOSE.ordinal());
        // IF_PAREN_CLOSE -> BRACE_OPEN -> IF_THEN_BRACE_OPEN
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.IF_THEN_BRACE_OPEN.ordinal());
        // IF_PAREN_CLOSE -> SEMICOLON -> IF_THEN_END
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_THEN_END.ordinal());
        // IF_PAREN_CLOSE -> STATEMENT -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_PAREN_CLOSE -> IF -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_PAREN_CLOSE -> WHILE -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_PAREN_CLOSE -> DO -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_PAREN_CLOSE -> FOR -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_PAREN_CLOSE -> FUNCTION -> IF_THEN_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal());
        // IF_THEN_SINGLE_STATEMENT -> ELSE -> IF_ELSE
        addStateRules(FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal(), TokenStates.ELSE.ordinal(),
                FSMStates.IF_ELSE.ordinal());
        // IF_PAREN_CLOSE -> BRACE_OPEN -> IF_THEN_BRACE_OPEN
        addStateRules(FSMStates.IF_PAREN_CLOSE.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.IF_THEN_BRACE_OPEN.ordinal());
        // IF_THEN_BRACE_OPEN -> BRACE_CLOSE -> IF_THEN_END
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.IF_THEN_END.ordinal());
        // IF_THEN_BRACE_OPEN -> SEMICOLON -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> STATEMENT -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> IF -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> WHILE -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> DO -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> FOR -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_BRACE_OPEN -> FUNCTION -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> SEMICOLON -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> STATEMENT -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> IF -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> WHILE -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> DO -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> FOR -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> FUNCTION -> IF_THEN_STATEMENT
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_THEN_STATEMENT.ordinal());
        // IF_THEN_STATEMENT -> BRACE_CLOSE -> IF_THEN_END
        addStateRules(FSMStates.IF_THEN_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.IF_THEN_END.ordinal());

        // Part 2 - ELSE_IF

        // IF_ELSE -> IF -> ELSE_IF_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.ELSE_IF_STATEMENT.ordinal());
        // ELSE_IF_STATEMENT -> LAMBDA -> ELSE_IF_END
        addStateRules(FSMStates.ELSE_IF_STATEMENT.ordinal(), TokenStates.LAMBDA.ordinal(),
                FSMStates.ELSE_IF_END.ordinal());
        // ELSE_IF_END -> ELSE -> IF_ELSE
        addStateRules(FSMStates.ELSE_IF_STATEMENT.ordinal(), TokenStates.ELSE.ordinal(),
                FSMStates.IF_ELSE.ordinal());

        // PART 3 - IF_ELSE

        // IF_THEN_END -> ELSE -> IF_ELSE
        addStateRules(FSMStates.IF_THEN_END.ordinal(), TokenStates.ELSE.ordinal(),
                FSMStates.IF_ELSE.ordinal());
        // IF_ELSE -> SEMICOLON -> IF_ELSE_END
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_ELSE_END.ordinal());
        // IF_ELSE -> STATEMENT -> IF_ELSE_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal());
        // IF_ELSE -> WHILE -> IF_ELSE_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal());
        // IF_ELSE -> DO -> IF_ELSE_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal());
        // IF_ELSE -> FOR -> IF_ELSE_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal());
        // IF_ELSE -> FUNCTION -> IF_ELSE_SINGLE_STATEMENT
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal());
        // IF_ELSE_SINGLE_STATEMENT -> LAMBDA -> IF_ELSE_END
        addStateRules(FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal(), TokenStates.LAMBDA.ordinal(),
                FSMStates.IF_ELSE_END.ordinal());
        // IF_ELSE -> BRACE_OPEN -> IF_ELSE_BRACE_OPEN
        addStateRules(FSMStates.IF_ELSE.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.IF_ELSE_BRACE_OPEN.ordinal());
        // IF_ELSE_BRACE_OPEN -> BRACE_CLOSE -> IF_ELSE_END
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.IF_ELSE_END.ordinal());
        // IF_ELSE_BRACE_OPEN -> SEMICOLON -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> STATEMENT -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> IF -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> WHILE -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> DO -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> FOR -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_BRACE_OPEN -> FUNCTION -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> SEMICOLON -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> STATEMENT -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> IF -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> WHILE -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> DO -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> FOR -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> FUNCTION -> IF_ELSE_STATEMENT
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.IF_ELSE_STATEMENT.ordinal());
        // IF_ELSE_STATEMENT -> BRACE_CLOSE -> IF_ELSE_END
        addStateRules(FSMStates.IF_ELSE_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.IF_ELSE_END.ordinal());
    }

    private void initForStates() {
        // FOR_START -> PAREN_OPEN -> FOR_PAREN_OPEN
        addStateRules(FSMStates.FOR_START.ordinal(), TokenStates.PAREN_OPEN.ordinal(),
                FSMStates.FOR_PAREN_OPEN.ordinal());
        // FOR_PAREN_OPEN -> SEMICOLON -> FOR_INIT_END
        addStateRules(FSMStates.FOR_PAREN_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_INIT_END.ordinal());
        // FOR_PAREN_OPEN -> STATEMENT -> FOR_INIT
        addStateRules(FSMStates.FOR_PAREN_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_INIT.ordinal());
        // FOR_INIT -> SEMICOLON -> FOR_INIT_END
        addStateRules(FSMStates.FOR_INIT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_INIT_END.ordinal());
        // FOR_INIT_END -> SEMICOLON -> FOR_COND_END
        addStateRules(FSMStates.FOR_INIT_END.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_COND_END.ordinal());
        // FOR_INIT_END -> STATEMENT -> FOR_COND
        addStateRules(FSMStates.FOR_INIT_END.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_COND.ordinal());
        // FOR_COND -> SEMICOLON -> FOR_COND_END
        addStateRules(FSMStates.FOR_COND.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_COND_END.ordinal());
        // FOR_COND_END -> PAREN_CLOSE -> FOR_PAREN_CLOSE
        addStateRules(FSMStates.FOR_COND_END.ordinal(), TokenStates.PAREN_CLOSE.ordinal(),
                FSMStates.FOR_PAREN_CLOSE.ordinal());
        // FOR_COND_END -> STATEMENT -> FOR_MODIFY
        addStateRules(FSMStates.FOR_COND_END.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_MODIFY.ordinal());
        // FOR_MODIFY -> PAREN_CLOSE -> FOR_PAREN_CLOSE
        addStateRules(FSMStates.FOR_MODIFY.ordinal(), TokenStates.PAREN_CLOSE.ordinal(),
                FSMStates.FOR_PAREN_CLOSE.ordinal());
        // FOR_PAREN_CLOSE -> SEMICOLON -> FOR_END
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_END.ordinal());
        // FOR_PAREN_CLOSE -> STATEMENT -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_PAREN_CLOSE -> IF -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_PAREN_CLOSE -> WHILE -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_PAREN_CLOSE -> DO -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_PAREN_CLOSE -> FOR -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_PAREN_CLOSE -> FUNCTION -> FOR_SINGLE_STATEMENT
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FOR_SINGLE_STATEMENT.ordinal());
        // FOR_SINGLE_STATEMENT -> LAMBDA -> FOR_END
        addStateRules(FSMStates.FOR_SINGLE_STATEMENT.ordinal(), TokenStates.LAMBDA.ordinal(),
                FSMStates.FOR_END.ordinal());
        // FOR_PAREN_CLOSE -> BRACE_OPEN -> FOR_BRACE_OPEN
        addStateRules(FSMStates.FOR_PAREN_CLOSE.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.FOR_BRACE_OPEN.ordinal());
        // FOR_BRACE_OPEN -> BRACE_CLOSE -> FOR_END
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.FOR_END.ordinal());
        // FOR_BRACE_OPEN -> SEMICOLON -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> STATEMENT -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> IF -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> WHILE -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> DO -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> FOR -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_BRACE_OPEN -> FUNCTION -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> SEMICOLON -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> STATEMENT -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> IF -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> WHILE -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> DO -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> FOR -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> FUNCTION -> FOR_STATEMENT
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FOR_STATEMENT.ordinal());
        // FOR_STATEMENT -> BRACE_CLOSE -> FOR_END
        addStateRules(FSMStates.FOR_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.FOR_END.ordinal());
    }

    private void initFunctionStates() {
        // FUNC_START -> BRACE_OPEN -> FUNC_BRACE_OPEN
        addStateRules(FSMStates.FUNC_START.ordinal(), TokenStates.BRACE_OPEN.ordinal(),
                FSMStates.FUNC_BRACE_OPEN.ordinal());
        // FUNC_START -> SEMICOLON -> FUNC_END
        addStateRules(FSMStates.FUNC_START.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FUNC_END.ordinal());
        // FUNC_BRACE_OPEN -> BRACE_CLOSE -> FUNC_END
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.FUNC_END.ordinal());
        // FUNC_BRACE_OPEN -> SEMICOLON -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> STATEMENT -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> IF -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> WHILE -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> DO -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> FOR -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_BRACE_OPEN -> FUNCTION -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_BRACE_OPEN.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> SEMICOLON -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.SEMICOLON.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> STATEMENT -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.STATEMENT.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> IF -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.IF.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> WHILE -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.WHILE.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> DO -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.DO.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> FOR -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.FOR.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> FUNCTION -> FUNC_STATEMENT
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.FUNCTION.ordinal(),
                FSMStates.FUNC_STATEMENT.ordinal());
        // FUNC_STATEMENT -> BRACE_CLOSE -> FUNC_END
        addStateRules(FSMStates.FUNC_STATEMENT.ordinal(), TokenStates.BRACE_CLOSE.ordinal(),
                FSMStates.FUNC_END.ordinal());
    }

    private String mapFSMStateToDecompState(int state) {
        if (state >= FSMStates.STATEMENT_START.ordinal()
                && state <= FSMStates.STATEMENT_END.ordinal())
            return DecompStates.P1.name();
        if (state >= FSMStates.IF_START.ordinal() && state <= FSMStates.IF_ELSE_END.ordinal()) {
            if (state == FSMStates.IF_START.ordinal())
                return DecompStates.D0.name();
            if (state > FSMStates.IF_START.ordinal() && state <= FSMStates.IF_THEN_END.ordinal())
                return DecompStates.D0_END.name();

            if (state == FSMStates.IF_ELSE.ordinal())
                return DecompStates.D1.name();
            else
                return DecompStates.D1_END.name();
        }
        if (state >= FSMStates.WHILE_START.ordinal() && state <= FSMStates.WHILE_END.ordinal()) {
            if (state == FSMStates.WHILE_START.ordinal())
                return DecompStates.D2.name();
            return DecompStates.D2_END.name();
        }
        if (state >= FSMStates.DO_WHILE_START.ordinal()
                && state <= FSMStates.DO_WHILE_END.ordinal()) {
            if (state == FSMStates.DO_WHILE_START.ordinal())
                return DecompStates.D3.name();
            return DecompStates.D3_END.name();
        }
        if (state >= FSMStates.FOR_START.ordinal() && state <= FSMStates.FOR_END.ordinal()) {
            if (state >= FSMStates.FOR_START.ordinal() && state <= FSMStates.FOR_INIT_END.ordinal())
                return DecompStates.P1.name();
            if (state >= FSMStates.FOR_COND.ordinal() && state <= FSMStates.FOR_COND_END.ordinal())
                return DecompStates.D0.name();
            return DecompStates.D0_END.name();
        }
        if (state >= FSMStates.FUNC_START.ordinal() && state <= FSMStates.FUNC_END.ordinal()) {
            if (state == FSMStates.FUNC_START.ordinal())
                return DecompStates.F1.name();
            return DecompStates.F1_END.name();
        }
        return "";
    }

    private boolean isFSMStartState(int fsmState) {
        return fsmState == FSMStates.STATEMENT_START.ordinal()
                || fsmState == FSMStates.IF_START.ordinal()
                || fsmState == FSMStates.WHILE_START.ordinal()
                || fsmState == FSMStates.DO_WHILE_START.ordinal()
                || fsmState == FSMStates.FOR_START.ordinal()
                || fsmState == FSMStates.FUNC_START.ordinal();
    }

    private void addStateRules(int startState, int input, int endState) {
        states[startState][input] = endState;
    }

    public void addString(String str) {
        strings.add(str);
    }

    public LinkedList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<Node<Integer>> getNodes() {
        return nodes;
    }

    public ArrayList<String> getStrings() {
        return strings;
    }

    public Node<Integer> parse() {
        try {
            // Tokenize all string inputs
            for (int i = 0; i < strings.size(); ++i) {
                tokenizer.tokenize(strings.get(i), i + 1);
                tokens.addAll(tokenizer.getTokens());
            }

            // Parse all tokens
            return parseTokens(getTokens());
        } catch (TokenizerException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private Node<Integer> parseTokens(LinkedList<Token> tokens) {
        Node<Integer> root = new Node<Integer>(-1);
        Node<Integer> walker = root;

        while (!tokens.isEmpty()) {
            Node<Integer> new_node = buildTRee(walker, tokens, null);
            if (new_node != null) {
                walker = new_node;
            } else {
                tokens.poll();
            }
        }

        if (root.children.size() > 0) {
            if (root.children.get(0).parents.get(0) == root)
                root.children.get(0).parents.remove(0);
            return root.children.get(0);
        } else
            return null;
    }

    private Node<Integer> buildTRee(Node<Integer> root, LinkedList<Token> tokens,
            Integer startState) {
        if (startState == null) {
            Token peekToken = tokens.peek();
            int peekInput = peekToken.type;
            int peekState = states[FSMStates.INIT_START.ordinal()][peekInput];
            startState = peekState;
        }

        boolean isStartState = isFSMStartState(startState);

        if (!isStartState)
            return null;

        if (startState == FSMStates.STATEMENT_START.ordinal())
            return buildStatementTree(root, tokens);
        if (startState == FSMStates.IF_START.ordinal())
            return buildIfTree(root, tokens);
        if (startState == FSMStates.WHILE_START.ordinal())
            return buildWhileTree(root, tokens);
        if (startState == FSMStates.DO_WHILE_START.ordinal())
            return buildDoWhileTree(root, tokens);
        if (startState == FSMStates.FOR_START.ordinal())
            return buildForLoopTree(root, tokens);
        if (startState == FSMStates.FUNC_START.ordinal())
            return buildFunctionTree(root, tokens);

        return root;
    }

    private Node<Integer> buildStatementTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.STATEMENT_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create walker and previous state
            Node<Integer> walker = root; // Walker will be used to link next nodes

            // Create first node if root node is not type statement
            if (root.type != DecompStates.P1.name()) {
                Node<Integer> start_node = new Node<Integer>(root.val + 1);
                start_node.tokens.add(token);
                start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
                start_node.parents.add(root);
                root.children.add(start_node);
                walker = start_node;

                nodes.add(start_node); // Store created nodes for final list
            } else {
                root.tokens.add(token);
            }

            // Map our node states so we can reconnect it later if needed
            Map<Integer, Node<Integer>> map = new HashMap<>();
            map.put(state, walker); // Map root to initial state

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                // No errors, just merge statement nodes
                if (peekState != FSMStates.ERROR.ordinal()) {
                    token = tokens.poll();
                    walker.tokens.add(token);

                    if (peekState == FSMStates.STATEMENT_END.ordinal())
                        return walker;

                    state = peekState;
                }
                // If error, exit;
                else {
                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }
            }
            return walker;
        }
        return root;
    }

    private Node<Integer> buildIfTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.IF_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create first node
            Node<Integer> start_node = new Node<Integer>(root.val + 1);
            start_node.tokens.add(token);
            start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
            start_node.parents.add(root);
            root.children.add(start_node);

            nodes.add(start_node); // Store created nodes for final list

            // Create walker and previous state
            Node<Integer> walker = start_node; // Walker will be used to link next nodes
            ArrayList<Node<Integer>> lastWalkers = new ArrayList<Node<Integer>>();
            Node<Integer> end_node = null;

            boolean isSuccess = false;

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                if (peekState != FSMStates.ERROR.ordinal()) {
                    if (peekState == FSMStates.IF_THEN_SINGLE_STATEMENT.ordinal()
                            || peekState == FSMStates.ELSE_IF_STATEMENT.ordinal()
                            || peekState == FSMStates.IF_ELSE_SINGLE_STATEMENT.ordinal()) {
                        walker = buildTRee(walker, tokens, null);

                        Node<Integer> lastWalker = new Node<Integer>(walker.val + 1);
                        lastWalker.tokens.add(walker.tokens.get(walker.tokens.size() - 1));
                        lastWalker.type = mapFSMStateToDecompState(peekState);
                        lastWalker.parents.add(walker);
                        walker.children.add(lastWalker);
                        lastWalkers.add(lastWalker);

                        nodes.add(lastWalker);
                        walker = lastWalker;

                        isSuccess = true;
                    } else if (peekState == FSMStates.IF_THEN_STATEMENT.ordinal()
                            || peekState == FSMStates.IF_ELSE_STATEMENT.ordinal()) {
                        walker = buildTRee(walker, tokens, null);
                    } else if (peekState == FSMStates.IF_ELSE.ordinal()) {
                        token = tokens.poll();

                        Node<Integer> new_node = new Node<Integer>(walker.val + 1);
                        new_node.tokens.add(token);
                        new_node.parents.add(start_node);
                        start_node.children.add(new_node);

                        nodes.add(new_node);
                        walker = new_node;

                        // In else branch, IF node is no longer D0 but D1
                        start_node.type = DecompStates.D1.name();
                    } else if (peekState == FSMStates.IF_THEN_END.ordinal()
                            || peekState == FSMStates.ELSE_IF_END.ordinal()
                            || peekState == FSMStates.IF_ELSE_END.ordinal()) {
                        // If peekState is END but state from pevious is BRACE_OPEN
                        // then there is empty body {}; create empty body node
                        if (state == FSMStates.IF_THEN_BRACE_OPEN.ordinal()
                                || state == FSMStates.IF_ELSE_BRACE_OPEN.ordinal()) {
                            Node<Integer> emptyNode = new Node<Integer>(walker.val + 1);
                            Token emptyToken =
                                    new Token(walker.tokens.get(walker.tokens.size() - 1).index,
                                            DecompStates.P1.ordinal(), "");
                            emptyNode.tokens.add(emptyToken);
                            emptyNode.type = DecompStates.P1.name();
                            emptyNode.parents.add(walker);
                            walker.children.add(emptyNode);
                            nodes.add(emptyNode);

                            walker = emptyNode;
                        }

                        token = tokens.poll();

                        Node<Integer> lastWalker = new Node<Integer>(walker.val + 1);
                        lastWalker.tokens.add(token);
                        lastWalker.type = mapFSMStateToDecompState(peekState);
                        lastWalker.parents.add(walker);
                        walker.children.add(lastWalker);
                        lastWalkers.add(lastWalker);

                        nodes.add(lastWalker);
                        walker = lastWalker;

                        isSuccess = true;

                        if (peekState == FSMStates.IF_ELSE_END.ordinal()) {
                            break;
                        }
                    } else {
                        token = tokens.poll();
                        walker.tokens.add(token);
                    }
                }
                // If error, exit;
                else {
                    if (isSuccess)
                        break;

                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }

                state = peekState;
            }

            // Finalize CFG structure

            // Create end_node
            end_node = new Node<Integer>(walker.val + 1);
            end_node.tokens.add(walker.tokens.get(walker.tokens.size() - 1));
            end_node.type = mapFSMStateToDecompState(state);
            nodes.add(end_node);

            // Connect to start walker if it does not have more than two children
            if (start_node.children.size() < 2) {
                end_node.parents.add(start_node);
                start_node.children.add(end_node);
            }

            // Connect previous last walkers
            for (Node<Integer> lastWalker : lastWalkers) {
                end_node.parents.add(lastWalker);
                lastWalker.children.add(end_node);
            }

            return end_node;
        }
        return root;
    }

    private Node<Integer> buildWhileTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.WHILE_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create first node
            Node<Integer> start_node = new Node<Integer>(root.val + 1);
            start_node.tokens.add(token);
            start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
            start_node.parents.add(root);
            root.children.add(start_node);

            nodes.add(start_node); // Store created nodes for final list

            // Create walker and previous state
            Node<Integer> walker = start_node; // Walker will be used to link next nodes
            Node<Integer> end_node = null;

            boolean isSuccess = false;

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                if (peekState != FSMStates.ERROR.ordinal()) {
                    if (peekState == FSMStates.WHILE_STATEMENT.ordinal()
                            || peekState == FSMStates.WHILE_SINGLE_STATEMENT.ordinal()) {
                        walker = buildTRee(walker, tokens, null);

                        if (peekState == FSMStates.WHILE_SINGLE_STATEMENT.ordinal()) {
                            isSuccess = true;
                            break;
                        }
                    } else if (peekState == FSMStates.WHILE_END.ordinal()) {
                        // If peekState is END but state from pevious is BRACE_OPEN
                        // then there is empty body {}; create empty body node
                        if (state == FSMStates.WHILE_BRACE_OPEN.ordinal()) {
                            Node<Integer> emptyNode = new Node<Integer>(walker.val + 1);
                            Token emptyToken =
                                    new Token(walker.tokens.get(walker.tokens.size() - 1).index,
                                            DecompStates.P1.ordinal(), "");
                            emptyNode.tokens.add(emptyToken);
                            emptyNode.type = DecompStates.P1.name();
                            emptyNode.parents.add(walker);
                            walker.children.add(emptyNode);
                            nodes.add(emptyNode);

                            walker = emptyNode;
                        }

                        token = tokens.poll();

                        end_node = new Node<Integer>(walker.val + 1);
                        end_node.tokens.add(token);
                        end_node.type = mapFSMStateToDecompState(peekState);
                        nodes.add(end_node);

                        isSuccess = true;
                        break;
                    } else {
                        token = tokens.poll();
                        walker.tokens.add(token);
                    }
                }
                // If error, exit;
                else {
                    if (isSuccess)
                        break;

                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }

                state = peekState;
            }

            // Finalize CFG structure

            // Link walker to start node
            walker.children.add(start_node);
            start_node.parents.add(walker);

            // Create end node if it does not exist using last token
            if (end_node == null) {
                end_node = new Node<Integer>(walker.val + 1);
                end_node.tokens.add(walker.tokens.get(walker.tokens.size() - 1));
                end_node.type = mapFSMStateToDecompState(state);
                nodes.add(end_node);
            }

            // Link start node to end node
            end_node.parents.add(start_node);
            start_node.children.add(end_node);

            return end_node;
        }
        return root;
    }

    private Node<Integer> buildDoWhileTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.DO_WHILE_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create first node
            Node<Integer> start_node = new Node<Integer>(root.val + 1);
            start_node.tokens.add(token);
            start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
            start_node.parents.add(root);
            root.children.add(start_node);

            nodes.add(start_node); // Store created nodes for final list

            // Create walker and previous state
            Node<Integer> walker = start_node; // Walker will be used to link next nodes
            Node<Integer> end_node = null;
            boolean isSuccess = false;

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                if (peekState != FSMStates.ERROR.ordinal()) {
                    if (peekState == FSMStates.DO_WHILE_STATEMENT.ordinal()) {
                        walker = buildTRee(walker, tokens, null);
                    } else if (peekState == FSMStates.DO_WHILE_END.ordinal()) {
                        token = tokens.poll();
                        walker.tokens.add(token);
                        end_node = walker;

                        isSuccess = true;
                        break;
                    } else if (peekState == FSMStates.DO_WHILE_BRACE_CLOSE.ordinal()
                            && state == FSMStates.DO_WHILE_BRACE_OPEN.ordinal()) {
                        token = tokens.poll();

                        // If peekState is END but state from pevious is BRACE_OPEN
                        // then there is empty body {}; create empty body node
                        Node<Integer> emptyNode = new Node<Integer>(walker.val + 1);
                        emptyNode.tokens.add(token);
                        emptyNode.type = DecompStates.P1.name();
                        emptyNode.parents.add(walker);
                        walker.children.add(emptyNode);
                        nodes.add(emptyNode);

                        walker = emptyNode;
                    } else if (peekState == FSMStates.DO_WHILE_KEYWORD.ordinal()) {
                        token = tokens.poll();

                        Node<Integer> new_node = new Node<Integer>(walker.val + 1);
                        new_node.tokens.add(token);
                        new_node.parents.add(walker);
                        walker.children.add(new_node);
                        walker = new_node;
                        nodes.add(new_node);
                    } else {
                        token = tokens.poll();
                        walker.tokens.add(token);
                    }
                }
                // If error, exit;
                else {
                    if (isSuccess)
                        break;

                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }
                state = peekState;
            }

            // Error, exit;
            if (end_node == null || end_node == start_node) {
                System.out.println("There was an error parsing the grammar for (" + walker.type
                        + ") token: " + walker.tokens.get(0));
                return walker;
            }

            // Finalize CFG structure

            // Link end node to start node
            end_node.type = mapFSMStateToDecompState(state);
            end_node.children.add(start_node);
            start_node.parents.add(end_node);

            return end_node;
        }
        return root;
    }


    private Node<Integer> buildForLoopTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.FOR_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create first node
            Node<Integer> start_node = new Node<Integer>(root.val + 1);
            start_node.tokens.add(token);
            start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
            start_node.parents.add(root);
            root.children.add(start_node);

            nodes.add(start_node); // Store created nodes for final list

            // Create walker and previous state
            Node<Integer> walker = start_node; // Walker will be used to link next nodes
            Node<Integer> forCond = null;
            Node<Integer> forBodyLastWalker = null;
            Node<Integer> forModify = null;
            Node<Integer> end_node = null;

            boolean isSuccess = false;

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                if (peekState != FSMStates.ERROR.ordinal()) {
                    if (peekState == FSMStates.FOR_COND.ordinal()
                            || peekState == FSMStates.FOR_COND_END.ordinal()) {
                        token = tokens.poll();

                        if (forCond == null) {
                            forCond = new Node<Integer>(walker.val + 1);
                            forCond.tokens.add(token);
                            forCond.type = mapFSMStateToDecompState(peekState);
                            forCond.parents.add(start_node);
                            start_node.children.add(forCond);

                            walker = forCond;
                            nodes.add(forCond);
                        } else {
                            walker.tokens.add(token);
                        }
                    } else if (peekState == FSMStates.FOR_MODIFY.ordinal()
                            || peekState == FSMStates.FOR_PAREN_CLOSE.ordinal()) {
                        token = tokens.poll();

                        if (forModify == null) {
                            forModify = new Node<Integer>(walker.val + 1);
                            forModify.tokens.add(token);

                            // Link forModify to forCond
                            forModify.children.add(forCond);
                            forCond.parents.add(forModify);

                            walker = forModify;
                            nodes.add(forModify);
                        } else {
                            walker.tokens.add(token);
                        }
                    } else if (peekState == FSMStates.FOR_STATEMENT.ordinal()
                            || peekState == FSMStates.FOR_SINGLE_STATEMENT.ordinal()) {
                        // Store old value
                        int oldForCondVal = forCond.val;

                        // Change to newest val so descendants are using newest node value index
                        forCond.val = walker.val;
                        walker = buildTRee(forCond, tokens, null); // Recurse using forCond

                        // Restore old value
                        forCond.val = oldForCondVal;

                        forBodyLastWalker = walker;

                        if (peekState == FSMStates.FOR_SINGLE_STATEMENT.ordinal()) {
                            isSuccess = true;
                            break;
                        }
                    } else if (peekState == FSMStates.FOR_END.ordinal()) {
                        // If peekState is END but state from pevious is BRACE_OPEN
                        // then there is empty body {}; create empty body node
                        if (state == FSMStates.FOR_BRACE_OPEN.ordinal()) {
                            Node<Integer> emptyNode = new Node<Integer>(walker.val + 1);
                            Token emptyToken =
                                    new Token(walker.tokens.get(walker.tokens.size() - 1).index,
                                            DecompStates.P1.ordinal(), "");
                            emptyNode.tokens.add(emptyToken);
                            emptyNode.type = DecompStates.P1.name();

                            emptyNode.parents.add(forCond);
                            forCond.children.add(emptyNode);
                            nodes.add(emptyNode);

                            forBodyLastWalker = emptyNode;
                            walker = emptyNode;
                        }

                        token = tokens.poll();
                        end_node = new Node<Integer>(walker.val + 1);
                        end_node.tokens.add(token);
                        end_node.type = mapFSMStateToDecompState(peekState);
                        nodes.add(end_node);

                        break;
                    } else {
                        token = tokens.poll();
                        walker.tokens.add(token);
                    }
                }
                // If error, exit;
                else {
                    if (isSuccess)
                        break;

                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }

                state = peekState;
            }

            // Finalize CFG structure

            // Create end_node if it doesn't exist
            if (end_node == null) {
                end_node = new Node<Integer>(walker.val + 1);
                end_node.tokens.add(walker.tokens.get(walker.tokens.size() - 1));
                end_node.type = mapFSMStateToDecompState(state);
                nodes.add(end_node);
            }

            // Link forCond to end_node
            forCond.children.add(end_node);
            end_node.parents.add(forCond);

            // If forBodyLastWalker exists, link forBodyLastWalker to forModify
            if (forBodyLastWalker != null) {
                forBodyLastWalker.children.add(forModify);
                forModify.parents.add(forBodyLastWalker);
            }
            // Else link forCond to forModify
            else {
                forCond.children.add(forModify);
                forModify.parents.add(forCond);
            }

            return end_node;
        }
        return root;
    }

    private Node<Integer> buildFunctionTree(Node<Integer> root, LinkedList<Token> tokens) {
        if (root != null) {
            Token token = tokens.poll();
            int input = token.type;
            int state = states[FSMStates.INIT_START.ordinal()][input];
            boolean isStartState = state == FSMStates.FUNC_START.ordinal();

            // Exit if state is invalid start state
            if (!isStartState)
                return root;

            // Create first node
            Node<Integer> start_node = new Node<Integer>(root.val + 1);
            start_node.tokens.add(token);
            start_node.type = mapFSMStateToDecompState(state); // Store DecompStates at root
            start_node.parents.add(root);
            root.children.add(start_node);

            nodes.add(start_node); // Store created nodes for final list

            // Create walker and previous state
            Node<Integer> walker = start_node; // Walker will be used to link next nodes
            Node<Integer> end_node = null;
            boolean isSuccess = false;

            while (!tokens.isEmpty()) {
                Token peekToken = tokens.peek();
                int peekInput = peekToken.type;
                int peekState = states[state][peekInput];

                if (peekState != FSMStates.ERROR.ordinal()) {
                    if (peekState == FSMStates.FUNC_STATEMENT.ordinal()) {
                        walker = buildTRee(walker, tokens, null);
                    } else if (peekState == FSMStates.FUNC_END.ordinal()) {
                        // If peekState is END but state from pevious is BRACE_OPEN
                        // then there is empty body {}; create empty body node
                        if (state == FSMStates.FUNC_BRACE_OPEN.ordinal()) {
                            Node<Integer> emptyNode = new Node<Integer>(walker.val + 1);
                            Token emptyToken =
                                    new Token(walker.tokens.get(walker.tokens.size() - 1).index,
                                            DecompStates.P1.ordinal(), "");
                            emptyNode.tokens.add(emptyToken);
                            emptyNode.type = DecompStates.P1.name();
                            emptyNode.parents.add(walker);
                            walker.children.add(emptyNode);
                            nodes.add(emptyNode);

                            walker = emptyNode;
                        }

                        token = tokens.poll();

                        // If function is a statement, ie ends in a SEMICOLOn like x = get();
                        // Then change type to STATEMENT
                        // And merge it with start_node
                        if (token.type == TokenStates.SEMICOLON.ordinal()) {
                            start_node.type = DecompStates.P1.name();
                            start_node.tokens.add(token);
                            end_node = start_node;
                        } else {
                            end_node = new Node<Integer>(walker.val + 1);
                            end_node.tokens.add(token);
                            end_node.type = mapFSMStateToDecompState(peekState);
                            end_node.parents.add(walker);
                            walker.children.add(end_node);
                            nodes.add(end_node);
                        }

                        isSuccess = true;
                        break;
                    } else {
                        token = tokens.poll();
                        walker.tokens.add(token);
                    }
                }
                // If error, exit;
                else {
                    if (isSuccess)
                        break;

                    System.out.println("There was an error parsing the grammar for (" + walker.type
                            + ") token: " + walker.tokens.get(0));
                    return walker;
                }
                state = peekState;
            }

            // Error, exit;
            if (end_node == null) {
                System.out.println("There was an error parsing the grammar for (" + walker.type
                        + ") token: " + walker.tokens.get(0));
                return walker;
            }

            return end_node;
        }
        return root;
    }

}
