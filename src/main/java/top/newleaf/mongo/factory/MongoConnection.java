package top.newleaf.mongo.factory;

/**
 * @author chengshx
 */
public class MongoConnection {

    private String uri;

    private String db;

    private String name;

    private boolean isDefault = false;

    public MongoConnection() {
    }

    public MongoConnection(String uri, String db, String name, boolean isDefault) {
        this.uri = uri;
        this.db = db;
        this.name = name;
        this.isDefault = isDefault;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("{");
        sb.append("\"uri\":\"").append(uri).append('\"');
        sb.append(", \"db\":\"").append(db).append('\"');
        sb.append(", \"name\":\"").append(name).append('\"');
        sb.append(", \"isDefault\":").append(isDefault);
        sb.append('}');
        return sb.toString();
    }
}
