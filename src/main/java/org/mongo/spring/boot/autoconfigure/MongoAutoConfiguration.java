package org.mongo.spring.boot.autoconfigure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chengshx
 * @date 2018/12/8
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongoAutoConfiguration implements InitializingBean {

    private MongoProperties mongoProperties;

    public MongoAutoConfiguration(MongoProperties mongoProperties) {
        this.mongoProperties = mongoProperties;
    }

    public void afterPropertiesSet() throws Exception {

    }

    @Bean
    public MongoFactory mongoFactory() {
        MongoFactory mongoFactory = new MongoFactory();
        mongoFactory.init(mongoProperties.getUri(), mongoProperties.getName(), mongoProperties.getCodecPackage());
        return mongoFactory;
    }
}
