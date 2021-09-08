package com.moredrowsy.cfg;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private class TokenInfo {
        public final Pattern regex;
        public final int type;

        public TokenInfo(Pattern regex, int type) {
            super();
            this.regex = regex;
            this.type = type;
        }
    }

    private LinkedList<TokenInfo> tokenInfos;
    private LinkedList<Token> tokens;

    public Tokenizer() {
        tokenInfos = new LinkedList<TokenInfo>();
        tokens = new LinkedList<Token>();
    }

    public void add(String regex, int type) {
        tokenInfos.add(
                new TokenInfo(Pattern.compile("^(" + regex + ")", Pattern.CASE_INSENSITIVE), type));
    }

    public void tokenize(String str, int index) {
        String s = str.trim();
        tokens.clear();

        while (!s.equals("")) {
            boolean match = false;

            for (TokenInfo info : tokenInfos) {
                Matcher m = info.regex.matcher(s);

                if (m.find()) {
                    match = true;
                    String tok = m.group().trim();
                    s = m.replaceFirst("").trim();
                    tokens.add(new Token(index, info.type, tok));
                    break;
                }
            }
            if (!match)
                throw new TokenizerException("Unexpected character in input: " + s);
        }
    }

    public LinkedList<Token> getTokens() {
        return tokens;
    }

}
