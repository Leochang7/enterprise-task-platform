package cn.leo.taskplatform.exception;

public class PermissionException extends BizException {

    public PermissionException(String code, String message) {
        super(code, message);
    }
}
