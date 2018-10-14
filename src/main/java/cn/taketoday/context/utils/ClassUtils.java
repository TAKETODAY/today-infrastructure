/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import cn.taketoday.context.exception.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Today <br>
 *         2018-06-0? ?
 */
public abstract class ClassUtils {

	private static Logger log = LoggerFactory.getLogger(ClassUtils.class);

	/** all the class in class path */
	private static Collection<Class<?>> clazz_cache;
	/** class loader **/
	private static ClassLoader classLoader;

	private static Map<Method, String[]> PARAMS_NAMES_CACHE = new HashMap<>(16);

	static {
		classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = ClassUtils.class.getClassLoader();
		}
		if (classLoader == null) {
			classLoader = ClassLoader.getSystemClassLoader();
		}
	}

	/**
	 * 
	 * @param cls
	 * @return
	 */
	public static final boolean isCollection(Class<?> cls) {
		return Collection.class.isAssignableFrom(cls);
	}

	/**
	 * load class
	 * 
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static final Class<?> forName(String name) throws ClassNotFoundException {
		return classLoader.loadClass(name);
	}

	/**
	 * Find class by annotation.
	 * 
	 * @param annotationClass
	 *            annotation class
	 * @return the set of class
	 */
	public static final Collection<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
		return getClassCache()//
//				.stream()//
				.parallelStream()//
				.filter(clazz -> clazz.isAnnotationPresent(annotationClass))//
				.collect(Collectors.toSet());
	}

	/**
	 * 
	 * @param annotationClass
	 * @return
	 */
	public static final Collection<Class<?>> getImplClasses(Class<?> interfaceClass) {
		return getClassCache()//
				.parallelStream()//
				.filter(clazz -> interfaceClass.isAssignableFrom(clazz) && interfaceClass != clazz)//
				.collect(Collectors.toSet());
	}

	public static final Collection<Class<?>> getImplClasses(Class<?> interfaceClass, String packageName) {
		return getClassCache()//
				.parallelStream()//
				.filter((clazz) -> clazz.getName().contains(packageName) && interfaceClass.isAssignableFrom(clazz))//
				.collect(Collectors.toSet());
	}

	/**
	 * clear cache
	 */
	public static final void clearCache() {
		if (PARAMS_NAMES_CACHE != null) {
			PARAMS_NAMES_CACHE.clear();
		}
		if (clazz_cache != null) {
			clazz_cache.clear();
		}
	}

	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	public static Collection<Class<?>> getClassCache() {
		if (clazz_cache == null) {
			clazz_cache = scanPackage("");
		}
		return clazz_cache;
	}

	public static final Collection<Class<?>> scan(String dir) {
		Set<Class<?>> clazz = new HashSet<Class<?>>();

		try {
			URL url = new URL("file:/" + dir);
			
//			File file = new File(dir);
			
			
			final String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				final String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				findAllClass("", filePath, clazz);// scan all class and set collection
			} else if ("jar".equals(protocol)) {
				JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
				if (jarURLConnection == null) {
					log.warn("Can't open connection with [{}].", dir);
				}
				JarFile jarFile = jarURLConnection.getJarFile();
				if (jarFile == null) {
					log.warn("Can't open jar file with [{}].", dir);
				}
				Enumeration<JarEntry> jarEntries = jarFile.entries();
				while (jarEntries.hasMoreElements()) {
					JarEntry jarEntry = jarEntries.nextElement();
					String jarEntryName = jarEntry.getName();
					if (jarEntryName.contains("today") && jarEntryName.endsWith(".class")) {
						String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/",
								".");
						try {
							
							clazz.add(classLoader.loadClass(className));
						} //
						catch (ClassNotFoundException e) {
							log.error("can't find class", e);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("exception occur", e);
		}
		return clazz;
	}

	/**
	 * scan class with given package.
	 * 
	 * @param basePackage
	 *            the package to scan
	 * @return class set
	 */
	public static Collection<Class<?>> scanPackage(String basePackage) {

		Set<Class<?>> clazz = new HashSet<Class<?>>();

		if (StringUtils.isEmpty(basePackage)) {
			basePackage = "";
		} else {
			basePackage = basePackage.replace('.', '/');
		}
		try {
			Enumeration<URL> uri = classLoader.getResources(basePackage);

			while (uri.hasMoreElements()) {
				URL url = uri.nextElement();
				final String protocol = url.getProtocol();
				if ("file".equals(protocol)) {
					final String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					findAllClass(basePackage, filePath, clazz);// scan all class and set collection
				} else if ("jar".equals(protocol)) {
					JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
					if (jarURLConnection == null) {
						continue;
					}
					JarFile jarFile = jarURLConnection.getJarFile();
					if (jarFile == null) {
						continue;
					}
					Enumeration<JarEntry> jarEntries = jarFile.entries();
					while (jarEntries.hasMoreElements()) {
						JarEntry jarEntry = jarEntries.nextElement();
						String jarEntryName = jarEntry.getName();
						if (jarEntryName.contains("today") && jarEntryName.endsWith(".class")) {
							String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/",
									".");
							try {

								clazz.add(classLoader.loadClass(className));
							} //
							catch (ClassNotFoundException e) {
								log.error("can't find class", e);
							}
						}
					}
				}
			}
			return clazz;
		} catch (IOException e) {
			log.error("io exception occur", e);
		}
		return null;
	}

	/**
	 * 
	 * 
	 * @param packageName
	 *            the name of package
	 * @param packagePath
	 *            the package physical path
	 * @param classes
	 *            class set
	 */
	private static void findAllClass(String packageName, String packagePath, Set<Class<?>> classes) {

		File directory = new File(packagePath);

		if (!directory.exists() || !directory.isDirectory()) {
			log.error("the package -> [{}] you provided that contains nothing", packageName);
			return;
		}

		// log.debug("enter package -> [{}]", packageName);

		// exists
		File[] directoryFiles = directory
				.listFiles(file -> (file.isDirectory()) || (file.getName().endsWith(".class")));

		if (directoryFiles == null) {
			log.error("the package -> [{}] you provided that contains nothing", packageName);
			return;
		}

		for (File file : directoryFiles) { //

			String fileName = file.getName();

			if (file.isDirectory()) { // recursive

				String scanPackage = packageName + "." + fileName;
				if (scanPackage.startsWith(".")) {
					scanPackage = scanPackage.replaceFirst("[.]", "");
				}
				findAllClass(scanPackage, file.getAbsolutePath(), classes);
				continue;
			}
			if (fileName.contains("package-info")) {
				continue;
			}

			String className = packageName + '.' + fileName.substring(0, fileName.length() - ".class".length());

			try {

				classes.add(classLoader.loadClass(className.replaceAll("/", "."))); // add
			} //
			catch (ClassNotFoundException e) {
				log.warn("Can't find class -> [{}]", className);
			}
		}
	}

	/**
	 * Compare whether the parameter type is consistent.
	 *
	 * @param types
	 *            the type of the asm({@link Type})
	 * @param classes
	 *            java type({@link Class})
	 * @return return param type equals
	 */
	private static boolean sameType(Type[] types, Class<?>[] classes) {
		if (types.length != classes.length)
			return false;
		for (int i = 0; i < types.length; i++) {
			if (!Type.getType(classes[i]).equals(types[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Find method parameter list, and cache it.
	 * 
	 * @param clazz
	 *            target class
	 * @param method
	 *            target method
	 * @return method parameter list
	 * @throws IOException
	 */
	public static String[] getMethodArgsNames(Method method) {

		if (PARAMS_NAMES_CACHE.containsKey(method)) {
			return PARAMS_NAMES_CACHE.get(method);
		}

		Class<?> declaringClass = method.getDeclaringClass();
		String[] paramNames = new String[method.getParameterCount()];
		String name = declaringClass.getName().replace('.', '/') + ".class";

		InputStream resourceAsStream = classLoader.getResourceAsStream(name);

		ClassReader classReader = null;
		try {

			classReader = new ClassReader(resourceAsStream);
		} //
		catch (IOException ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}

		classReader.accept(new ClassVisitor(Opcodes.ASM6) {

			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String desc,
					final String signature, final String[] exceptions) {
				final Type[] args = Type.getArgumentTypes(desc);
				// The method name is the same and the number of parameters is the same
				if (!name.equals(method.getName()) || !sameType(args, method.getParameterTypes())) {
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
				MethodVisitor v = super.visitMethod(access, name, desc, signature, exceptions);

				return new MethodVisitor(Opcodes.ASM6, v) {
					@Override
					public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
							int index) {
						// if it is a static method, the first is the parameter
						// if it's not a static method, the first one is "this" and then the parameter
						// of the method
						if (!Modifier.isStatic(method.getModifiers())) {
							index = index - 1;
						}

						if (index >= 0 && index < paramNames.length) {
							paramNames[index] = name;
						}
						super.visitLocalVariable(name, desc, signature, start, end, index);
					}
				};
			}
		}, 0);

		PARAMS_NAMES_CACHE.put(method, paramNames);
		return paramNames;
	}

	/**
	 * 
	 * @param clazz
	 * @param annotationClass
	 * @param implClass
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] getClassAnntation(Class<?> clazz, Class<T> annotationClass, Class<? extends T> implClass)
			throws Exception {

		List<T> targetInstance = new ArrayList<>();

		Annotation[] annotations = clazz.getAnnotations();// clazz 的注解
		Method[] declaredMethods = annotationClass.getDeclaredMethods();

		for (Annotation targetClassAnno : annotations) {
			// clazz 每一个注解
			Class<? extends Annotation> annotationType = targetClassAnno.annotationType();

			if (annotationType == annotationClass) {// 如果等于对象注解就直接添加
				targetInstance.add((T) targetClassAnno);
				continue;
			}
			// 不是对象注解就转换
			// 获取每一个注解上的所有注解
			Annotation[] allAnnoAnnotations = annotationType.getAnnotations();
			// 对所有注解转换
			convert(annotationClass, implClass, targetInstance, declaredMethods, targetClassAnno, annotationType,
					allAnnoAnnotations);
		}

		return targetInstance.toArray((T[]) Array.newInstance(annotationClass, targetInstance.size()));
	}

	private static <T> void convert(Class<T> annotationClass, Class<? extends T> implClass, List<T> object,
			Method[] declaredMethods, Annotation targetAnnoInstance, Class<?> targetAnnoType,
			Annotation[] allAnnoAnnotations) throws Exception {

		// 对所有的注解(targetClassAnno)上的注解遍历，判断是否是annotationClass
		for (Annotation annotation_ : allAnnoAnnotations) {
			// annotation 上的 class
			Class<?> annotationType_ = annotation_.annotationType();
			if (annotationType_ != annotationClass) {
				continue;
			}
			T newInstance = implClass.getConstructor().newInstance();

			for (Method method : declaredMethods) {// 遍历对象class的方法
				String name = method.getName();
				Field field = implClass.getDeclaredField(name);
				field.setAccessible(true);
				Method declaredMethod = null;
				try {
					declaredMethod = targetAnnoType.getMethod(name);
				} catch (NoSuchMethodException e) {
					//
					field.set(newInstance, annotation_.annotationType().getMethod(name).invoke(annotation_));
					continue;
				}
				Object value = declaredMethod.invoke(targetAnnoInstance);
				if (field.getType().isArray() && !value.getClass().isArray()) {
					Object array = Array.newInstance(field.getType().getComponentType(), 1);
					Array.set(array, 0, value);
					value = array;
				}
				field.set(newInstance, value);
			}
			object.add(newInstance);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] getMethodAnntation(Method method, Class<T> annotationClass, Class<? extends T> implClass)
			throws Exception {
		List<T> targetInstance = new ArrayList<>();

		Annotation[] annotations = method.getAnnotations();// clazz 的注解
		Method[] declaredMethods = annotationClass.getDeclaredMethods();

		for (Annotation targetClassAnno : annotations) {
			// clazz 每一个注解

			Class<? extends Annotation> annotationType = targetClassAnno.annotationType();

			if (annotationType == annotationClass) {// 如果等于对象注解就直接添加
				targetInstance.add((T) targetClassAnno);
				continue;
			}
			// 不是对象注解就转换
			// 获取每一个注解上的所有注解
			Annotation[] allAnnoAnnotations = annotationType.getAnnotations();
			// 对所有注解转换
			convert(annotationClass, implClass, targetInstance, declaredMethods, targetClassAnno, annotationType,
					allAnnoAnnotations);
		}

		return targetInstance.toArray((T[]) Array.newInstance(annotationClass, targetInstance.size()));
	}

	/**
	 * 
	 * @param targetClassAnno
	 * @param annotationClass
	 * @param implClass
	 * @return
	 * @throws Exception
	 */
	public static <T> T getAnntation(Annotation targetClassAnno, Class<? extends Annotation> annotationClass,
			Class<? extends T> implClass) throws Exception {
		return convert(implClass, targetClassAnno, annotationClass.getDeclaredMethods());
	}

	/**
	 * 
	 * @param implClass
	 * @param targetInstance
	 * @param declaredMethods
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws NoSuchFieldException
	 * @throws ConfigurationException
	 */
	private static <T> T convert(Class<? extends T> implClass, Object targetInstance, Method[] declaredMethods)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			NoSuchFieldException, ConfigurationException {

		T newInstance = implClass.getConstructor().newInstance();// instance

		for (Method method : declaredMethods) {

			Field field = implClass.getDeclaredField(method.getName());

			if (field == null) {
				throw new ConfigurationException("you must specify a field named -> {}.", method.getName());
			}
			field.setAccessible(true);
			field.set(newInstance, method.invoke(targetInstance));
		}

		return newInstance;
	}

}
