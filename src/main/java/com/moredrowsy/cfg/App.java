package com.moredrowsy.cfg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Control Flow Graph (CFG) Program
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        System.out.println("Control Flow Graph Program");

        Parser parser = new Parser();

        String filename = "input.txt";
        InputStream in = App.class.getClassLoader().getResourceAsStream(filename);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in));) {
            String line;

            // Parse each line
            while ((line = br.readLine()) != null) {
                parser.addString(line);
            }
        }

        // Print code text
        System.out.println("\n\nCode text:");
        for (String str : parser.getStrings()) {
            System.out.println(str);
        }

        // Parse all input strings
        Node<Integer> root = parser.parse();
        ArrayList<Node<Integer>> nodes = parser.getNodes();

        System.out.println("\n\nVertices:");
        for (Node<Integer> node : nodes) {
            System.out.println(node);

            System.out.println("\tchildren:");
            for (Node<Integer> child : node.children) {
                System.out.println("\t\t" + child);
            }
            if (node.children.size() == 0)
                System.out.println("\t\tNone");

            System.out.println("\tparents:");
            for (Node<Integer> parent : node.parents) {
                System.out.println("\t\t" + parent);
            }
            if (node.parents.size() == 0)
                System.out.println("\t\tNone");
        }
    }
}
