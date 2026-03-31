package cn.leo.taskplatform.exception;

public class AuthException extends BizException {

    public AuthException(String code, String message) {
        super(code, message);
    }
}
