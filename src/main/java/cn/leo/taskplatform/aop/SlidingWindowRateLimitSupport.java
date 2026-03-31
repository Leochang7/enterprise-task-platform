package cn.leo.taskplatform.aop;

import cn.leo.taskplatform.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimitSupport {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> slidingWindowScript;

    public void check(String key, int windowSeconds, int maxRequests) {
        long now = Instant.now().toEpochMilli();
        Long allowed = stringRedisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }
    }
}
