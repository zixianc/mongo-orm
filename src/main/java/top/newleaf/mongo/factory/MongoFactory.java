package top.newleaf.mongo.factory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.newleaf.mongo.scanner.BeanCodecScanAction;
import top.newleaf.mongo.scanner.ClassScanner;

import javax.persistence.Table;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chengshx
 */
public class MongoFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoFactory.class);
    private final static String CONF_NAME = "mongo.xml";
    private static Map<String, MongoDB> dbs = new HashMap<>();
    private static MongoFactory instance = new MongoFactory();
    private ConcurrentHashMap<Class<?>, String> tables = new ConcurrentHashMap<Class<?>, String>();
    private MongoDB defaultDb;
    private boolean hasInit = false;

    private MongoFactory() {
    }

    public static MongoFactory getInstance() {
        return instance;
    }

    public static MongoDB getDb() {
        return instance.defaultDb;
    }

    public static MongoDB getDb(String name) {
        return dbs.get(name);
    }

    public static Map<String, MongoDB> getDbs() {
        return dbs;
    }

    public boolean getHasInit() {
        return hasInit;
    }

    public String getCollectionName(Class<?> clazz) {
        String collName;
        collName = tables.get(clazz);
        if (collName == null) {
            Table table = clazz.getAnnotation(Table.class);
            if (table != null && !"".equals(table.name())) {
                collName = table.name();
                tables.put(clazz, collName);
            }
            if (collName == null) {
                throw new IllegalArgumentException(clazz.getName() + "未指定表名");
            }
        }
        return collName;
    }

    /**
     * mongodb连接初始化
     * @param confPath
     */
    public void init(String confPath) {
        try {
            Document document = loadXml(confPath);
            // 解析codec扫描位置
            List<Node> packageNodes = document.selectNodes("mongo/packages/package");
            Set<String> packages = new HashSet<>();
            if (packages != null) {
                for (Node packageNode : packageNodes) {
                    packages.add(packageNode.getText());
                }
            }
            BeanCodecScanAction scanAction = new BeanCodecScanAction();
            ClassScanner.scan(packages, true, scanAction);
            // 解析mongo数据源配置
            List<MongoConnection> connections = parseMongoConnection(document);
            // 创建mongo连接
            if (!connections.isEmpty()) {
                createConnections(connections, scanAction.getCodecSet());
            }
        } catch (Exception e) {
            LOGGER.error("初始化mongo连接失败", e);
        }
    }

    /**
     * 创建mongo连接
     * @param connections 连接信息
     * @param codecSet codec集合
     */
    public void createConnections(List<MongoConnection> connections, Set<? extends Codec> codecSet) {
        if(!hasInit && !connections.isEmpty()) {
            synchronized (instance) {
                if (!hasInit) {
                    hasInit = true;
                    for(MongoConnection connection : connections) {
                        MongoClientOptions.Builder build = new MongoClientOptions.Builder();
                        if (codecSet.size() > 0) {
                            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(codecSet.toArray(new Codec[codecSet.size()])), MongoClient.getDefaultCodecRegistry());
                            build.codecRegistry(codecRegistry);
                        }
                        MongoClientURI mongoURI = new MongoClientURI(connection.getUri(), build);
                        MongoClient client = new MongoClient(mongoURI);
                        MongoDatabase db = client.getDatabase(connection.getDb());
                        MongoDB mongoDB = new MongoDB(connection.getName(), db);
                        dbs.put(connection.getName(), mongoDB);
                        if(connection.getIsDefault()) {
                            defaultDb = mongoDB;
                        }
                    }
                }
            }
        }
    }

    private Document loadXml(String confPath) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            if (confPath == null || confPath.isEmpty()) {
                // 未指定mongo.xml配置，尝试在类路径加载
                try (InputStream inputStream = MongoFactory.class.getClassLoader().getResourceAsStream(CONF_NAME)) {
                    if (inputStream != null) {
                        document = reader.read(inputStream);
                        LOGGER.warn("未指定具体mongo.xml配置，在类路径加载");
                    }
                }
            } else {
                if(!confPath.endsWith(CONF_NAME)) {
                    confPath = confPath.endsWith("/") ? confPath + CONF_NAME : confPath + "/" + CONF_NAME;
                }
                document = reader.read(confPath);
            }
        } catch (Exception e) {
            LOGGER.error("读取mongo.xml错误", e);
        }
        if (document == null) {
            throw new RuntimeException("未检测到mongo.xml配置，请检查配置路径是否正确");
        }
        return document;
    }

    private List<MongoConnection> parseMongoConnection(Document document) {
        List<MongoConnection> connections = new ArrayList<>();
        List<Node> connectionNodes = document.selectNodes("mongo/connections/connection");
        if (connectionNodes != null) {
            for (Node connectionNode : connectionNodes) {
                String uri = connectionNode.selectSingleNode("uri").getText();
                String dbName = connectionNode.selectSingleNode("db").getText();
                String name = connectionNode.selectSingleNode("name").getText();
                Node isDefaultNode = connectionNode.selectSingleNode("isDefault");
                boolean isDefault = false;
                if (isDefaultNode != null) {
                    isDefault = Boolean.valueOf(isDefaultNode.getText());
                }
                connections.add(new MongoConnection(uri, dbName, name, isDefault));
            }
        }
        return connections;
    }

}
