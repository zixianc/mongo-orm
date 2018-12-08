package org.mongo.spring.boot.autoconfigure;

import java.lang.annotation.*;

/**
 * @author chengshx
 * @date 2018/12/8
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableMongoCodec {
}
