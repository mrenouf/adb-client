package com.bitgrind.android.usb;

import com.google.common.base.Splitter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by mrenouf on 3/14/17.
 */
public class UdevRulesParser {

    public static void parse(String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            parse(reader);
        }
    }

    static class RuleList {
        Map<String, Integer> labels;
        List<Rule> rules;

        RuleList() {
             labels = new HashMap<>();
             rules = new ArrayList<>();
        }

        void append(Rule rule) {
            rules.add(rule);
            for (Assignment a : rule.assigments) {
                if (a.key == Key.LABEL) {
                    labels.put(a.value, rules.size());
                }
            }
        }

        public void dump() {
            System.out.println("LABELS: " + labels);
            for (Rule rule : rules) {
                System.out.println("RULE: " + rule);
            }
        }
    }

    static class Rule {
        String comment;
        List<Matcher> matchers;
        List<Assignment> assigments;
        Rule next;

        Rule() {
            matchers = new ArrayList<>();
            assigments = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Rule{" +
                    "comment=" + comment +
                    ", matchers=" + matchers +
                    ", assigments=" + assigments +
                    '}';
        }
    }

    Map<String, Rule> labels = new HashMap<>();
    List<String> symlink = new ArrayList<>();
    String mode;
    String group;
    Map<String, String> attr = new HashMap<>();

    enum Type {
        MATCH,
        ASSIGNMENT
    }

    enum Key {
        ACTION,
        SUBSYSTEM,
        ATTR,
        ENV,
        GOTO,
        LABEL,
        SYMLINK,
        MODE,
        GROUP,
        TAG
    }

    static class Expression {
        Key key;
        String qualifer;
        Operator op;
        String value;

        Expression(String key, @Nullable String qualifier, Operator op,  String value) {
            this.key = Key.valueOf(key);
            this.qualifer = qualifier;
            this.op = op;
            this.value = value;
        }
    }

    static class Matcher extends Expression {
        Matcher(String key, @Nullable String qualifier, Operator op, String value) {
            super(key, qualifier, op, value);
        }

        @Override
        public String toString() {
            return "Matcher{key=" + key + ", qualifer=" + qualifer + ", op=" + op + ", value=" + value + "}";
        }
    }

    static class Assignment extends Expression {
        Assignment(String key, @Nullable String qualifier, Operator op, String value) {
            super(key, qualifier, op, value);
        }

        @Override
        public String toString() {
            return "Assignment{key=" + key + ", qualifer=" + qualifer + ", op=" + op + ", value=" + value + "}";
        }
    }

    enum Operator {
        APPEND("+=", Type.ASSIGNMENT),
        ASSIGN("=", Type.ASSIGNMENT),
        IS_EQUAL("==", Type.MATCH),
        NOT_EQUAL("!=", Type.MATCH);

        private static final Map<String, Operator> map = new HashMap<>();

        static {
            for (Operator op : Operator.values()) {
                map.put(op.value, op);
            }
        }

        private final String value;
        private final Type type;

        Operator(String value, Type type) {
            this.value = value;
            this.type = type;
        }

        public Type type() {
            return type;
        }

        public static Operator fromString(String value) {
            if (!map.containsKey(value)) {
                throw new IllegalArgumentException();
            }
            return map.get(value);
        }

    }


    public static void parse(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must be non-null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            parse(reader);
        }
    }

    public static void parse(BufferedReader reader) throws IOException {
        RuleList list = new RuleList();
        String line;
        String preceedingComment = null;
        while ((line = reader.readLine()) != null) {
            for (int i = 0; i < line.length(); i++) {
                if (Character.isSpaceChar(line.charAt(i))) {
                    continue;
                }
                if (line.charAt(i) == '#') {
                    preceedingComment = line.substring(i+1).trim();
                    break;
                }
                list.append(parseRule(preceedingComment, line.substring(i)));
                preceedingComment = null;
                break;
            }
        }
        list.dump();
    }

    private static Rule parseRule(String preceedingComment, String substring) {
        Rule rule = new Rule();
        rule.comment = preceedingComment;
        for (String token : Splitter.on(',').split(substring)) {
            parseToken(rule, token.trim());
        }
        return rule;
    }
    private static final Pattern UDEV_RULE =
            Pattern.compile("^([A-Z]+)(\\{[A-Za-z0-9_]+})?((?:!|\\+|=)?=)\"([^\"]+)\"$");

    private static void parseToken(Rule rule, String token) {
        java.util.regex.Matcher matcher = UDEV_RULE.matcher(token);
        if (matcher.matches()) {
            String key = matcher.group(1);
            String qual = matcher.group(2);
            Operator op = Operator.fromString(matcher.group(3));
            String value = matcher.group(4);
            switch (op.type()) {
                case MATCH:
                    rule.matchers.add(new Matcher(key, qual, op, value));
                    break;
                case ASSIGNMENT:
                    rule.assigments.add(new Assignment(key, qual, op, value));
                    break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        parse(UdevRulesParser.class.getResourceAsStream("udev_rules.txt"));
    }
}
