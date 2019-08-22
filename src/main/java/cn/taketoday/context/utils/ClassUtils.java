/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.ThrowableSupplier;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.asm.ClassReader;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Label;
import cn.taketoday.context.asm.MethodVisitor;
import cn.taketoday.context.asm.Opcodes;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.io.FileBasedResource;
import cn.taketoday.context.io.JarEntryResource;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.ResourceFilter;

/**
 * 
 * @author TODAY <br>
 *         2018-06-0? ?
 */
public abstract class ClassUtils {

    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

    public static boolean traceEnabled = log.isTraceEnabled();

    /** class loader **/
    private static ClassLoader classLoader;
    /** scanned classes */
    private static Set<Class<?>> classesCache;

    private static final Map<String, Class<?>> PRIMITIVE_CACHE = new HashMap<>(32);

    /** @since 2.1.1 */
    private static final Set<Class<? extends Annotation>> IGNORE_ANNOTATION_CLASS = new HashSet<>();//

    private static final String[] IGNORE_SCAN_JARS;

    private static boolean ignoreScanJarsPrefix = true;

    private static final ParameterFunction PARAMETER_NAMES_FUNCTION = new ParameterFunction();
    private static final Map<Class<?>, Map<Method, String[]>> PARAMETER_NAMES_CACHE = new HashMap<>(256);

    private static final Map<AnnotationKey<?>, Object> ANNOTATIONS = new WeakHashMap<>(128);

    private static final AnnotationAttributes[] EMPTY_ANNOTATION_ATTRIBUTES = new AnnotationAttributes[0];
    private static final Map<AnnotationKey<?>, AnnotationAttributes[]> ANNOTATION_ATTRIBUTES = new WeakHashMap<>(128);

    /** Class resource filter */
    private static final ResourceFilter CLASS_RESOURCE_FILTER = (Resource resource) -> {
        return resource.isDirectory()//
                || (resource.getName().endsWith(Constant.CLASS_FILE_SUFFIX) //
                        && !resource.getName().startsWith("package-info"));
    };

    static {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassUtils.class.getClassLoader();
        }
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        setClassLoader(classLoader);

        // Load the META-INF/ignore/jar-prefix to ignore some jars
        // --------------------------------------------------------------
        final Set<String> ignoreScanJars = new HashSet<>();

        try { // @since 2.1.6
            final Enumeration<URL> resources = classLoader.getResources("META-INF/ignore/jar-prefix");
            final Charset charset = Constant.DEFAULT_CHARSET;
            while (resources.hasMoreElements()) {
                try (final BufferedReader reader = new BufferedReader(//
                        new InputStreamReader(resources.nextElement().openStream(), charset))) { // fix

                    String str;
                    while ((str = reader.readLine()) != null) {
                        ignoreScanJars.add(str);
                    }
                }
            }
        }
        catch (IOException e) {
            log.error("IOException occurred when load 'META-INF/ignore/jar-prefix'", e);
            throw ExceptionUtils.newContextException(e);
        }
        IGNORE_SCAN_JARS = ignoreScanJars.toArray(Constant.EMPTY_STRING_ARRAY);

        // Map primitive types
        // -------------------------------------------
        final Set<Class<?>> primitiveTypes = new HashSet<>(32);
        Collections.addAll(primitiveTypes, //
                boolean.class, byte.class, char.class, int.class, //
                long.class, double.class, float.class, short.class, //
                boolean[].class, byte[].class, char[].class, double[].class, //
                float[].class, int[].class, long[].class, short[].class//
        );
        primitiveTypes.add(void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            PRIMITIVE_CACHE.put(primitiveType.getName(), primitiveType);
        }

        // Add ignore annotation
        addIgnoreAnnotationClass(Target.class);
        addIgnoreAnnotationClass(Inherited.class);
        addIgnoreAnnotationClass(Retention.class);
        addIgnoreAnnotationClass(Documented.class);
    }

    public static void addIgnoreAnnotationClass(Class<? extends Annotation> annotationClass) {
        IGNORE_ANNOTATION_CLASS.add(annotationClass);
    }

    public static final boolean isCollection(Class<?> cls) {
        return Collection.class.isAssignableFrom(cls);
    }

    /**
     * clear cache
     */
    public static void clearCache() {
        setClassCache(null);

        ANNOTATIONS.clear();
        ANNOTATION_ATTRIBUTES.clear();
    }

    public static void setClassLoader(ClassLoader classLoader) {
        ClassUtils.classLoader = classLoader;
    }

    public static ClassLoader getClassLoader() {
        return ClassUtils.classLoader;
    }

    public static boolean isIgnoreScanJarsPrefix() {
        return ignoreScanJarsPrefix;
    }

    public static void setIgnoreScanJarsPrefix(boolean ignoreScanJars) {
        ClassUtils.ignoreScanJarsPrefix = ignoreScanJars;
    }

    /**
     * get all classes loaded in class path
     */
    public static Set<Class<?>> getClassCache() {

        if (classesCache == null || classesCache.isEmpty()) {
            setClassCache(scan(Constant.BLANK));
        }
        return classesCache;
    }

    public static void setClassCache(Set<Class<?>> classes) {
        ClassUtils.classesCache = classes;
    }

    // ------------------------------------------ Class Scan

    /**
     * Whether given class name present in class path
     * 
     * @param className
     *            class name
     * @return whether given class name present in class path
     */
    public static boolean isPresent(String className) {

        try {
            forName(Objects.requireNonNull(className, "class name can't be null"));
            return true;
        }
        catch (Throwable ex) {
            return false;
        }
    }

    public static Class<?> resolvePrimitiveClassName(String name) {
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if (name != null && name.length() <= 8) {
            // Could be a primitive - likely.
            return PRIMITIVE_CACHE.get(name);
        }
        return null;
    }

    /**
     * Load class .from spring
     * 
     * @param name
     *            a class full name
     * @return a class
     * @throws ClassNotFoundException
     *             when class could not be found
     * @since 2.1.6
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(Constant.ARRAY_SUFFIX)) {
            Class<?> elementClass = //
                    forName(name.substring(0, name.length() - Constant.ARRAY_SUFFIX.length()));
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(Constant.NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            Class<?> elementClass = //
                    forName(name.substring(Constant.NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1));
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(Constant.INTERNAL_ARRAY_PREFIX)) {
            Class<?> elementClass = forName(name.substring(Constant.INTERNAL_ARRAY_PREFIX.length()));
            return Array.newInstance(elementClass, 0).getClass();
        }

        try {
            return Class.forName(name, false, classLoader);
        }
        catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(Constant.PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String innerClassName = name.substring(0, lastDotIndex) //
                        + Constant.INNER_CLASS_SEPARATOR //
                        + name.substring(lastDotIndex + 1);
                try {
                    return Class.forName(innerClassName, false, classLoader);
                }
                catch (ClassNotFoundException ex2) {
                    // Swallow - let original exception get through
                }
            }
            throw ex;
        }
    }

    /**
     * Load class
     * 
     * @param <T>
     *            return class type
     * @param name
     *            class full name
     * @return class if not found will returns null
     */
    public static final <T> Class<T> loadClass(String name) {
        return loadClass(name, classLoader);
    }

    /**
     * Load class with given class name and {@link ClassLoader}
     * 
     * @param <T>
     *            return class type
     * @param name
     *            class gull name
     * @param classLoader
     *            use this {@link ClassLoader} load the class
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final <T> Class<T> loadClass(String name, ClassLoader classLoader) {
        try {
            return (Class<T>) classLoader.loadClass(name);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Find class by annotation.
     * 
     * @param annotationClass
     *            annotation class
     * @return the set of class
     */
    public static Collection<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        return filter(clazz -> clazz.isAnnotationPresent(annotationClass));
    }

    /**
     * Get all child classes in class path
     * 
     * @param superClass
     *            super class or a interface class
     * @return a {@link Collection} of impl class
     */
    public static Set<Class<?>> getImplClasses(Class<?> superClass) {
        return filter(clazz -> superClass.isAssignableFrom(clazz) && superClass != clazz);
    }

    /**
     * Get all child classes in class path filter with package name
     * 
     * @param superClass
     *            super class or a interface class
     * @param packageName
     *            package name
     * @return a {@link Collection} of impl class
     */
    public static Set<Class<?>> getImplClasses(Class<?> superClass, String packageName) {
        return filter(clazz -> clazz.getName().startsWith(packageName) //
                && superClass != clazz//
                && superClass.isAssignableFrom(clazz) //
        );
    }

    public static final <T> Set<Class<?>> filter(final Predicate<Class<?>> predicate) {
        return getClassCache()//
//				.stream()//
                .parallelStream()//
                .filter(predicate)//
                .collect(Collectors.toSet());
    }

    /**
     * Get {@link Collection} of class under the packages
     * 
     * @param packages
     *            package name
     * @return a {@link Collection} of class under the packages
     */
    public static Set<Class<?>> getClasses(final String... packages) {

        if (StringUtils.isArrayEmpty(packages) || //
                (packages.length == 1 && StringUtils.isEmpty(packages[0]))) //
        {
            return getClassCache();
        }

        return filter(clazz -> {
            final String name = clazz.getName();
            for (final String prefix : packages) {
                if (StringUtils.isEmpty(prefix) || name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Scan class with given package.
     * 
     * @param packages
     *            The packages to scan
     * @return Class set
     */
    public static Set<Class<?>> scan(final String... packages) {

        Objects.requireNonNull(packages, "scan package can't be null");

        if (classesCache == null || classesCache.isEmpty()) {

            final Set<Class<?>> scanClasses = new HashSet<>(2048);
            if (packages.length == 1) {
                scanOne(scanClasses, packages[0]); // packages.length == 1
            }
            else {
                final Set<String> packagesToScan = new HashSet<>(8);
                for (final String location : packages) {

                    if (StringUtils.isEmpty(location)) { // contains "" scan all class
                        scan(scanClasses);
                        setClassCache(scanClasses);
                        return scanClasses;
                    }
                    else {
                        packagesToScan.add(location);
                    }
                }
                for (final String location : packagesToScan) {
                    scan(scanClasses, location);
                }
            }
            setClassCache(scanClasses);
            return scanClasses;
        }
        return getClasses(packages);
    }

    protected static void scanOne(final Set<Class<?>> scanClasses, final String location) {

        if (StringUtils.isEmpty(location)) {
            scan(scanClasses);
        }
        else {
            scan(scanClasses, location);
        }
    }

    /**
     * Scan classes to classes set
     * 
     * @param scanClasses
     *            classes set
     * @param packageName
     *            package name
     */
    public static void scan(final Collection<Class<?>> scanClasses, final String packageName) {

        final String resourceToUse = //
                packageName.replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR);

        try {
            if (traceEnabled) {
                log.trace("Scan package: [{}]", packageName);
            }
            final Enumeration<URL> uri = classLoader.getResources(resourceToUse);
            while (uri.hasMoreElements()) {
                scan(scanClasses, ResourceUtils.getResource(uri.nextElement()), packageName);
            }
        }
        catch (IOException e) {
            log.error("IO exception occur With Msg: [{}]", e.getMessage(), e);
            throw new ContextException(e);
        }
    }

    /**
     * Scan class in a {@link Resource}
     * 
     * @param scanClasses
     *            class set
     * @param resource
     *            {@link Resource} in class maybe a jar file or class directory
     * @param packageName
     *            if {@link Resource} is a directory will use this packageName
     * @throws IOException
     * @since 2.1.6
     */
    private static void scan(final Collection<Class<?>> scanClasses, final Resource resource, String packageName) throws IOException {

        if (resource instanceof FileBasedResource) {
            if (resource.isDirectory()) {
                findInDirectory(resource, scanClasses);
                return;
            }
            final String fileName = resource.getName();
            if (fileName.endsWith(".jar")) {
                scanInJarFile(scanClasses, resource, fileName, packageName, () -> new JarFile(resource.getFile()));
            }
        }
        else if (resource instanceof JarEntryResource) {
            scanInJarFile(scanClasses, resource, resource.getFile().getName(), packageName,
                    () -> ((JarEntryResource) resource).getJarFile());
        }
//        else if (resource instanceof ClassPathResource) {
//            scan(scanClasses, ((ClassPathResource) resource).getOriginalResource(), packageName);
//        }
    }

    private static void scanInJarFile(final Collection<Class<?>> scanClasses, final Resource resource, //
            final String fileName, String packageName, final ThrowableSupplier<JarFile, IOException> supplier) throws IOException//
    {
        if (ignoreScanJarsPrefix) {
            for (final String ignoreJarName : IGNORE_SCAN_JARS) {
                if (fileName.startsWith(ignoreJarName)) {
                    return;
                }
            }
        }
        if (traceEnabled) {
            log.trace("Scan in jar file: [{}]", resource.getLocation());
        }
        try (final JarFile jarFile = supplier.get()) {
            final Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                loadClassInJar(jarEntries.nextElement(), packageName, scanClasses);
            }
        }
    }

    protected static String getClassName(final Resource resource) throws IOException {
        try (final InputStream inputStream = resource.getInputStream()) {
            return new ClassReader(inputStream).getClassName()//
                    .replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
        }
    }

    /**
     * Scan all the classpath classes
     * 
     * @param scanClasses
     *            class set
     * @since 2.1.6
     */
    public static void scan(Collection<Class<?>> scanClasses) {

        try {
            if (classLoader instanceof URLClassLoader) {
                // fix: protocol is file not a jar protocol
                for (final URL url : ((URLClassLoader) classLoader).getURLs()) {
                    scan(scanClasses, ResourceUtils.getResource(url), Constant.BLANK);
                }
            }
        }
        catch (IOException e) {
            log.error("IO exception occur With Msg: [{}]", e.getMessage(), e);
            throw new ContextException(e);
        }
    }

    /**
     * Load classes from a {@link JarEntry}
     * 
     * @param jarEntry
     *            the entry of jar
     * @param scanClasses
     *            class set
     */
    public static void loadClassInJar(JarEntry jarEntry, String packageName, Collection<Class<?>> scanClasses) {

        if (jarEntry.isDirectory()) {
            return;
        }
        final String jarEntryName = jarEntry.getName(); // cn/taketoday/xxx/yyy.class
        if (jarEntryName.endsWith(Constant.CLASS_FILE_SUFFIX)) {

            // fix #10 classes loading from a jar can't be load @off
            final String nameToUse = jarEntryName.replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);

            if (StringUtils.isEmpty(packageName) || nameToUse.startsWith(packageName)) {
                try {
                    scanClasses.add(classLoader.loadClass(//
                            nameToUse.substring(0, nameToUse.lastIndexOf(Constant.PACKAGE_SEPARATOR))//
                    ));
                }
                catch (Error | ClassNotFoundException e) {} //@on
            }
        }
    }

    /**
     * <p>
     * Find in directory.
     * </p>
     * Note: don't need packageName
     * 
     * @param packagePath
     *            the package physical path
     * @param scanClasses
     *            class set
     * @throws IOException
     */
    private static void findInDirectory(Resource directory, Collection<Class<?>> scanClasses) //
            throws IOException //
    {
        if (!directory.exists()) {
            log.error("The location: [{}] you provided that does not exist", directory.getLocation());
            return;
        }

        // exists
        if (traceEnabled) {
            log.trace("Enter: [{}]", directory.getLocation());
        }
        for (final Resource resource : directory.list(CLASS_RESOURCE_FILTER)) {
            if (resource.isDirectory()) { // recursive
                findInDirectory(resource, scanClasses);
            }
            else {
                try {
                    scanClasses.add(classLoader.loadClass(getClassName(resource))); // add
                }
                catch (ClassNotFoundException | Error e) {
                    // log.warn("Can't find class: [{}]", className);
                }
            }
        }
    }

    // -------------------------------------------------Annotation

    /**
     * Get the array of {@link Annotation} instance
     * 
     * @param element
     *            annotated element
     * @param annotationClass
     *            target annotation class
     * @param implClass
     *            impl class
     * @return the array of {@link Annotation} instance
     * @since 2.1.1
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T[] getAnnotationArray(
            final AnnotatedElement element,
            final Class<T> annotationClass,
            final Class<? extends T> implClass) throws ContextException //
    {
        Objects.requireNonNull(annotationClass, "annotation class can't be null");

        return (T[]) ANNOTATIONS.computeIfAbsent(new AnnotationKey<>(element, annotationClass), (k) -> {

            final AnnotationAttributes[] annotationAttributes = //
                    getAnnotationAttributesArray(element, annotationClass);

            int i = 0;
            final Object array = Array.newInstance(annotationClass, annotationAttributes.length);

            for (final AnnotationAttributes attributes : annotationAttributes) {
                Array.set(array, i++, injectAttributes(attributes, annotationClass, newInstance(implClass)));
            }
            return array;
        });
    }

    /**
     * Get the array of {@link Annotation} instance
     * 
     * @param element
     *            annotated element
     * @param targetClass
     *            target annotation class
     * @return the array of {@link Annotation} instance
     * @since 2.1.1
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T[] getAnnotationArray(AnnotatedElement element, Class<T> targetClass) {

        Objects.requireNonNull(targetClass, "annotation class can't be null");

        return (T[]) ANNOTATIONS.computeIfAbsent(new AnnotationKey<>(element, targetClass), (k) -> {

            final AnnotationAttributes[] annAttributes = getAnnotationAttributesArray(element, targetClass);

            int i = 0;
            final Object array = Array.newInstance(targetClass, annAttributes.length);

            for (final AnnotationAttributes attributes : annAttributes) {
                Array.set(array, i++, getAnnotationProxy(targetClass, attributes));
            }
            return array;
        });
    }

    /**
     * Get Annotation by reflect
     * 
     * @param element
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @param implClass
     *            The implementation class
     * @return the {@link Collection} of {@link Annotation} instance
     * @since 2.0.x
     */
    public static <A extends Annotation> List<A> getAnnotation(final AnnotatedElement element, //
            final Class<A> annotationClass, final Class<? extends A> implClass) throws ContextException//
    {
        return Arrays.asList(getAnnotationArray(element, annotationClass, implClass));
    }

    /**
     * Inject {@link AnnotationAttributes} by reflect
     * 
     * @param source
     *            Element attributes
     * @param annotationClass
     *            Annotated class
     * @param instance
     *            target instance
     * @return target instance
     * @throws ContextException
     *             if any {@link Exception} occurred
     * @since 2.1.5
     */
    public static <A> A injectAttributes(AnnotationAttributes source, //
            Class<? extends A> annotationClass, A instance) throws ContextException//
    {
        final Class<?> implClass = instance.getClass();
        String name = null;
        try {
            for (final Method method : annotationClass.getDeclaredMethods()) {
                // method name must == field name
                name = method.getName();
                makeAccessible(implClass.getDeclaredField(name)).set(instance, source.get(name));
            }
            return instance;
        }
        catch (NoSuchFieldException e) {
            log.error("You Must Specify A Field: [{}] In Class: [{}]", name, implClass.getName(), e);
            throw new ContextException(e);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("An Exception Occurred When Inject Attributes Attributes, With Msg: [{}]", ex, ex);
            throw ExceptionUtils.newContextException(ex);
        }
    }

    /**
     * Get Annotation Attributes from an annotation instance
     * 
     * @param annotation
     *            annotation instance
     * @return {@link AnnotationAttributes}
     * @since 2.1.1
     */
    public static AnnotationAttributes getAnnotationAttributes(final Annotation annotation) throws ContextException {

        try {

            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final Method[] declaredMethods = annotationType.getDeclaredMethods();
            final AnnotationAttributes attributes = //
                    new AnnotationAttributes(annotationType, declaredMethods.length);

            for (final Method method : declaredMethods) {
                attributes.put(method.getName(), method.invoke(annotation));
            }
            return attributes;
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("An Exception Occurred When Getting Annotation Attributes: [{}]", ex, ex);
            throw ExceptionUtils.newContextException(ex);
        }
    }

    /**
     * Get Annotation by proxy
     * 
     * @param annotatedElement
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @return the {@link Collection} of {@link Annotation} instance
     * @since 2.1.1
     */
    public static <T extends Annotation> List<T> getAnnotation(//
            final AnnotatedElement annotatedElement, final Class<T> annotationClass) throws ContextException//
    {
        return Arrays.asList(getAnnotationArray(annotatedElement, annotationClass));
    }

    /**
     * Get First Annotation
     * 
     * @param annotatedElement
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @return the {@link Collection} of {@link Annotation} instance
     * @since 2.1.7
     */
    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, final AnnotatedElement annotatedElement) {

        final T[] annotationArray = getAnnotationArray(annotatedElement, annotationClass);
        return ObjectUtils.isEmpty(annotationArray) ? null : annotationArray[0];
    }

    /**
	 * Get Annotation by proxy
	 * 
	 * @param annotationClass
	 *            The annotation class
	 * @param attributes
	 *            The annotation attributes key-value
	 * @return the target {@link Annotation} instance
	 * @since 2.1.1
	 * @off
	 */
	public static <T extends Annotation> T getAnnotationProxy(Class<T> annotationClass, AnnotationAttributes attributes) {
		return annotationClass.cast(Proxy.newProxyInstance(classLoader, new Class[] { annotationClass, Annotation.class }, //
				(Object proxy, Method method, Object[] args) -> {
					// The switch statement compares the String object in its expression with the expressions
					// associated with each case label as if it were using the String.equals method;
					// consequently, the comparison of String objects in switch statements is case sensitive. 
					// The Java compiler generates generally more efficient bytecode from switch statements 
					// that use String objects than from chained if-then-else statements.
					switch (method.getName())
					{
						case Constant.EQUALS : 			return eq(attributes, args);
						case Constant.HASH_CODE :		return attributes.hashCode();
						case Constant.TO_STRING :		return attributes.toString();
						case Constant.ANNOTATION_TYPE :	return annotationClass;
						default : 						return attributes.get(method.getName());
					}
				}//
		));
	}
	//@on

    /**
     * Equals
     * 
     * @param attributes
     *            key-value attributes
     * @param args
     *            InvocationHandler args
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @since 2.1.1
     */
    private static Object eq(final AnnotationAttributes attributes, final Object[] args)//
            throws IllegalAccessException, InvocationTargetException //
    {
        final Object object = args[0];
        if (attributes == object) {
            return true;
        }
        if (object instanceof Annotation) {
            for (final Method method_ : object.getClass().getDeclaredMethods()) {
                final Object value_ = attributes.get(method_.getName());
                final Object value = method_.invoke(object);
                if (value == null || value_ == null || !value.equals(value_)) {
                    return false;
                }
            }
            return true;
        }
        if (object instanceof AnnotationAttributes) {
            return object.equals(attributes);
        }
        return false;
    }

    /**
     * Get attributes the 'key-value' of annotations
     * 
     * @param element
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @return a set of {@link AnnotationAttributes}
     * @since 2.1.1
     */
    public static <T extends Annotation> List<AnnotationAttributes> //
            getAnnotationAttributes(final AnnotatedElement element, final Class<T> annotationClass) throws ContextException//
    {
        return Arrays.asList(getAnnotationAttributesArray(element, annotationClass));
    }

    /**
     * Get attributes the 'key-value' of annotations
     * 
     * @param element
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @return a set of {@link AnnotationAttributes}
     * @since 2.1.1
     */
    public static <T extends Annotation> AnnotationAttributes //
            getAnnotationAttributes(final Class<T> annotationClass, final AnnotatedElement element) throws ContextException //
    {
        final AnnotationAttributes[] array = getAnnotationAttributesArray(element, annotationClass);
        return ObjectUtils.isEmpty(array) ? null : array[0];
    }

    /**
     * Get attributes the 'key-value' of annotations
     * 
     * @param element
     *            The annotated element
     * @param targetClass
     *            The annotation class
     * @return a set of {@link AnnotationAttributes} never be null
     * @since 2.1.1
     */
    public static <T extends Annotation> AnnotationAttributes[] //
            getAnnotationAttributesArray(final AnnotatedElement element, final Class<T> targetClass) throws ContextException//
    {
        Objects.requireNonNull(element, "annotated element can't be null");

        return ANNOTATION_ATTRIBUTES.computeIfAbsent(new AnnotationKey<>(element, targetClass), (k) -> {

            final LinkedHashSet<AnnotationAttributes> result = new LinkedHashSet<>();
            for (final Annotation annotation : element.getDeclaredAnnotations()) {
                final AnnotationAttributes attr = getAnnotationAttributes(annotation, targetClass);
                if (attr != null) {
                    result.add(attr);
                }
            }

            return result.isEmpty() ? EMPTY_ANNOTATION_ATTRIBUTES : result.toArray(EMPTY_ANNOTATION_ATTRIBUTES);
        });
    }

    @SuppressWarnings("serial")
    private static class AnnotationKey<T> implements Serializable {

        private final int hash;
        private final Class<T> annotationClass;
        private final AnnotatedElement annotatedElement;

        public AnnotationKey(AnnotatedElement annotatedElement, Class<T> annotationClass) {
            this.annotationClass = annotationClass;
            this.annotatedElement = annotatedElement;
            this.hash = Objects.hash(annotatedElement, annotationClass);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof AnnotationKey) {
                AnnotationKey<?> other = (AnnotationKey<?>) obj;

                return annotatedElement.equals(other.annotatedElement) //
                        && annotationClass.equals(other.annotationClass);
            }
            return false;
        }
    }

    /**
     * Get target {@link AnnotationAttributes}
     * 
     * @param annotationClass
     *            The annotation class
     * @param annotation
     *            The annotation instance
     * @return {@link AnnotationAttributes}
     * @since 2.1.1
     */
    public static <T extends Annotation> AnnotationAttributes //
            getAnnotationAttributes(final Annotation annotation, final Class<T> annotationClass) throws ContextException//
    {
        try {

            if (annotation == null) {
                return null;
            }
            final Class<? extends Annotation> annotationType = annotation.annotationType();

            if (annotationType == annotationClass) {// 如果等于对象注解就直接添加
                return getAnnotationAttributes(annotation);
            }

            // filter some annotation classes
            // -----------------------------------------
            if (IGNORE_ANNOTATION_CLASS.contains(annotationType)) {
                return null;
            }

            // find the default value of annotation
            // -----------------------------------------

            final AnnotationAttributes annotationAttributes = // recursive
                    getTargetAnnotationAttributes(annotationClass, annotationType);

            if (annotationAttributes == null) { // there is no an annotation
                return null;
            }

            // found it
            String name;
            Object value;

            for (final Method method : annotationType.getDeclaredMethods()) {
                name = method.getName();
                value = annotationAttributes.get(name);
                if (value == null || eq(method.getReturnType(), value.getClass())) {
                    annotationAttributes.put(name, method.invoke(annotation));
                }
            }
            return annotationAttributes;
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            log.error("An Exception Occurred When Getting Annotation Attributes, With Msg: [{}]", ex.getMessage(), ex);
            throw ExceptionUtils.newContextException(ex);
        }
    }

    private final static boolean eq(Class<?> returnType, Class<?> clazz) {
        if (returnType == clazz) {
            return true;
        }
        if (returnType.isPrimitive()) {
            switch (returnType.getName())
            {//@off
                case "int" :    return clazz == Integer.class;
                case "long" :   return clazz == Long.class;
                case "byte" :   return clazz == Byte.class;
                case "char" :   return clazz == Character.class;
                case "float" :  return clazz == Float.class;
                case "double" : return clazz == Double.class;
                case "short" :  return clazz == Short.class;
                case "boolean" :return clazz == Boolean.class;
                default:        return false;
            } //@on
        }
        return false;
    }

    /**
     * Use recursive to find the target annotation instance
     * 
     * @param targetAnnotationType
     *            target {@link Annotation} class
     * @param annotationType
     * @return target {@link AnnotationAttributes}
     * @since 2.1.1
     */
    public static <T extends Annotation> AnnotationAttributes //
            getTargetAnnotationAttributes(final Class<T> targetAnnotationType, final Class<? extends Annotation> annotationType)//
    {

        for (final Annotation currentAnnotation : annotationType.getAnnotations()) {

            if (IGNORE_ANNOTATION_CLASS.contains(currentAnnotation.annotationType())) {
                continue;
            }
            if (currentAnnotation.annotationType() == targetAnnotationType) {
                return getAnnotationAttributes(currentAnnotation); // found it
            }

            final AnnotationAttributes attributes = // recursive
                    getTargetAnnotationAttributes(targetAnnotationType, currentAnnotation.annotationType());

            if (attributes != null) {
                return attributes;
            }
        }
        return null;
    }

    /**
     * Whether a {@link Annotation} present on {@link AnnotatedElement}
     * 
     * @param <A>
     *            {@link Annotation} type
     * @param element
     *            Target {@link AnnotatedElement}
     * @param annType
     *            Target annotation type
     * @return Whether it's present
     */
    public static <A extends Annotation> boolean isAnnotationPresent(final AnnotatedElement element, final Class<A> annType) {
        return element.isAnnotationPresent(annType) || getAnnotation(annType, element) != null;
    }

    // ----------------------------- new instance

    /**
     * Get instance with bean class use default {@link Constructor}
     * 
     * @param beanClass
     *            bean class
     * @return the instance of target class
     * @since 2.1.2
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> beanClass) throws ContextException {
        try {
            return (T) newInstance(beanClass, ContextUtils.getApplicationContext());
        }
        catch (ReflectiveOperationException e) {
            throw ExceptionUtils.newContextException(e);
        }
    }

    /**
     * Get instance with bean class
     * 
     * @param beanClassName
     *            bean class name string
     * @return the instance of target class
     * @since 2.1.2
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(String beanClassName) throws ContextException {
        try {
            return (T) newInstance(classLoader.loadClass(beanClassName));
        }
        catch (Throwable e) {
            throw ExceptionUtils.newContextException(e);
        }
    }

    /**
     * 
     * Use default {@link Constructor} or Annotated {@link Autowired}
     * {@link Constructor} to create bean instance.
     * 
     * @param beanDefinition
     *            target bean's definition
     * @param beanFactory
     *            bean factory
     * @return {@link BeanDefinition} 's instance
     * @throws ReflectiveOperationException
     *             if any reflective operation exception occurred
     * @since 2.1.5
     */
    public static Object newInstance(final BeanDefinition beanDefinition, final BeanFactory beanFactory) //
            throws ReflectiveOperationException //
    {
        return newInstance(beanDefinition.getBeanClass(), beanFactory);
    }

    /**
     * Use default {@link Constructor} or Annotated {@link Autowired}
     * {@link Constructor} to create bean instance.
     * 
     * @param beanClass
     *            target bean class
     * @param beanFactory
     *            bean factory
     * @return bean class 's instance
     * @throws ReflectiveOperationException
     *             if any reflective operation exception occurred
     */
    public static Object newInstance(final Class<?> beanClass, final BeanFactory beanFactory) //
            throws ReflectiveOperationException //
    {
        try {
            return accessibleConstructor(beanClass).newInstance();
        }
        catch (final NoSuchMethodException e) {

            final Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
            if (constructors.length == 1) {
                final Constructor<?> constructor = constructors[0];
                return constructor.newInstance(//
                        ContextUtils.resolveParameter(makeAccessible(constructor), beanFactory)//
                );
            }
            for (final Constructor<?> constructor : constructors) {

                if (constructor.isAnnotationPresent(Autowired.class)) {
                    return constructor.newInstance(//
                            ContextUtils.resolveParameter(makeAccessible(constructor), beanFactory)//
                    );
                }
            }
            throw e;
        }
    }

    // --------------------------------- Field

    /**
     * Get bean instance's {@link Field}
     * 
     * @param target
     *            target instance
     * @return all {@link Field}
     * @since 2.1.5
     */
    public static Collection<Field> getFields(Object target) {
        return getFields(target.getClass());
    }

    /**
     * Get all {@link Field} list
     * 
     * @param targetClass
     *            target class
     * @return get all the {@link Field}
     * @since 2.1.2
     */
    public static Collection<Field> getFields(Class<?> targetClass) {

        final List<Field> list = new ArrayList<>(64);
        do {
            for (final Field field : targetClass.getDeclaredFields()) {
                list.add(field);
            }
        } while ((targetClass = targetClass.getSuperclass()) != Object.class && targetClass != null);

        return list;
    }

    /**
     * Get all {@link Field} array
     * 
     * @param targetClass
     *            target class
     * @return get all the {@link Field} array
     * @since 2.1.2
     */
    public static Field[] getFieldArray(Class<?> targetClass) {
        return getFields(targetClass).toArray(new Field[0]);
    }

    // --------------------------- parameter names discovering

    /**
     * Find method parameter list. Uses ObjectWeb's ASM library for analyzing class
     * files.
     * 
     * @param method
     *            target method
     * @return method parameter list
     * @throws ContextException
     *             when could not access to the class file
     * @since 2.1.6
     */
    public static String[] getMethodArgsNames(Method method) throws ContextException {
        return PARAMETER_NAMES_CACHE.computeIfAbsent(method.getDeclaringClass(), PARAMETER_NAMES_FUNCTION).get(method);
    }

    final static class ParameterFunction implements Function<Class<?>, Map<Method, String[]>> {

        @Override
        public Map<Method, String[]> apply(final Class<?> declaringClass) {

            final Map<Method, String[]> map = new ConcurrentHashMap<>(32);

            try (InputStream resourceAsStream = getClassLoader()//
                    .getResourceAsStream(declaringClass.getName()//
                            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)//
                            .concat(Constant.CLASS_FILE_SUFFIX))) //
            {

                final ClassNode classVisitor = new ClassNode();
                new ClassReader(resourceAsStream).accept(classVisitor, 0);

                for (final MethodNode methodNode : classVisitor.methodNodes) {

                    final Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
                    final Class<?>[] argTypes = new Class<?>[argumentTypes.length];
                    int i = 0;
                    for (final Type argumentType : argumentTypes) {
                        argTypes[i++] = forName(argumentType.getClassName());
                    }

                    final Method method = declaringClass.getDeclaredMethod(methodNode.name, argTypes);

                    final int parameterCount = method.getParameterCount();
                    if (parameterCount == 0) {
                        map.put(method, Constant.EMPTY_STRING_ARRAY);
                        continue;
                    }

                    if (Modifier.isAbstract(method.getModifiers()) || method.isBridge() || method.isSynthetic()) {
                        map.put(method, Stream.of(method.getParameters()).map(Parameter::getName).toArray(String[]::new));
                        continue;
                    }

                    final String[] paramNames = new String[parameterCount];
                    final List<String> localVariables = methodNode.localVariables;
                    if (localVariables.size() >= parameterCount) {
                        final int offset = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
                        for (i = 0; i < parameterCount; i++) {
                            paramNames[i] = localVariables.get(i + offset);
                        }
                    }
                    map.put(method, paramNames);
                }
            }
            catch (IOException | ClassNotFoundException | NoSuchMethodException | IndexOutOfBoundsException e) {
                throw new ContextException("When visit declaring class: [" + declaringClass.getName() + ']', e);
            }
            return map;
        }

    }

    final static class ClassNode extends ClassVisitor {

        private final List<MethodNode> methodNodes = new ArrayList<>();

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (isSyntheticOrBridged(access) //
                    || Constant.CONSTRUCTOR_NAME.equals(name) //
                    || Constant.STATIC_CLASS_INIT.equals(name)) {
                return null;
            }
            final MethodNode methodNode = new MethodNode(name, descriptor);
            methodNodes.add(methodNode);
            return methodNode;
        }

        private final static boolean isSyntheticOrBridged(int access) {
            return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
        }
    }

    final static class MethodNode extends MethodVisitor {
        private final String name;
        private final String desc;
        private final List<String> localVariables = new ArrayList<>();

        MethodNode(final String name, final String desc) {
            this.name = name;
            this.desc = desc;
        }
        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            localVariables.add(name);
        }
    }

    // --------------------------------access

    /**
     * Make the given field accessible, explicitly setting it accessible if
     * necessary.
     */
    public static Field makeAccessible(final Field field) {

        if ((!Modifier.isPublic(field.getModifiers()) //
                || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) && !field.isAccessible()) {

            field.setAccessible(true);
        }
        return field;
    }

    public static Object invokeMethod(final Method method, final Object target, final Object... args) {
        try {
            return method.invoke(target, args);
        }
        catch (IllegalAccessException e) {
            return invokeMethod(makeAccessible(method), target, args);
        }
        catch (Exception ex) {
            throw ExceptionUtils.newContextException(ExceptionUtils.unwrapThrowable(ex));
        }
    }

    public static Method makeAccessible(final Method method) {

        if ((!Modifier.isPublic(method.getModifiers()) //
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }

        return method;
    }

    public static <T> Constructor<T> accessibleConstructor(final Class<T> clazz, final Class<?>... parameterTypes)
            throws NoSuchMethodException //
    {
        return makeAccessible(clazz.getDeclaredConstructor(parameterTypes));
    }

    public static <T> Constructor<T> makeAccessible(Constructor<T> constructor) {

        if ((!Modifier.isPublic(constructor.getModifiers()) //
                || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {

            constructor.setAccessible(true);
        }
        return constructor;
    }

}
