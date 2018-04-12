package com.yhj.web.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * 类工具
 * @author Today
 */
public final class ClassHelper {

	/**
	 * 获取类加载器 获取加载器类的实现比较简单，只需获取当前线程的ClassLoader
	 */
	public static ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * 获取指定包名下所有的类； 获取指定包名下所有的类，需要根据包名并将其转换为文件路径，读取class文件或jar包，获取指定的类名去加载类
	 */
	public static Set<Class<?>> getClassSet(String packageName) {

		Set<Class<?>> classSet = new HashSet<Class<?>>();
		try {
			Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				if (url != null) {
					String protocol = url.getProtocol();
					if ("file".equals(protocol)) {
						//空格%20
						String packagePath = url.getPath().replace("%20", " ");
						//设置全限定类名
						setClassName(classSet, packagePath, packageName);
					} else if ("jar".equals(protocol)) {
						JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
						if (jarURLConnection != null) {
							JarFile jarFile = jarURLConnection.getJarFile();
							if (jarFile != null) {
								Enumeration<JarEntry> jarEntries = jarFile.entries();
								while (jarEntries.hasMoreElements()) {
									JarEntry jarEntry = jarEntries.nextElement();
									String jarEntryName = jarEntry.getName();
									if (jarEntryName.endsWith(".class")) {
										String className = jarEntryName.substring(0, jarEntryName.lastIndexOf("."))
												.replaceAll("/", ".");
										doAddClass(classSet, className);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("get class set failure.");
		}
		return classSet;
	}

	private final static void setClassName(Set<Class<?>> classSet, String packagePath, String packageName) {
		File[] files = new File(packagePath).listFiles(new FileFilter() {
//			@Override
			public boolean accept(File file) {
				return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
			}
		});
		for (File file : files) {
			String fileName = file.getName();
			if (file.isFile()) {
				//是class文件就得到他的全限定名
				String className = fileName.substring(0, fileName.lastIndexOf("."));
				if (!isEmpty(packageName)) {
					className = packageName + "." + className;
					doAddClass(classSet, className);
				}
			} else {
				String subPackagePath = fileName;
				if (!isEmpty(packageName)) {
					subPackagePath = packagePath + subPackagePath;
					fileName = packageName + "." + fileName;
					System.out.println(subPackagePath);
					System.out.println(fileName);
					setClassName(classSet, subPackagePath, fileName);
				}
			}
		}
	}

	private final static void doAddClass(Set<Class<?>> classSet, String className) {
		Class<?> cls = loadClass(className, false);
		classSet.add(cls);
	}
	
	/**
	 * 加载类 加载类需要提供类名与是否初始化的标志，这里提到的初始化指是否执行类的静态代码块;
	 * 为了提高加载类的性能，可以将loadClass方法的isInitialized参数设置false
	 */
	public static Class<?> loadClass(String className, boolean isInitialized) {
		try {
			// 进行类加载
			return Class.forName(className, isInitialized, getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("load class failure.");
		}
	}

	public final static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

}
