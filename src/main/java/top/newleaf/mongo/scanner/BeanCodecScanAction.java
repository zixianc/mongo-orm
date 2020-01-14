package top.newleaf.mongo.scanner;

import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.newleaf.mongo.codec.BeanCodec;

import java.util.HashSet;
import java.util.Set;

/**
 * @author chengshx
 * @date 2020/1/9
 */
public class BeanCodecScanAction implements IScanAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(BeanCodecScanAction.class);

    private Set<Codec> codecSet = new HashSet<>();

    @Override
    public void doAction(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (CollectibleCodec.class.isAssignableFrom(clazz) && BeanCodec.class != clazz) {
                codecSet.add((Codec) clazz.newInstance());
                LOGGER.info("add codec : {}", className);
            }
        } catch (Throwable e) {
            LOGGER.error("load class faild : {}", className, e);
        }
    }

    public Set<Codec> getCodecSet() {
        return codecSet;
    }
}
