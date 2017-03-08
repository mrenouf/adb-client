package com.bitgrind.android.adb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AdbStarter {

    private static final Pattern VERSION =
            Pattern.compile("version ([0-9]+)\\.([0-9]+)\\.([0-9]+)");

    private static Result<AdbVersion> getAdbVersion(Path adbPath) {
        try {
            Process exec = Runtime.getRuntime().exec(new String[]{adbPath.toString(), "version"});
            exec.waitFor();
            String versionString =
                    new BufferedReader(new InputStreamReader(exec.getInputStream(), StandardCharsets.UTF_8), 80)
                            .readLine();

            Matcher matcher = VERSION.matcher(versionString);
            if (matcher.find()) {
                int maj = Integer.parseInt(matcher.group(1));
                int min = Integer.parseInt(matcher.group(2));
                int pt = Integer.parseInt(matcher.group(3));
                return Result.ofValue(new AdbVersion(maj, min, pt));
            } else {
                return Result.error(ErrorCode.PARSE_ERROR);
            }
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        } catch (InterruptedException e) {
            return Result.error(ErrorCode.INTERRUPTED, e);
        }
    }

    // First try to start adb from the path if it exists
    public static boolean startAdb(Path adbPath) {
        boolean started = false;
        try {
            System.err.println(getAdbVersion(adbPath.resolve("adb")));
            if (Runtime.getRuntime().exec(new String[]{adbPath.resolve("adb").toString(), "start-server"}).waitFor() == 0) {
                started = true;
            }
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        return started;
    }

    public static void main(String[] args) {
        new AdbStarter().startAdb();
    }

    private void startAdb() {
        Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                .map(Paths::get)
                .filter(path -> Files.isExecutable(path.resolve("adb")))
                .findFirst()
                .ifPresent(AdbStarter::startAdb);
    }

    private static class AdbVersion {
        final int major;
        final int minor;
        final int point;

        public AdbVersion(int major, int minor, int point) {
            this.major = major;
            this.minor = minor;
            this.point = point;
        }

        @Override
        public String toString() {
            return String.format("AdbVersion{%d.%d.%d}", major, minor, point);
        }
    }
}
