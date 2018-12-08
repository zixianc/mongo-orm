package org.mongo.spring.boot.autoconfigure;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import javax.persistence.Table;
import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author chengshx
 * @date 2018年4月10日
 */
public class MongoFactory {

	private ConcurrentHashMap<Class<?>, String> tables = new ConcurrentHashMap<Class<?>, String>();
	private Set<Codec<?>> codecSet = new HashSet<Codec<?>>();
	private MongoDatabase db;

	public MongoFactory() {
	}

	public MongoDatabase getDb() {
		return db;
	}

	/**
	 * 获取自定义解析器集合
	 * @param clazz
	 * @return
	 */
	public <T> MongoCollection<T> getCollection(Class<T> clazz) {
		String collName = getCollectionName(clazz);
		if(collName != null) {
			return getDb().getCollection(collName, clazz);
		}
		return null;
	}

	/**
	 * 获取默认集合
	 * @param collName
	 * @return
	 */
	public MongoCollection<Document> getCollection(String collName) {
		return getDb().getCollection(collName);
	}

	/**
	 * mongodb连接初始化
	 * @param uri mongodb连接
	 * @param dbName mongodbName
	 * @param codecPackage 自定义codec-package
	 */
	public void init(String uri, String dbName, String codecPackage) {
		try {
			// 加载mongo配置
			if(uri == null) {
				throw new IllegalArgumentException("mongo.uri连接没有配置");
			}
			if(dbName == null) {
				// 没有配置dbName从uri截取默认db
				dbName = uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("?"));
				System.out.println("截取默认dbName：" + dbName);
			}
			// 扫描codec
			scan(codecPackage, true);
			// 创建mongo连接
			MongoClientOptions.Builder build = new MongoClientOptions.Builder();
			if(codecSet.size() > 0) {
				CodecRegistry codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(codecSet.toArray(new Codec[codecSet.size()])), MongoClient.getDefaultCodecRegistry());
				build.codecRegistry(codecRegistry);
			}
			MongoClientURI mongoURI = new MongoClientURI(uri, build);
			MongoClient client = new MongoClient(mongoURI);
			db = client.getDatabase(dbName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 批量扫描包
	 * @param scanPackage 扫描路径
	 * @param recursive 是否扫描子包
	 * @throws Exception
	 */
	private void scan(String scanPackage, boolean recursive) throws Exception {
		String scanPath = scanPackage.replaceAll("\\.", "/");
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = loader.getResources(scanPath);
		while (urls.hasMoreElements()){
			URL url = urls.nextElement();
			if (url != null){
				String protocol = url.getProtocol();
				String pkgPath = url.getPath();
				if ("file".equals(protocol)){
					scanClassNameFromDir(scanPackage, pkgPath, recursive);
				}else if ("jar".equals(protocol)){
					scanClassNameFromJar(scanPackage, url, recursive);
				}
			}
		}
	}

	private void scanClassNameFromDir(String scanPackage, String pkgPath, final boolean recursive) throws Exception {
		// 接收 .class 文件 或 类文件夹
		File[] files = new File(pkgPath).listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isFile() && file.getName().endsWith(".class")) || (recursive && file.isDirectory());
			}
		});
		if (files != null){
			for (File f : files) {
				String fileName = f.getName();
				if (f.isFile()){
					String clazzName = getClassName(scanPackage, fileName);
					addCodec(clazzName);
				} else {
					if (recursive){
						String subPkgName = scanPackage + "." + fileName;
						String subPkgPath = pkgPath + "/" + fileName;
						scanClassNameFromDir(subPkgName, subPkgPath, recursive);
					}
				}
			}
		}
	}

	private void scanClassNameFromJar(String scanPackage, URL url, final boolean recursive) throws Exception {
		JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
		JarFile jarFile = jarURLConnection.getJarFile();
		Enumeration<JarEntry> jarEntries = jarFile.entries();
		while (jarEntries.hasMoreElements()){
			JarEntry jarEntry = jarEntries.nextElement();
			String jarEntryName = jarEntry.getName();
			if (jarEntry.isDirectory()){
				continue;
			}
			if (jarEntryName.endsWith(".class")){
				String clazzName = jarEntryName.substring(0, jarEntryName.indexOf('.')).replace('/', '.');
				if (recursive){
					if (clazzName.startsWith(scanPackage)){
						addCodec(clazzName);
					}
				}else {
					int index = clazzName.lastIndexOf(".");
					String pkg;
					if (index != -1){
						pkg = clazzName.substring(0, index);
					}else {
						pkg = "";
					}
					if (pkg.equals(scanPackage)){
						addCodec(clazzName);
					}
				}
			}
		}
	}

	private String getClassName(String pkgName, String fileName) {
		int endIndex = fileName.lastIndexOf(".");
		String clazz = null;
		if (endIndex >= 0) {
			clazz = fileName.substring(0, endIndex);
		}
		String clazzName = null;
		if (clazz != null) {
			clazzName = pkgName + "." + clazz;
		}
		return clazzName;
	}

	private String getCollectionName(Class<?> clazz) {
		String collName;
		collName = tables.get(clazz);
		if(collName == null) {
			Table table = clazz.getAnnotation(Table.class);
			if(table != null && !"".equals(table.name())) {
				collName = table.name();
				tables.put(clazz, collName);
			}
			if(collName == null) {
				throw new IllegalArgumentException(clazz.getName() + "没有指定表名");
			}
		}
		return collName;
	}

	private void addCodec(String className) throws Exception {
		Class<?> clazz = Class.forName(className);
		if(BeanCodec.class.isAssignableFrom(clazz) && BeanCodec.class != clazz) {
			codecSet.add((Codec<?>) clazz.newInstance());
            System.out.println("扫描到codec" + clazz.getName());
		}
	}

}
