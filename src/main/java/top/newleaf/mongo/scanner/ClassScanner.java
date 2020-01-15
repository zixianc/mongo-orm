package top.newleaf.mongo.scanner;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author chengshx
 */
public class ClassScanner {

    /**
     * 批量扫描包
     *
     * @param scanPackages 扫描路径
     * @param recursive    是否扫描子包
     * @throws Exception
     */
    public static void scan(Set<String> scanPackages, boolean recursive, IScanAction scanAction) throws Exception {
        if (scanPackages != null && !scanPackages.isEmpty()) {
            for (String scanPackage : scanPackages) {
                String scanPath = scanPackage.replaceAll("\\.", "/");
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Enumeration<URL> urls = loader.getResources(scanPath);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if (url != null) {
                        String protocol = url.getProtocol();
                        String pkgPath = url.getPath();
                        if ("file".equals(protocol)) {
                            scanClassNameFromDir(scanPackage, pkgPath, recursive, scanAction);
                        } else if ("jar".equals(protocol)) {
                            scanClassNameFromJar(scanPackage, url, recursive, scanAction);
                        }
                    }
                }
            }
        }
    }

    private static void scanClassNameFromDir(String scanPackage, String pkgPath, final boolean recursive, IScanAction scanAction) {
        // 接收 .class 文件 或 类文件夹
        File[] files = new File(pkgPath).listFiles(file -> (file.isFile() && file.getName().endsWith(".class")) || (recursive && file.isDirectory()));
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if (f.isFile()) {
                    String clazzName = getClassName(scanPackage, fileName);
                    scanAction.doAction(clazzName);
                } else {
                    if (recursive) {
                        String subPkgName = scanPackage + "." + fileName;
                        String subPkgPath = pkgPath + "/" + fileName;
                        scanClassNameFromDir(subPkgName, subPkgPath, recursive, scanAction);
                    }
                }
            }
        }
    }

    private static void scanClassNameFromJar(String scanPackage, URL url, final boolean recursive, IScanAction scanAction) throws Exception {
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName();
            if (jarEntry.isDirectory()) {
                continue;
            }
            if (jarEntryName.endsWith(".class")) {
                String clazzName = jarEntryName.substring(0, jarEntryName.indexOf('.')).replace('/', '.');
                if (recursive) {
                    if (clazzName.startsWith(scanPackage)) {
                        scanAction.doAction(clazzName);
                    }
                } else {
                    int index = clazzName.lastIndexOf(".");
                    String pkg;
                    if (index != -1) {
                        pkg = clazzName.substring(0, index);
                    } else {
                        pkg = "";
                    }
                    if (pkg.equals(scanPackage)) {
                        scanAction.doAction(clazzName);
                    }
                }
            }
        }
    }

    private static String getClassName(String pkgName, String fileName) {
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
}
