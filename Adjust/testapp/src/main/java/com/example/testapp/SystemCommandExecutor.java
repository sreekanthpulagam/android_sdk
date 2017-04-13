package com.example.testapp;

import android.os.SystemClock;

/**
 * Created by nonelse on 10.03.17.
 */

public class SystemCommandExecutor extends ICommandExecutor {
    @Override
    public void executeCommand(Command command) {
        super.executeCommand(command);

        switch (command.methodName) {
            case "exit": exit(); break;
        }
    }

    private void exit() {
        System.exit(0);
    }
}
