package com.dxy.library.cache.redis.exception;

/**
 * @author duanxinyuan
 * 2020/4/18 16:59
 */
public class UnsupportedCommandException extends RuntimeException {

    public UnsupportedCommandException() {
        super();
    }

    public UnsupportedCommandException(String message) {
        super("the command '" + message + "' is not supported");
    }

    public UnsupportedCommandException(String message, Throwable cause) {
        super("the command '" + message + "' is not supported", cause);
    }

    public UnsupportedCommandException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedCommandException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super("the command '" + message + "' is not supported", cause, enableSuppression, writableStackTrace);
    }

}
