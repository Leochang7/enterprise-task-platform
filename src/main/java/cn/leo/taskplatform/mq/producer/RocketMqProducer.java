package cn.leo.taskplatform.mq.producer;

import cn.leo.taskplatform.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RocketMqProducer {

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    public void sendTaskDispatchMessage(String topic, Object message) {
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            throw new BizException("TASK_5001", "RocketMQTemplate 未配置");
        }
        rocketMQTemplate.convertAndSend(topic, message);
    }
}
