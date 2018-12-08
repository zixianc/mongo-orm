package org.mongo.spring.boot.autoconfigure;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.*;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/**
 * @author chengshx
 * @date 2018/11/25
 */
public class BeanCodec<T> implements CollectibleCodec<T> {

    private static final String ID_FIELD = "_id";

    private final Codec<Document> documentCodec;
    private Class<T> tClass;

    public BeanCodec() {
        documentCodec = new DocumentCodec();
    }

    public T generateIdIfAbsentFromDocument(T t) {
        return t;
    }

    public boolean documentHasId(T t) {
        return false;
    }

    public BsonValue getDocumentId(T t) {
        return null;
    }

    public T decode(BsonReader bsonReader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(bsonReader, decoderContext);
        Class<T> tClass = getTClass();
        try {
            T t = tClass.newInstance();
            Field[] fields = tClass.getDeclaredFields();
            for(Field field : fields) {
                setField(t, document, field);
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void encode(BsonWriter bsonWriter, T t, EncoderContext encoderContext) {
        Document document = new Document();
        Field[] fields = tClass.getDeclaredFields();
        for(Field field : fields) {
            setColumn(t, document, field);
        }
        documentCodec.encode(bsonWriter, document, encoderContext);
    }

    public Class<T> getEncoderClass() {
        return getTClass();
    }

    /**
     * 填充document的值
     * @param t
     * @param document
     * @param field
     */
    private void setColumn(T t, Document document, Field field) {
        try {
            field.setAccessible(true);
            // 非transient
            if(!field.isAnnotationPresent(Transient.class)) {
                // 是否自定义column
                String columnName;
                Column column = field.getAnnotation(Column.class);
                if(column != null) {
                    columnName = column.name();
                } else {
                    columnName = getColumnName(field.getName());
                }
                // get方法获取字段值
                String methodGetName = getMethodGetName(field);
                Method method = t.getClass().getMethod(methodGetName);
                Object value = method.invoke(t);
                if (!ID_FIELD.equals(columnName)) {
                    document.put(columnName, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 填充实体的值
     * @param t
     * @param document
     * @param field
     */
    private void setField(T t, Document document, Field field) {
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
                    Object defaultVal = field.get(t);
                    if(defaultVal != null) {
                        value = document.get(columnName, defaultVal);
                    } else {
                        value = document.get(columnName);
                    }
                }
                // set方法设置字段值
                String setMethodName = getMethodSetName(field);
                Class<?> fieldClass = field.getType();
                Method method = t.getClass().getMethod(setMethodName, fieldClass);
                method.invoke(t, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Class<T> getTClass() {
        if(tClass == null) {
            tClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return tClass;
    }

    private String getColumnName(String fieldName) {
        StringBuilder column = new StringBuilder(fieldName);
        int count = 0;
        for(int i = 0; i < fieldName.length(); i++) {
            char s = fieldName.charAt(i);
            // 大写字母转下划线加小写字母
            if(s >= 'A' && s <= 'Z' && i > 0) {
                char sBefore = fieldName.charAt(i - 1);
                // 前一个字母非大写
                if(sBefore < 'A' || sBefore > 'Z') {
                    column.replace(i + count, i +  count + 1, "_" + Character.toLowerCase(s));
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
