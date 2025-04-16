package org.carpetorgaddition.exception;

import org.carpetorgaddition.util.IOUtils;

import java.io.IOException;

public class CommandExecuteIOException extends RuntimeException {
    private CommandExecuteIOException(IOException throwable) {
        super(throwable);
    }

    // 创建异常，并将异常信息写入日志
    public static CommandExecuteIOException of(IOException e) {
        CommandExecuteIOException exception = new CommandExecuteIOException(e);
        IOUtils.loggerError(e);
        return exception;
    }
}
