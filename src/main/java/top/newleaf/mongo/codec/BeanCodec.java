package top.newleaf.mongo.codec;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chengshx
 */
public class BeanCodec<T> implements CollectibleCodec<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(BeanCodec.class);
    private static final String ID_FIELD = "_id";

    private final Codec<Document> documentCodec;
    private Class<T> tClass;

    public BeanCodec() {
        documentCodec = new DocumentCodec();
    }

    @Override
    public T generateIdIfAbsentFromDocument(T t) {
        return t;
    }

    @Override
    public boolean documentHasId(T t) {
        return false;
    }

    @Override
    public BsonValue getDocumentId(T t) {
        return null;
    }

    @Override
    public T decode(BsonReader bsonReader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(bsonReader, decoderContext);
        Class<T> tClass = getTClass();
        try {
            T t = tClass.newInstance();
            Field[] fields = tClass.getDeclaredFields();
            for (Field field : fields) {
                setField(t, document, field);
            }
            return t;
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return null;
    }

    @Override
    public void encode(BsonWriter bsonWriter, T t, EncoderContext encoderContext) {
        Document document = new Document();
        Field[] fields = tClass.getDeclaredFields();
        for (Field field : fields) {
            setColumn(t, document, field);
        }
        documentCodec.encode(bsonWriter, document, encoderContext);
    }

    @Override
    public Class<T> getEncoderClass() {
        return getTClass();
    }

    /**
     * 填充document的值
     *
     * @param obj
     * @param document
     * @param field
     */
    private void setColumn(Object obj, Document document, Field field) {
        try {
            field.setAccessible(true);
            // 非transient
            if (!field.isAnnotationPresent(Transient.class)) {
                // 自定义columnName
                String columnName;
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columnName = column.name();
                } else {
                    columnName = getColumnName(field.getName());
                }
                if (obj == null) {
                    document.put(columnName, null);
                } else {
                    // get方法获取字段值
                    String methodGetName = getMethodGetName(field);
                    Method method = obj.getClass().getMethod(methodGetName);
                    Object value = method.invoke(obj);
                    // 处理自定义泛型
                    value = setGenericColumn(field, value);
                    if (!ID_FIELD.equals(columnName)) {
                        document.put(columnName, value);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    /**
     * 填充实体的值
     *
     * @param obj
     * @param document
     * @param field
     */
    private void setField(Object obj, Document document, Field field) {
        try {
            field.setAccessible(true);
            // 非transient
            if (!field.isAnnotationPresent(Transient.class)) {
                // 是否自定义column
                String columnName;
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columnName = column.name();
                } else {
                    columnName = getColumnName(field.getName());
                }
                // 获取列的值
                Object value;
                if (ID_FIELD.equals(columnName)) {
                    value = document.getObjectId(ID_FIELD).toString();
                } else {
                    Object defaultVal = field.get(obj);
                    value = document.get(columnName);
                    if (value == null && defaultVal != null) {
                        value = defaultVal;
                    }
                }
                // 泛型字段
                value = setGenericField(field, value);
                // set方法设置字段值
                String setMethodName = getMethodSetName(field);
                Class<?> fieldClass = field.getType();
                Method method = obj.getClass().getMethod(setMethodName, fieldClass);
                method.invoke(obj, value);
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private Class<T> getTClass() {
        if (tClass == null) {
            tClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return tClass;
    }

    private Object setGenericColumn(Field field, Object value) {
        if (value == null) {
            return value;
        }
        if (field.getType() == List.class) {
            // 泛型List
            Class genericType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (isGenericType(genericType)) {
                List<Document> documents = new ArrayList<>();
                Field[] fields = genericType.getDeclaredFields();
                for (Object obj : (List) value) {
                    Document document = new Document();
                    for (Field f : fields) {
                        setColumn(obj, document, f);
                    }
                    documents.add(document);
                }
                value = documents;
            }
        } else if (isGenericType(field.getType())) {
            // 泛型实体
            Field[] fields = field.getType().getDeclaredFields();
            Document document = new Document();
            for (Field f : fields) {
                setColumn(value, document, f);
            }
            value = document;
        }
        return value;
    }

    private Object setGenericField(Field field, Object value) {
        if (value == null) {
            return value;
        }
        Object object = value;
        if (field.getType() == List.class) {
            // 获取数组泛型实例化递归设置字段值
            Class genericType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (isGenericType(genericType)) {
                List genericList = new ArrayList();
                Field[] fields = genericType.getDeclaredFields();
                for (Document document : (List<Document>) value) {
                    try {
                        Object genericObj = genericType.newInstance();
                        for (Field genericField : fields) {
                            setField(genericObj, document, genericField);
                        }
                        genericList.add(genericObj);
                    } catch (Exception e) {
                        LOGGER.error("初始化List泛型对象失败，filed = {}", field.getName(), e);
                    }
                }
                object = genericList;
            }
        } else if (isGenericType(field.getType())) {
            // 泛型实体递归设置字段
            try {
                Field[] fields = field.getType().getDeclaredFields();
                object = field.getType().newInstance();
                for (Field f : fields) {
                    setField(object, (Document) value, f);
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
        return object;
    }

    /**
     * 判断是否为自定义类型
     *
     * @param clazz
     * @return
     */
    private boolean isGenericType(Class<?> clazz) {
        return !clazz.isPrimitive() && !Number.class.isAssignableFrom(clazz) && !String.class.isAssignableFrom(clazz) && !Boolean.class.isAssignableFrom(clazz)
                && !Map.class.isAssignableFrom(clazz) && !BasicDBObject.class.isAssignableFrom(clazz) && !BasicDBList.class.isAssignableFrom(clazz);
    }

    private String getColumnName(String fieldName) {
        StringBuilder column = new StringBuilder(fieldName);
        int count = 0;
        for (int i = 0; i < fieldName.length(); i++) {
            char s = fieldName.charAt(i);
            // 大写字母转下划线加小写字母
            if (s >= 'A' && s <= 'Z' && i > 0) {
                char sBefore = fieldName.charAt(i - 1);
                // 前一个字母非大写
                if (sBefore < 'A' || sBefore > 'Z') {
                    column.replace(i + count, i + count + 1, "_" + Character.toLowerCase(s));
                    count++;
                }
            }
        }
        return column.toString();
    }

    private String getMethodSetName(Field field) {
        String name = field.getName();
        return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String getMethodGetName(Field field) {
        String name = field.getName();
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
