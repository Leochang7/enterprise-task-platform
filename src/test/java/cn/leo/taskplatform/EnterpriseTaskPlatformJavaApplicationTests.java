package cn.leo.taskplatform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"
})
class EnterpriseTaskPlatformJavaApplicationTests {

    @Test
    void contextLoads() {
    }

}
