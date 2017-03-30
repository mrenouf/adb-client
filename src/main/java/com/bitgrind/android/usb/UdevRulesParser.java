package com.bitgrind.android.usb;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class UdevRulesParser {

    static class State {
        Map<String, String> env = new HashMap<>();
        Map<String, String> attr = new HashMap<>();
        List<String> symlink = new ArrayList<>();
        List<String> tag = new ArrayList<>();
        Map<Key, String> keyValues = new HashMap<>();
        int gotoRule = -1;
        List<String> group = new ArrayList<>();

        State() {
            keyValues.put(Key.SUBSYSTEM, "usb");
            keyValues.put(Key.ACTION, "add");
        }

        void setAttr(String name, String value) {
            attr.put(name, value);
        }

        boolean evaluateMatcher(Matcher matcher) {
            switch (matcher.key) {
                case SUBSYSTEM:
                    return matcher.op.eval(Objects.toString(keyValues.get(Key.SUBSYSTEM), ""), matcher.value);
                case ATTR:
                    return matcher.op.eval(Objects.toString(attr.get(matcher.qualifier), ""), matcher.value);
                case ACTION:
                    return matcher.op.eval(Objects.toString(keyValues.get(Key.ACTION), ""), matcher.value);
                case ENV:
                    return matcher.op.eval(Objects.toString(env.get(matcher.qualifier), ""), matcher.value);
            }
            return false;
        }

        void evaluateAssignment(RuleList list, Assignment assignment) {
            switch (assignment.key) {
                case SYMLINK:
                    if (assignment.op == Operator.ASSIGN) {
                        symlink = Lists.newArrayList(assignment.value);
                    } else {
                        symlink.add(assignment.value);
                    }
                    break;
                case ENV:
                    if (assignment.op == Operator.ASSIGN) {
                        env.put(assignment.qualifier, assignment.value);
                    } else {
                        throw new IllegalStateException("Can't append to ENV values");
                    }
                    break;
                case GOTO:
                    if (assignment.op == Operator.ASSIGN) {
                        gotoRule = list.labels.get(assignment.value);
                    } else {
                        throw new IllegalStateException("Can't append to GOTO");
                    }
                case GROUP:
                    switch (assignment.op) {
                        case ASSIGN:
                            group = Lists.newArrayList(assignment.value);
                            break;
                        case APPEND:
                            group.add(assignment.value);
                            break;
                        default:
                            throw new IllegalStateException("Bad operator for assignment: "
                                + assignment.op);
                    }
                case MODE:
                    // TODO
                    break;
                case LABEL:
                    // Nothing to do
                    break;
                case TAG:
                    switch (assignment.op) {
                        case ASSIGN:
                            tag = Lists.newArrayList(assignment.value);
                            break;
                        case APPEND:
                            tag.add(assignment.value);
                            break;
                        default:
                            throw new IllegalStateException("Bad operator for assignment: "
                                + assignment.op);
                    }
            }
        }
    }

    private static void evaluate(State state, RuleList rules) {
        next_rule:
        for (int i = 0; i < rules.rules.size(); i++) {
            Rule rule = rules.rules.get(i);
            for (Matcher m : rule.matchers) {
                if (!state.evaluateMatcher(m)) {
                    continue next_rule;
                }
            }
            for (Assignment a : rule.assigments) {
                state.evaluateAssignment(rules, a);
            }
            if (state.gotoRule >= 0) {
                i = (state.gotoRule - 1);
                state.gotoRule = -1;

            }
        }
    }

    static class RuleList {
        Map<String, Integer> labels = new HashMap<>();
        List<Rule> rules = new ArrayList<>();

        RuleList() {
             labels = new HashMap<>();
        }

        void append(Rule rule) {
            rules.add(rule);
            for (Assignment a : rule.assigments) {
                if (a.key == Key.LABEL) {
                    labels.put(a.value, rules.size());
                }
            }
        }
    }

    static class Rule {
        String comment;
        List<Matcher> matchers;
        List<Assignment> assigments;

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
        String qualifier;
        Operator op;
        String value;

        Expression(String key, @Nullable String qualifier, Operator op,  String value) {
            this.key = Key.valueOf(key);
            this.qualifier = qualifier;
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
            return "Matcher{key=" + key + ", qualifier=" + qualifier + ", op=" + op + ", value=" + value + "}";
        }
    }

    static class Assignment extends Expression {
        Assignment(String key, @Nullable String qualifier, Operator op, String value) {
            super(key, qualifier, op, value);
        }

        @Override
        public String toString() {
            return "Assignment{key=" + key + ", qualifier=" + qualifier + ", op=" + op + ", value=" + value + "}";
        }
    }

    enum Operator {
        APPEND("+=", Type.ASSIGNMENT),
        ASSIGN("=", Type.ASSIGNMENT),
        IS_EQUAL("==", Type.MATCH) {
            boolean eval(String a, String b) {
                return a.equals(b);
            }
        },
        NOT_EQUAL("!=", Type.MATCH) {
            boolean eval(String a, String b) {
                return !a.equals(b);
            }
        };

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

        boolean eval(String a, String b) {
            return false;
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


    private static RuleList parse(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must be non-null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            return parse(reader);
        }
    }

    private static RuleList parse(BufferedReader reader) throws IOException {
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
        //list.dump();
        return list;
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
            Pattern.compile("^([A-Z]+)(?:\\{([A-Za-z0-9_]+)})?((?:!|\\+|=)?=)\"([^\"]+)\"$");

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

        //state.setAttr("idVendor", "0502");
        //state.setAttr("idProduct", "3604");
//        state.setAttr("idVendor", "18d1");
//        state.setAttr("idProduct", "4ee1");


        RuleList list = parse(UdevRulesParser.class.getResourceAsStream("udev_rules.txt"));

        List<UsbIdsLoader.UsbId> androidDevices = new ArrayList<>();
        List<UsbIdsLoader.UsbId> androidAdbDevices = new ArrayList<>();
        List<UsbIdsLoader.UsbId> androidFastbootDevices = new ArrayList<>();

        for (UsbIdsLoader.UsbId id : UsbIdsLoader.usbIds()) {
            State state = new State();
            state.setAttr("idProduct",  String.format("%04x", id.productId));
            state.setAttr("idVendor",  String.format("%04x", id.vendorId));
            evaluate(state, list);
            if (state.symlink.contains("android")) {
                androidDevices.add(id);
            }
            if (state.symlink.contains("android_fastboot")) {
                androidFastbootDevices.add(id);
            }
            if (state.symlink.contains("android_adb")) {
                androidAdbDevices.add(id);
            }
        }

        System.out.println("android\n    " + Joiner.on("\n    ").join(androidDevices));
        System.out.println();
        System.out.println("adb    \n    " + Joiner.on("\n    ").join(androidAdbDevices));
        System.out.println();
        System.out.println("fastboot\n    " + Joiner.on("\n    ").join(androidFastbootDevices));
        System.out.println();
    }
}
