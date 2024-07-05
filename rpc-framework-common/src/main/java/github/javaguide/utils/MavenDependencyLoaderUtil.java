package github.javaguide.utils;


import github.javaguide.Hello;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述
 *
 * @author: gusang
 * @date: 2024年06月30日 17:32
 */
public class MavenDependencyLoaderUtil {
//    public static void main(String[] args) {
//        List<String> ans = MavenDependencyLoaderUtil.findInterfacesInPackage("github.javaguide");
//        for(String str:ans){
//            System.out.println(str);
//        }
//    }
    public static List<String> findInterfacesInPackage(String packageName) {
        List<String> interfaceNames = new ArrayList<>();

        // Get all classes in the package and its sub-packages
        List<Class<?>> classes = getClasses(packageName);
        for (Class<?> clazz : classes) {
            // Check if class is an interface
            if (clazz.isInterface()) {
                interfaceNames.add(clazz.getName());
            }
        }

        return interfaceNames;
    }

    public static List<Class<?>> getClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String[] classpathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
            for (String classpathEntry : classpathEntries) {
                File baseDir = new File(classpathEntry);
                File packageDir = new File(baseDir, path);
                if (!packageDir.exists()) {
                    continue;
                }
                for (File file : packageDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            // Handle the exception
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }
}
