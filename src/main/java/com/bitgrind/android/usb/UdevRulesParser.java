package com.bitgrind.android.usb;

import com.google.common.base.Splitter;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
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

    public static void parse(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must be non-null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            parse(reader);
        }
    }

    public static void parse(BufferedReader reader) throws IOException {
        String line;
        String preceedingComment = null;
        while ((line = reader.readLine()) != null) {
            for (int i = 0; i < line.length(); i++) {
                if (Character.isSpaceChar(line.charAt(i))) {
                    continue;
                }
                if (line.charAt(i) == '#') {
                    preceedingComment = line.substring(i+1);
                    break;
                }
                parseRule(preceedingComment, line.substring(i));
                break;
            }
        }
    }

    private static void parseRule(String preceedingComment, String substring) {
        for (String token : Splitter.on(',').split(substring)) {
            parseToken(token.trim());
        }
    }
    private static final Pattern UDEV_RULE =
            Pattern.compile("^([A-Z]+)(\\{[A-Za-z0-9_]+})?((?:!|\\+|=)?=)\"([^\"]+)\"$");

    private static void parseToken(String token) {
        Matcher matcher = UDEV_RULE.matcher(token);
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++)
            System.out.format("group[%d]: %s\n",i,matcher.group(i));
        }
        System.out.println();
    }

    interface Operator {
        void apply(String value);
    }

    abstract class Append implements Operator {}
    abstract class Assign implements Operator {}
    abstract class Compare implements Operator {}

    public static void main(String[] args) throws IOException {
        parse(UdevRulesParser.class.getResourceAsStream("udev_rules.txt"));
    }
}
