package cn.taketoday.context.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-0? ?
 */
@Slf4j
public abstract class ClassUtils {

	/** all the class in class path */
	private static Set<Class<?>>			clazz_cache;
	/** class loader **/
	private static ClassLoader				classLoader;

	private static Map<Method, String[]>	PARAMS_NAMES_CACHE	= new ConcurrentHashMap<>(16);

	static {
		
		classLoader = ClassUtils.class.getClassLoader();
		clazz_cache = scanPackage("");
	}

	/**
	 * clear cache
	 */
	public static void clearCache() {
		if(PARAMS_NAMES_CACHE != null) {
			PARAMS_NAMES_CACHE.clear();
		}
		if(clazz_cache != null) {
			clazz_cache.clear();
		}
	}

	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	public static Set<Class<?>> getClassCache() {
		return clazz_cache;
	}

	/**
	 * scan class with given package.
	 * 
	 * @param basePackage
	 *            the package to scan
	 * @return class set
	 */
	public static Set<Class<?>> scanPackage(String basePackage) {

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
							} catch (ClassNotFoundException e) {
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

			if (file.isDirectory()) { // recursive

				String scanPackage = packageName + "." + file.getName();
				if (scanPackage.startsWith(".")) {
					scanPackage = scanPackage.replaceFirst("[.]", "");
				}
				findAllClass(scanPackage, file.getAbsolutePath(), classes);
			} else {

				String className = packageName + '.'
						+ file.getName().substring(0, file.getName().length() - ".class".length());

				if (className.startsWith(".")) {
					className = className.replaceFirst("[.]", "");
				}
				try {
					// log.debug("find class -> [{}]", className);
					classes.add(classLoader.loadClass(className.replaceAll("/", "."))); // add
				} catch (ClassNotFoundException e) {
					log.error("can't find class", e);
				}
			}
		}
	}

//	public final static String[] getMethodArgsNames(Class<?> clazz, Method method) {
//
//		if (PARAMS_NAMES_CACHE.containsKey(method)) {
//			return PARAMS_NAMES_CACHE.get(method);
//		}
//
//		try {
//
//			final String methodName = method.getName();
//			final String className = clazz.getName();
//
//			ClassPool pool = ClassPool.getDefault();
//
//			pool.insertClassPath(new ClassClassPath(ClassUtils.class));
//
//			CtClass cc = pool.get(className);
//			CtMethod cm = cc.getDeclaredMethod(methodName);
//			MethodInfo methodInfo = cm.getMethodInfo();
//
//			CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
//
//			String[] paramNames = new String[cm.getParameterTypes().length];
//
//			LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
//					.getAttribute(LocalVariableAttribute.tag);
//
//			if (attr != null) {
//				int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
//				for (int i = 0; i < paramNames.length; i++) {
//					paramNames[i] = attr.variableName(i + pos);
//				}
//				PARAMS_NAMES_CACHE.put(method, paramNames);
//				return paramNames;
//			}
//		} catch (Exception ex) {
//			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
//		}
//		return null;
//	}
	/**
	 * Compare whether the parameter type is consistent
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
	 * find method parameter list, and cache it.
	 * 
	 * @param clazz
	 *            target class
	 * @param method
	 *            target method
	 * @return method parameter list
	 * @throws IOException
	 */
	public static String[] getMethodArgsNames(Class<?> clazz, Method method) {

		if (PARAMS_NAMES_CACHE.containsKey(method)) {
			return PARAMS_NAMES_CACHE.get(method);
		}

		String[] paramNames = new String[method.getParameterCount()];

		String name = clazz.getName().replace('.', '/') + ".class";

		InputStream resourceAsStream = classLoader.getResourceAsStream(name);
		
		ClassReader classReader = null;
		try {
			classReader = new ClassReader(resourceAsStream);
		} catch (IOException ex) {
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
						int i = index - 1;
						// if it is a static method, the first is the parameter
						// if it's not a static method, the first one is "this" and then the parameter
						// of the method
						if (Modifier.isStatic(method.getModifiers())) {
							i = index;
						}
						if (i >= 0 && i < paramNames.length) {
							paramNames[i] = name;
						}
						super.visitLocalVariable(name, desc, signature, start, end, index);
					}
				};
			}
		}, 0);

		PARAMS_NAMES_CACHE.put(method, paramNames);
		return paramNames;
	}
	
}



