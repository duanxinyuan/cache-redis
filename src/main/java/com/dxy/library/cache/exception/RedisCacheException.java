package com.dxy.library.cache.exception;

/**
 * @author duanxinyuan
 * 2019/2/26 23:45
 */
public class RedisCacheException extends RuntimeException {

    public RedisCacheException() {
        super();
    }

    public RedisCacheException(String message) {
        super(message);
    }

    public RedisCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisCacheException(Throwable cause) {
        super(cause);
    }

    protected RedisCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
