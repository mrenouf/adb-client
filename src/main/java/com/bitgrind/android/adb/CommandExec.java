package com.bitgrind.android.adb;

import java.io.IOException;

public interface CommandExec {
    Result<String[]> execute(String[] args) throws IOException, InterruptedException;

    CommandExec DEFAULT = new DefaultCommandExec();

    static CommandExec getDefault() {
        return DEFAULT;
    }
}
