# mongo-orm
主要基于javax.persistence规范实现字段的映射和集合的映射，借助mogodb-java-driver提供的默认DocumentCodec实现自定义编解码器的功能

* 驼峰字段自动映射  
在业务中我们通常会把实体的字段声明成驼峰形式，而表结构会设计成小写字母和下滑线的形式，要做到字段的关联就需要手动做一个字段的映射，mongo-orm会自动映射驼峰字段
* 自定义字段名称  
可通过@Column自定义映射字段名称
```
@Column(name = "just_test")
private String justForTest;
```
* 自定义剔除不需映射的字段  
可通过@Transient注解排除无需持久化的字段
```
@Transient
private String test;
```
* 支持@Table关联实体和集合
```java
@Table(name = "t_comment")
public class Comment {}
```
* 快速生成自定义解码器  
通过继承BeanCodec即完成自定义codec

* 支持多数据源

# 快速上手

* 添加依赖
```
<dependency>
    <groupId>top.newleaf</groupId>
    <artifactId>mongo-orm</artifactId>
    <version>1.0.0</version>
</dependency>
```

* 初始化mongo-orm
`
MongoFactory.getInstatnce().init(confPath);
`

* 按照需要使用javax.persistence注解注释字段和实体（暂时只限上述介绍的功能）

```java
@Table(name = "t_comment")
public class Comment {

    private Long id;

    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                '}';
    }
}
```

* 继承BeanCodec传入实体泛型
```java
public class CommentCodec extends BeanCodec<Comment> {
}

```

* 配置uri、name、扫描路径、默认库
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mongo>

    <packages>
        <package>top.newleaf.test</package>
        <package>top.newleaf.codec</package>
    </packages>

    <connections>
        <connection>
            <uri><![CDATA[mongodb://user:url:port/dbname1?replicaSet=port&authSource=admin&journal=true]]></uri>
            <db>dbname1</db>
            <name>db1</name>
            <isDefault>true</isDefault>
        </connection>

        <connection>
            <uri><![CDATA[mongodb://user:url:port/dbname2?replicaSet=port&authSource=admin&journal=true]]></uri>
            <db>dbname2</db>
            <name>db2</name>
        </connection>
    </connections>
</mongo>
```

* 愉快的使用

```java
public class CommentDAO {

    private MongoDB mongoDB = MongoFactory.getDb();
    
    public Comment getComment(long id) {
        return mongoDB.getCollection(Comment.class).find(Filters.eq("id", id)).first();
    }
    
    public void insert(Comment comment) {
        mongoFactory.getCollection(Comment.class).insertOne(comment);
    }
    
    public void update(Comment comment) {
        mongoDB.getCollection(Comment.class).replaceOne(Filters.eq("id", comment.getId()), comment);
    }
}
```