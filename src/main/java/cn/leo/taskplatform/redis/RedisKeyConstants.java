package cn.leo.taskplatform.redis;

public final class RedisKeyConstants {

    public static final String AUTH_TOKEN = "auth:token:";
    public static final String AUTH_PERMISSION = "auth:perm:";
    public static final String TASK_STATUS = "task:status:";
    public static final String TASK_DETAIL = "task:detail:";
    public static final String TASK_IDEMPOTENT = "task:idempotent:";
    public static final String RATE_LIMIT = "rate_limit:";

    private RedisKeyConstants() {
    }
}
