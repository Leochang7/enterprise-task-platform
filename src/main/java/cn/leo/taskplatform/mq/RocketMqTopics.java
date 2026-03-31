package cn.leo.taskplatform.mq;

public final class RocketMqTopics {

    public static final String TASK_DISPATCH_NORMAL = "task_dispatch_normal";
    public static final String TASK_DISPATCH_HIGH = "task_dispatch_high";
    public static final String TASK_RETRY = "task_retry";
    public static final String TASK_NOTIFY = "task_notify";
    public static final String TASK_DEAD_LETTER = "task_dead_letter";

    private RocketMqTopics() {
    }
}
