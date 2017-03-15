package com.bitgrind.android.adb;

import java.io.File;
import java.io.IOException;
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
    private final CommandExec commandExec;

    private Path adbPath;

    public AdbStarter() {
        this(CommandExec.DEFAULT, null);
    }

    public AdbStarter(String adbPath) {
        this(CommandExec.DEFAULT, adbPath);
    }

    public AdbStarter(CommandExec commandExec, String adbPath) {
        this.adbPath = Paths.get(adbPath);
        this.commandExec = commandExec;
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.startsWith("Windows");
    }

    private static Optional<Path> findExecutableInPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return Optional.empty();
        }
        return PATTERN_PATHSEP.splitAsStream(path)
                .map(Paths::get)
                .filter(pathElement -> Files.isExecutable(pathElement.resolve(name)))
                .findFirst()
                .map(pathElement -> pathElement.resolve(name));
    }

    public Result<AdbVersion> getAdbVersion(Path adbPath) {
        try {
            Result<String[]> result = commandExec.execute(new String[]{adbPath.toString(), "version"});
            if (result.ok()) {
                String[] output = result.get();
                if (output.length > 0) {
                    Matcher matcher = VERSION.matcher(output[0]);
                    if (matcher.find()) {
                        int maj = Integer.parseInt(matcher.group(1));
                        int min = Integer.parseInt(matcher.group(2));
                        int pt = Integer.parseInt(matcher.group(3));
                        return Result.ofValue(new AdbVersion(maj, min, pt));
                    }
                }
            }
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        } catch (InterruptedException e) {
            return Result.error(ErrorCode.INTERRUPTED, e);
        }
        return Result.error(ErrorCode.PARSE_ERROR);
    }

    public Result<AdbVersion> startAdb(AdbVersion minVersion) {
        if (adbPath == null) {
            Optional<Path> adbExec = findExecutableInPath(ADB_EXECUTABLE);
            if (!adbExec.isPresent()) {
                return Result.error(ErrorCode.ADB_MISSING_FROM_PATH);
            }
            adbPath = adbExec.get();
        }
        Result<AdbVersion> versionResult = getAdbVersion(adbPath);
        if (versionResult.ok()) {
            AdbVersion version = versionResult.get();
            if (version.lessThan(minVersion)) {
                return Result.error(ErrorCode.ADB_VERSION_MISMATCH, new Exception(
                        String.format("%s is version %s. %s or newer required", adbPath, version, minVersion)));
            }
            try {
                System.out.format("Starting adb (%s)\n", adbPath);
                Result<String[]> result = commandExec.execute(new String[]{adbPath.toString(), "start-server"});
                if (!result.ok()) {
                    return result.asError();
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
