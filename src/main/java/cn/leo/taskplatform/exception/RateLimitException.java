package cn.leo.taskplatform.exception;

public class RateLimitException extends BizException {

    public RateLimitException(String message) {
        super("TASK_4290", message);
    }
}
