package top.newleaf.mongo.factory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * @author chengshx
 * @date 2019/7/15
 */
public class MongoDB {

    private String name;

    private MongoDatabase mongoDatabase;

    public MongoDB() {
    }

    public MongoDB(String name, MongoDatabase mongoDatabase) {
        this.name = name;
        this.mongoDatabase = mongoDatabase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public void setMongoDatabase(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    /**
     * 获取自定义解析器集合
     *
     * @param clazz
     * @return
     */
    public <T> MongoCollection<T> getCollection(Class<T> clazz) {
        String collName = MongoFactory.getInstance().getCollectionName(clazz);
        if (collName != null) {
            return getCollection(collName, clazz);
        }
        return null;
    }

    /**
     * 获取默认集合
     *
     * @param collName
     * @return
     */
    public MongoCollection<Document> getCollection(String collName) {
        return mongoDatabase.getCollection(collName);
    }

    /**
     * 获取自定义解析器集合
     *
     * @param clazz
     * @return
     */
    public <T> MongoCollection<T> getCollection(String collName, Class<T> clazz) {
        if (collName != null) {
            return mongoDatabase.getCollection(collName, clazz);
        }
        return null;
    }
}
