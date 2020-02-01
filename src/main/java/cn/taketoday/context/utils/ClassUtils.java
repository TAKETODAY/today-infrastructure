/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.context.Constant.EMPTY_ANNOTATION_ATTRIBUTES;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
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
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

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

    private static final Map<String, Class<?>> PRIMITIVE_CACHE = new HashMap<>(32);

    /** @since 2.1.1 */
    private static final Set<Class<? extends Annotation>> IGNORE_ANNOTATION_CLASS = new HashSet<>();

    private static final Map<AnnotationKey<?>, Object> ANNOTATIONS = new WeakHashMap<>(128);
    private static final ParameterFunction PARAMETER_NAMES_FUNCTION = new ParameterFunction();
    private static final Map<Class<?>, Map<Method, String[]>> PARAMETER_NAMES_CACHE = new HashMap<>(256);
    private static final Map<AnnotationKey<?>, AnnotationAttributes[]> ANNOTATION_ATTRIBUTES = new WeakHashMap<>(128);

    static {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassUtils.class.getClassLoader();
        }
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        setClassLoader(classLoader);

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
        addIgnoreAnnotationClass(Repeatable.class);
        addIgnoreAnnotationClass(Documented.class);
    }

    public static void addIgnoreAnnotationClass(Class<? extends Annotation> annotationClass) {
        IGNORE_ANNOTATION_CLASS.add(annotationClass);
    }

    /**
     * clear cache
     */
    public static void clearCache() {
        ANNOTATIONS.clear();
        ANNOTATION_ATTRIBUTES.clear();
    }

    public static void setClassLoader(ClassLoader classLoader) {
        ClassUtils.classLoader = classLoader;
    }

    public static ClassLoader getClassLoader() {
        return ClassUtils.classLoader;
    }

    /**
     * get all classes loaded in class path
     * @deprecated Deprecated in 2.1.7 High scan performance without caching
     */
    @Deprecated
    public static Set<Class<?>> getClassCache() {
        return scan(Constant.BLANK);
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
        
        requireNonNull(className, "class name can't be null");
        try {
            forName(className);
            return true;
        }
        catch (ClassNotFoundException ex) {
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
     *              a class full name
     * @param classLoader
     *              The Class file loader
     * @return a class
     * @throws ClassNotFoundException
     *             When class could not be found
     * @since 2.1.7
     */
    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException {
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

        if (classLoader == null) {
            classLoader = getClassLoader();
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
        return forName(name, classLoader);
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
     * Scan class with given package.
     * 
     * @param locations
     *            The packages to scan
     * @return Class set
     */
    public static Set<Class<?>> scan(final String... locations) {
        return new CandidateComponentScanner().scan(locations);
    }
    
    /**
     * Scan all the classpath classes
     * 
     * @param scanClasses
     *            class set
     * @since 2.1.6
     */
    public static void scan(Set<Class<?>> scanClasses) {
        new CandidateComponentScanner()
                .setCandidates(scanClasses)
                .scan();       
    }
    
    public static String getClassName(ClassReader r) {
        return r.getClassName().replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
    }
    
    public static String getClassName(final byte[] classFile) {
        return getClassName(new ClassReader(classFile));
    }
    
    public static String getClassName(final Resource resource) throws IOException {
        try (final InputStream inputStream = resource.getInputStream()) {
            return getClassName(inputStream);
        }
    }

    public static String getClassName(final InputStream inputStream) throws IOException {
        return getClassName(new ClassReader(inputStream));
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
    public static <T extends Annotation> T[] 
                getAnnotationArray(final AnnotatedElement element,
                                   final Class<T> annotationClass,
                                   final Class<? extends T> implClass) throws ContextException //
    {
        if (annotationClass == null) {
            return null;
        }

        requireNonNull(implClass, "Implementation class can't be null");

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

        if (targetClass == null) {
            return null;
        }

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
    public static <A> A injectAttributes(final AnnotationAttributes source,
                                         final Class<?> annotationClass, final A instance) throws ContextException//
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
            throw new ContextException("You Must Specify A Field: " + name + "] In Class: [" + implClass.getName() + "]", e);
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Inject Attributes Attributes, With Msg: ["+ ex +"]", ex);
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
            throw new ContextException("An Exception Occurred When Getting Annotation Attributes: [" + ex + "]", ex);
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
     * @param element
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @param implClass
     *            the annotation' subclass
     * @return the {@link Collection} of {@link Annotation} instance
     * @since 2.1.7
     */
    public static <T extends Annotation> T getAnnotation(final Class<T> annotationClass,
                                                         final Class<? extends T> implClass, final AnnotatedElement element)
    {
        final T[] array = getAnnotationArray(element, annotationClass, implClass);
        return ObjectUtils.isEmpty(array) ? null : array[0];
    }

    /**
     * Get First Annotation
     * 
     * @param annotated
     *            The annotated element object
     * @param annotationClass
     *            The annotation class
     * @return The target {@link Annotation} instance
     * @since 2.1.7
     */
    public static <T extends Annotation> T getAnnotation(final Object annotated, final Class<T> annotationClass) {
        return annotated == null ? null : getAnnotation(annotationClass, annotated.getClass());
    }
    /**
     * Get First Annotation
     * 
     * @param annotatedElement
     *            The annotated element
     * @param annotationClass
     *            The annotation class
     * @return The target {@link Annotation} instance
     * @since 2.1.7
     */
    public static <T extends Annotation> T getAnnotation(final Class<T> annotationClass,
                                                         final AnnotatedElement annotatedElement) {

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
		return annotationClass.cast(Proxy.newProxyInstance(classLoader, new Class[] { annotationClass, Annotation.class },
				(Object proxy, Method method, Object[] args) -> {
					// The switch statement compares the String object in its expression with the expressions
					// associated with each case label as if it were using the String.equals method;
					// consequently, the comparison of String objects in switch statements is case sensitive. 
					// The Java compiler generates generally more efficient bytecode from switch statements 
					// that use String objects than from chained if-then-else statements.
					switch (method.getName())
					{
						case Constant.EQUALS : 			return eq(proxy, attributes, args[0]);
						case Constant.HASH_CODE :		return attributes.hashCode();
						case Constant.TO_STRING :		return attributes.toString();
						case Constant.ANNOTATION_TYPE :	return annotationClass;
						default :                       return attributes.get(method.getName());
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
    private static Object eq(final Object proxy, final AnnotationAttributes attributes, final Object object)//
            throws IllegalAccessException, InvocationTargetException //
    {
        if (proxy == object) {
            return true;
        }

        final Class<?> targetClass = proxy.getClass();
        if (targetClass.isInstance(object)) {

            for (Entry<String, Object> entry : attributes.entrySet()) {
                final String key = entry.getKey(); // method name

                try {

                    final Method method = targetClass.getDeclaredMethod(key);

                    if (method.getReturnType() == void.class //
                            || !Objects.deepEquals(method.invoke(object), entry.getValue())) {

                        return false;
                    }
                }
                catch (NoSuchMethodException e) {
                    return false;
                }
            }
            return true;
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
     * @return First of the {@link AnnotationAttributes} on the element
     * @since 2.1.7
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
        if (targetClass == null) {
            return null;
        }
        requireNonNull(element, "annotated element can't be null");

        return ANNOTATION_ATTRIBUTES.computeIfAbsent(new AnnotationKey<>(element, targetClass), (k) -> {
            final Set<AnnotationAttributes> result = new LinkedHashSet<>();
            for (final Annotation annotation : element.getAnnotations()) {
                final Set<AnnotationAttributes> attr = getAnnotationAttributes(annotation, targetClass);
                if (!attr.isEmpty()) {
                    result.addAll(attr);
                }
            }
            return result.isEmpty() ? EMPTY_ANNOTATION_ATTRIBUTES : result.toArray(new AnnotationAttributes[result.size()]);
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
                final AnnotationKey<?> other = (AnnotationKey<?>) obj;

                return Objects.equals(annotatedElement, other.annotatedElement) //
                        && Objects.equals(annotationClass, other.annotationClass);
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
     * @return {@link AnnotationAttributes} set never be null
     * @since 2.1.7
     */
    public static <T extends Annotation> Set<AnnotationAttributes> //
            getAnnotationAttributes(final Annotation annotation, final Class<T> annotationClass) throws ContextException//
    {
        try {

            if (annotation == null) {
                return Collections.emptySet();
            }
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == annotationClass) {// 如果等于对象注解就直接添加
                return Collections.singleton(getAnnotationAttributes(annotation));
            }

            // filter some annotation classes
            // -----------------------------------------
            if (IGNORE_ANNOTATION_CLASS.contains(annotationType)) {
                return Collections.emptySet();
            }

            // find the default value of annotation
            // -----------------------------------------
            
            final Set<AnnotationAttributes> ret = new HashSet<>();
            
            findTargetAnnotationAttributes(ret, annotationClass, annotationType);

            if (ret.isEmpty()) { // there is no an annotation
                return Collections.emptySet();
            }

            // found it
            String name;
            Object value;
            for (final AnnotationAttributes attributes : ret) {
                for (final Method method : annotationType.getDeclaredMethods()) {
                    name = method.getName();
                    value = attributes.get(name);
                    if (value == null || eq(method.getReturnType(), value.getClass())) {
                        attributes.put(name, method.invoke(annotation));
                    }
                }
            }
            return ret;
        }
        catch (Throwable ex) {
            ex = ExceptionUtils.unwrapThrowable(ex);
            throw new ContextException("An Exception Occurred When Getting Annotation Attributes, With Msg: [" + ex + "]", ex);
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
     * Use recursive to find the All target annotation instance
     * 
     * @param targetAnnotationType
     *              target {@link Annotation} class
     * @param annotationType
     *              {@link Annotation} source
     * @return target {@link AnnotationAttributes}
     * @since 2.1.7
     */
    public static <T extends Annotation> void
                findTargetAnnotationAttributes(final Set<AnnotationAttributes> attributes,
                                               final Class<T> targetAnnotationType, 
                                               final Class<? extends Annotation> annotationType)//
    {
        final Set<Class<? extends Annotation>> ignoreAnnotation = IGNORE_ANNOTATION_CLASS;
        for (final Annotation currentAnnotation : annotationType.getAnnotations()) {
            if (ignoreAnnotation.contains(currentAnnotation.annotationType())) {
                continue;
            }
            if (currentAnnotation.annotationType() == targetAnnotationType) {
                attributes.add(getAnnotationAttributes(currentAnnotation)); // found it
            }
            else {
                findTargetAnnotationAttributes(attributes, targetAnnotationType, currentAnnotation.annotationType());
            }
        }
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
    public static <A extends Annotation> boolean isAnnotationPresent(final AnnotatedElement element,
                                                                     final Class<A> annType) {
        if (annType == null) {
            return false;
        }
        return element.isAnnotationPresent(annType)//
                || ObjectUtils.isNotEmpty(getAnnotationAttributesArray(element, annType));
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
    public static <T> T newInstance(Class<T> beanClass) throws ContextException {
        try {
            return newInstance(beanClass, ContextUtils.getApplicationContext());
        }
        catch (ReflectiveOperationException e) {
            throw new ContextException(e);
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
            throw new ContextException(e);
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
    public static <T> T newInstance(final Class<T> beanClass, final BeanFactory beanFactory) //
            throws ReflectiveOperationException //
    {
        final Constructor<T> constructor = obtainConstructor(beanClass);
        return constructor.newInstance(ContextUtils.resolveParameter(constructor, beanFactory));
    }

    /**
     * Obtain a suitable {@link Constructor}.
     * <p>
     * Look for the default constructor, if there is no default constructor, then
     * get all the constructors, if there is only one constructor then use this
     * constructor, if not more than one use the @Autowired constructor if there is
     * no suitable {@link Constructor} will throw an exception
     * <p>
     * 
     * @param <T>
     *            Target type
     * @param beanClass
     *            target bean class
     * @return Suitable constructor
     * @throws NoSuchMethodException
     *             If there is no suitable constructor
     * @since 2.1.7
     */
    public static <T> Constructor<T> obtainConstructor(Class<T> beanClass) throws NoSuchMethodException {
        try {
            return accessibleConstructor(beanClass);
        }
        catch (final NoSuchMethodException e) {
            @SuppressWarnings("unchecked") //
            final Constructor<T>[] constructors = (Constructor<T>[]) beanClass.getDeclaredConstructors();
            if (constructors.length == 1) {
                return makeAccessible(constructors[0]);
            }
            for (final Constructor<T> constructor : constructors) {
                if (constructor.isAnnotationPresent(Autowired.class)) {
                    return makeAccessible(constructor);
                }
            }
            throw e;
        }
    }

    /**
     * If the class is dynamically generated then the user class will be extracted
     * in a specific format.
     * 
     * @param synthetic
     *            Input object
     * @return The user class
     * @since 2.1.7
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getUserClass(T synthetic) {
        return (Class<T>) getUserClass(requireNonNull(synthetic).getClass());
    }

    /**
     * If the class is dynamically generated then the user class will be extracted
     * in a specific format.
     * 
     * @param syntheticClass
     *            input test class
     * @return The user class
     * @since 2.1.7
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getUserClass(Class<T> syntheticClass) {
        if (requireNonNull(syntheticClass).getName().lastIndexOf(Constant.CGLIB_CLASS_SEPARATOR) > -1) {
            Class<?> superclass = syntheticClass.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return (Class<T>) superclass;
            }
        }
        return syntheticClass;
    }

    /**
     * If the class is dynamically generated then the user class will be extracted
     * in a specific format.
     * 
     * @param name
     *            Class name
     * @return The user class
     * @since 2.1.7
     */
    public static <T> Class<T> getUserClass(String name) {
        final int i = requireNonNull(name).indexOf(Constant.CGLIB_CLASS_SEPARATOR);
        return i > 0 ? loadClass(name.substring(0, i)) : loadClass(name);
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
        final Collection<Field> fields = getFields(targetClass);
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Get genericity class
     * 
     * @param type
     *            source type
     * @since 2.1.7
     */
    public static java.lang.reflect.Type[] getGenericityClass(final Class<?> type) {
        if (type != null) {
            final java.lang.reflect.Type pType = type.getGenericSuperclass();
            if (pType instanceof ParameterizedType) {
                return ((ParameterizedType) pType).getActualTypeArguments();
            }
        }
        return null;
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

            try (InputStream resourceAsStream = getClassLoader()
                    .getResourceAsStream(declaringClass.getName()
                            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
                                .concat(Constant.CLASS_FILE_SUFFIX))) 
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
        public MethodVisitor visitMethod(int access, 
                                         String name,
                                         String descriptor,
                                         String signature, 
                                         String[] exceptions) {
            
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
        public void visitLocalVariable(String name, 
                                       String descriptor, 
                                       String signature, 
                                       Label start, Label end, int index) {
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
            throw new ContextException(ExceptionUtils.unwrapThrowable(ex));
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
    
    //
    // ---------------------------------
    
    
    /**
     * Return the qualified name of the given method, consisting of fully qualified
     * interface/class name + "." + method name.
     * 
     * @param method
     *            the method
     * @return the qualified name of the method
     */
    public static String getQualifiedMethodName(Method method) {
        return getQualifiedMethodName(method, null);
    }

    /**
     * Return the qualified name of the given method, consisting of fully qualified
     * interface/class name + "." + method name.
     * 
     * @param method
     *            the method
     * @param clazz
     *            the clazz that the method is being invoked on (may be {@code null}
     *            to indicate the method's declaring class)
     * @return the qualified name of the method
     */
    public static String getQualifiedMethodName(Method method, Class<?> clazz) {
        requireNonNull(method, "Method must not be null");
        return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
    }

}
