package top.newleaf.mongo.scanner;

/**
 * @author chengshx
 */
public interface IScanAction {

    /**
     * 扫到class的具体操作
     * @param className 类名称
     */
    void doAction(String className);
}
