package com.jobgpt.common;

public class MissingEnvironmentVariableException extends IllegalStateException {

    public MissingEnvironmentVariableException(String variableName, String usage) {
        super("Missing required environment variable: " + variableName + ". " + usage);
    }
}
