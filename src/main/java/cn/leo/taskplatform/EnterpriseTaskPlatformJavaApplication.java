package cn.leo.taskplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("cn.leo.taskplatform.mapper")
@ConfigurationPropertiesScan
public class EnterpriseTaskPlatformJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseTaskPlatformJavaApplication.class, args);
    }

}
