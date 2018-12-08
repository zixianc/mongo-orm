package org.mongo.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chengshx
 * @date 2018/12/7
 */
@ConfigurationProperties(prefix = "org.mongo")
public class MongoProperties {

    private String uri;

    private String name;

    private String codecPackage;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodecPackage() {
        return codecPackage;
    }

    public void setCodecPackage(String codecPackage) {
        this.codecPackage = codecPackage;
    }
}
