package com.bitgrind.android.adb;

import com.bitgrind.android.adb.CommandExec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class DefaultCommandExec implements CommandExec {

    public Result<String[]> execute(String... args) throws IOException, InterruptedException {
        Process exec = Runtime.getRuntime().exec(args);
        int resultCode = exec.waitFor();
        BufferedReader br =
                new BufferedReader(new InputStreamReader(exec.getInputStream(), StandardCharsets.UTF_8), 80);
        if (resultCode != 0) {
            return Result.error(ErrorCode.COMMAND_FAILED, new CommandException(br.readLine()));
        }
        ArrayList<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        return Result.ofValue(lines.toArray(new String[lines.size()]));
    }


}
