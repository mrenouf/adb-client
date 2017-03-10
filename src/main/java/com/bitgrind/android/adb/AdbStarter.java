package com.bitgrind.android.adb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdbStarter {
    private static final String ADB_EXECUTABLE = isWindows() ? "adb.exe" : "adb";
    private static final Pattern PATTERN_PATHSEP = Pattern.compile(Pattern.quote(File.pathSeparator));

    private static final Pattern VERSION =
            Pattern.compile("version ([0-9]+)\\.([0-9]+)\\.([0-9]+)");

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.startsWith("Windows");
    }

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

    private static Optional<Path> findExecutableInPath(String name) {
        //String path = System.getenv("PATH");
        String path = "/usr/local/bin:/usr/bin:/bin:/usr/local/games:/usr/games:/usr/lib/jvm/java-8-oracle/bin:/usr/lib/jvm/java-8-oracle/db/bin:/usr/lib/jvm/java-8-oracle/jre/bin:/home/mrenouf/Android/Sdk/tools:/home/mrenouf/Android/Sdk/platform-tools";
        if (path == null) {
            return Optional.empty();
        }
        return PATTERN_PATHSEP.splitAsStream(path)
                .map(Paths::get)
                .filter(pathElement -> Files.isExecutable(pathElement.resolve(name)))
                .findFirst()
                .map(pathElement -> pathElement.resolve(name));
    }

    public static void main(String[] args) throws Exception {
        //System.out.println(new AdbStarter().startAdbFromPath(AdbVersion.ANY).checkedGet());

        final Path ADB_PATH = Paths.get("/home/mrenouf/Android/Sdk/platform-tools/adb");
        System.out.println(new AdbStarter().startAdb(ADB_PATH, AdbVersion.ANY));

    }

    public Result<AdbVersion> startAdbFromPath(AdbVersion minVersion) {
        Optional<Path> adbExec = findExecutableInPath(ADB_EXECUTABLE);
        if (!adbExec.isPresent()) {
            return Result.error(ErrorCode.ADB_MISSING_FROM_PATH);
        }
        return startAdb(adbExec.get(), minVersion);
    }

    public Result<AdbVersion> startAdb(Path path, AdbVersion minVersion) {
        Result<AdbVersion> versionResult = getAdbVersion(path);
        if (!Files.isExecutable(path)) {
            return Result.error(ErrorCode.NOT_FOUND);
        }
        boolean started = false;
        if (versionResult.ok()) {
            AdbVersion version = versionResult.get();
            if (version.lessThan(minVersion)) {
                return Result.error(ErrorCode.ADB_VERSION_MISMATCH, new Exception(
                        String.format("%s is version %s. %s or newer required", path, version, minVersion)));
            }
            try {
                System.out.format("Starting adb (%s)\n", path);
                if (Runtime.getRuntime().exec(new String[]{path.toString(), "start-server"}).waitFor() == 0) {
                    started = true;
                }
                if (!started) {
                    return Result.error(ErrorCode.ADB_FAILED_TO_START);
                }
                return Result.ofValue(versionResult.get());
            } catch (InterruptedException e) {
                return Result.exception(e);
            } catch (IOException e) {
                return Result.exception(e);
            }
        }
        return versionResult.asError();
    }
}
