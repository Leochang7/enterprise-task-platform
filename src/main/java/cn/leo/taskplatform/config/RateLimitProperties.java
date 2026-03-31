package cn.leo.taskplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "task.rate-limit")
public class RateLimitProperties {

    private Rule submit = new Rule();
    private Rule login = new Rule();

    @Data
    public static class Rule {
        private int windowSeconds = 60;
        private int maxRequests = 10;
    }
}
