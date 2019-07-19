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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.el.ELException;
import javax.el.ELProcessor;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConcurrentProperties;
import cn.taketoday.context.Condition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConditionalImpl;
import cn.taketoday.context.annotation.DefaultProps;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.PropertyValueResolver;
import cn.taketoday.context.loader.PropsPropertyResolver;
import cn.taketoday.context.loader.ValuePropertyResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides el, {@link Properties} loading, {@link Parameter}
 * resolving
 * 
 * @author TODAY <br>
 *         2019-01-16 20:04
 */
@Slf4j
public abstract class ContextUtils {

    private static PropertyValueResolver[] propertyValueResolvers;

    // @since 2.1.6 // shared elProcessor
    private static ELProcessor elProcessor;

    // @since 2.1.6 shared applicationContext
    public static ApplicationContext applicationContext;

    static {

        addPropertyValueResolver(//
                new ValuePropertyResolver(), //
                new PropsPropertyResolver(), //
                new AutowiredPropertyResolver()//
        );
    }

    /**
     * Get shared {@link ELProcessor}
     * 
     * @return Shared {@link ELProcessor}
     */
    public static ELProcessor getELProcessor() {
        return elProcessor;
    }

    /**
     * {@link ELProcessor}
     * 
     * @param elProcessor
     *            A new elProcessor
     */
    public static void setELProcessor(final ELProcessor elProcessor) {
        ContextUtils.elProcessor = elProcessor;
    }

    /**
     * @since 2.1.6
     */
    public static PropertyValueResolver[] getPropertyValueResolvers() {
        return propertyValueResolvers;
    }

    /**
     * @since 2.1.6
     */
    public static void setPropertyValueResolvers(PropertyValueResolver[] propertyValueResolvers) {
        ContextUtils.propertyValueResolvers = propertyValueResolvers;
    }

    /**
     * Get {@link ApplicationContext}
     * 
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Add {@link PropertyValueResolver} to {@link #propertyValueResolvers}
     * 
     * @param resolvers
     *            {@link TypeConverter} object
     * @since 2.1.6
     */
    public static void addPropertyValueResolver(PropertyValueResolver... resolvers) {

        final List<PropertyValueResolver> propertyValueResolvers = new ArrayList<>();

        if (getPropertyValueResolvers() != null) {
            Collections.addAll(propertyValueResolvers, getPropertyValueResolvers());
        }

        Collections.addAll(propertyValueResolvers, resolvers);

        OrderUtils.reversedSort(propertyValueResolvers);

        setPropertyValueResolvers(propertyValueResolvers.toArray(new PropertyValueResolver[0]));
    }

    /**
     * Find bean names
     * 
     * @param defaultName
     *            Default bean name
     * @param names
     *            Annotation values
     * @return Bean names
     */
    public static String[] findNames(String defaultName, String... names) {
        if (StringUtils.isArrayEmpty(names)) {
            return new String[] { defaultName }; // default name
        }
        return names;
    }

    /**
     * Resolve {@link Env} {@link Annotation}
     * 
     * @param value
     *            {@link Env} {@link Annotation}
     * @param expectedType
     *            expected value type
     * @return A resolved value object
     * @since 2.1.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolveValue(final Env value, final Class<T> expectedType) throws ConfigurationException {

        final Object resolveValue = resolveValue(new StringBuilder()//
                .append(Constant.PLACE_HOLDER_PREFIX)//
                .append(value.value())//
                .append(Constant.PLACE_HOLDER_SUFFIX).toString(), expectedType//
        );

        if (resolveValue != null) {
            return (T) resolveValue;
        }
        if (value.required()) {
            throw new ConfigurationException("Can't resolve property: [" + value.value() + "]");
        }

        final String defaultValue = value.defaultValue();
        if (StringUtils.isEmpty(defaultValue)) {
            return null;
        }
        return resolveValue(defaultValue, expectedType, System.getProperties());
    }

    /**
     * Resolve {@link Value} {@link Annotation}
     * 
     * @param value
     *            {@link Value} {@link Annotation}
     * @param expectedType
     *            expected value type
     * @return A resolved value object
     * @since 2.1.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolveValue(final Value value, final Class<T> expectedType) throws ConfigurationException {

        final Object resolveValue = resolveValue(value.value(), expectedType);
        if (resolveValue != null) {
            return (T) resolveValue;
        }
        if (value.required()) {
            throw new ConfigurationException("Can't resolve expression: [" + value.value() + "]");
        }
        final String defaultValue = value.defaultValue();
        if (StringUtils.isEmpty(defaultValue)) {
            return null;
        }
        return resolveValue(defaultValue, expectedType, System.getProperties());
    }

    /**
     * Replace a placeholder use default {@link System} properties source or eval el
     * 
     * @param expression
     *            expression {@link String}
     * @param expectedType
     *            expected value type
     * @return A resolved value object
     * @since 2.1.6
     */
    public static <T> T resolveValue(final String expression, final Class<T> expectedType) throws ConfigurationException {
        return resolveValue(expression, expectedType, System.getProperties());
    }

    /**
     * replace a placeholder or eval el
     * 
     * @param expression
     *            expression {@link String}
     * @param expectedType
     *            expected value type
     * @return A resolved value object
     * @since 2.1.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolveValue(final String expression, //
            final Class<T> expectedType, final Properties variables) throws ConfigurationException //
    {
        if (expression.contains(Constant.PLACE_HOLDER_PREFIX)) {
            final String replaced = resolvePlaceholder(variables, expression, false);
            return (T) ConvertUtils.convert(replaced, expectedType);
        }

        if (expression.contains(Constant.EL_PREFIX)) {
            try {
                return getELProcessor().getValue(expression, expectedType);
            }
            catch (ELException e) {
                throw new ConfigurationException(e);
            }
        }
        return (T) ConvertUtils.convert(expression, expectedType);
    }

    /**
     * Resolve parameters list
     * 
     * @param executable
     *            Target executable instance {@link Method} or a {@link Constructor}
     * @param beanFactory
     *            Bean factory
     * @since 2.1.2
     * @return Parameter list
     */
    public static Object[] resolveParameter(Executable executable, BeanFactory beanFactory) {

        final int parameterLength = executable.getParameterCount();
        if (parameterLength == 0) {
            return null;
        }

        // parameter list
        final Object[] args = new Object[parameterLength];
        final Parameter[] parameters = executable.getParameters();

        for (int i = 0; i < parameterLength; i++) {

            final Parameter parameter = parameters[i];
            final Class<?> type = parameter.getType();

            // if it is a Map
            if (Map.class.isAssignableFrom(type)) {
                Props props = parameter.getAnnotation(Props.class);
                if (props == null) {
                    props = new DefaultProps();
                }
                args[i] = loadProps(props, System.getProperties());
                continue;
            }
            // @since 2.1.6
            if (parameter.isAnnotationPresent(Value.class)) {
                args[i] = resolveValue(parameter.getAnnotation(Value.class), type);
                continue;
            }
            // @since 2.1.6
            if (parameter.isAnnotationPresent(Env.class)) {
                args[i] = resolveValue(parameter.getAnnotation(Env.class), type);
                continue;
            }

            boolean required = true;

            final Autowired autowiredOnParamter = parameter.getAnnotation(Autowired.class); // @Autowired on parameter

            Object bean; // bean instance
            if (autowiredOnParamter != null) {
                final String name = autowiredOnParamter.value();
                required = autowiredOnParamter.required();
                if (StringUtils.isNotEmpty(name)) {
                    // use name and bean type to get bean
                    bean = beanFactory.getBean(name, type);
                }
                else {
                    bean = beanFactory.getBean(type);
                }
            }
            else {
                bean = beanFactory.getBean(type);// use parameter type to obtain a bean
            }
            // @Props on a bean (pojo) which has already annotated @Autowired or not
            if (parameter.isAnnotationPresent(Props.class)) {
                final Props props = parameter.getAnnotation(Props.class);
                if (bean != null) {
                    // Environment.getProperties()
                    bean = resolveProps(props, bean, loadProps(props, System.getProperties()));
                }
                else {
                    bean = resolveProps(props, type, loadProps(props, System.getProperties()));
                }
            }
            if (bean == null && required) {
                // if it is required
                LoggerFactory.getLogger(ContextUtils.class)//
                        .error("[{}] is required and there isn't a [{}] bean", parameter, type);
                throw new NoSuchBeanDefinitionException(type);
            }
            args[i] = bean;
        }

        return args;
    }

    /**
     * Get a {@link InputStream} from given resource string
     * 
     * @param resource
     *            Target resource string
     * @return A {@link InputStream}
     * @throws IOException
     *             If any IO {@link Exception} occurred
     */
    public static final InputStream getResourceAsStream(String resource) throws IOException {

        InputStream in = ResourceUtils.getResource(resource).getInputStream();
        if (in == null) {
            throw new IOException("Could not find resource " + resource);
        }
        return in;
    }

    /**
     * 
     * @param resource
     * @return
     * @throws IOException
     */
    public static final Properties getResourceAsProperties(String resource) throws IOException {
        Properties props = new ConcurrentProperties();

        try (InputStream in = ResourceUtils.getResource(StringUtils.checkPropertiesName(resource)).getInputStream()) {
            props.load(in);
        }

        return props;
    }

    /**
     * Get {@link InputStream} from a url stirng
     * 
     * @param urlString
     *            Target url string
     * @return {@link InputStream}
     * @throws IOException
     *             If can't get the stream
     */
    public static final InputStream getUrlAsStream(String urlString) throws IOException {
        return new URL(urlString).openConnection().getInputStream();
    }

    /**
     * Load {@link Properties} from a url string
     * 
     * @param urlString
     *            Target url string
     * @return {@link Properties}
     * @throws IOException
     *             If any IO {@link Exception} occurred
     */
    public static final Properties getUrlAsProperties(String urlString) throws IOException {
        Properties props = new ConcurrentProperties();
        try (InputStream in = getUrlAsStream(StringUtils.checkPropertiesName(urlString))) {
            props.load(in);
        }
        return props;
    }

    /**
     * Resolve placeholder s
     * 
     * @param properties
     *            {@link Properties}
     * @param value
     *            the value will as a key, if don't exist return itself
     * @return A resolved string
     * @throws ConfigurationException
     *             If not exist target property
     */
    public static String resolvePlaceholder(Map<Object, Object> properties, String value) throws ConfigurationException {
        return resolvePlaceholder(properties, value, true);
    }

    /**
     * Resolve placeholder s
     * 
     * @param properties
     *            {@link Properties} variables source
     * @param input
     *            Input expression
     * @param throw_
     *            If there doesn't exist the key throw {@link Exception}
     * @return A resolved string
     * @throws ConfigurationException
     *             If not exist target property
     */
    public static String resolvePlaceholder(Map<Object, Object> properties, String input, boolean throw_) //
            throws ConfigurationException //
    {
        if (input == null || input.length() <= 3) { // #{} > 3
            return input;
        }
        int prefixIndex = 0;
        int suffixIndex = 0;

        final StringBuilder builder = new StringBuilder();
        while ((prefixIndex = input.indexOf(Constant.PLACE_HOLDER_PREFIX)) > -1 //
                && (suffixIndex = input.indexOf(Constant.PLACE_HOLDER_SUFFIX)) > -1) {

            builder.append(input.substring(0, prefixIndex));

            final String key = input.substring(prefixIndex + 2, suffixIndex);

            final Object property = properties.get(key);
            if (property == null) {
                if (throw_) {
                    throw new ConfigurationException("Properties -> [" + key + "] , must specify a value.");
                }
                LoggerFactory.getLogger(ContextUtils.class).debug("There is no property for key: [{}]", key);
                return null;
            }
            // find
            builder.append(resolvePlaceholder(properties, (property instanceof String) ? (String) property : null, throw_));
            input = input.substring(suffixIndex + 1);
        }
        if (builder.length() == 0) {
            return input;
        }
        return builder.append(input).toString();
    }

    // ----------------- loader

    /**
     * Set init methods to {@link BeanDefinition}
     * 
     * @param beanDefinition
     *            Target {@link BeanDefinition}
     * @param initMethods
     *            Resolved init methods
     * @since 2.1.3
     */
    public static void resolveInitMethod(BeanDefinition beanDefinition, String... initMethods) {
        beanDefinition.setInitMethods(resolveInitMethod(beanDefinition.getBeanClass(), initMethods));
    }

    /**
     * @param beanClass
     *            Bean class
     * @param initMethods
     *            Init Method s
     * @since 2.1.2
     */
    public static Method[] resolveInitMethod(Class<?> beanClass, String... initMethods) {

        if (initMethods == null) {
            initMethods = Constant.EMPTY_STRING_ARRAY;
        }
        final List<Method> methods = new ArrayList<>(4);

        addInitMethod(methods, beanClass, initMethods);
        // superclass
        final Class<?> superClass = beanClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            addInitMethod(methods, superClass, initMethods);
        }
        OrderUtils.reversedSort(methods);
        return methods.toArray(new Method[0]);
    }

    /**
     * Add a method which annotated with {@link PostConstruct}
     * 
     * @param methods
     *            Method list
     * @param beanClass
     *            Bean class
     * @param initMethods
     *            Init Method name
     * @since 2.1.2
     */
    private static void addInitMethod(List<Method> methods, Class<?> beanClass, String... initMethods) {
        for (final Method method : beanClass.getDeclaredMethods()) {

            if (method.isAnnotationPresent(PostConstruct.class)) {
                methods.add(method);
                continue;
            }
            for (final String initMethod : initMethods) {
                if (method.getParameterCount() == 0 && initMethod.equals(method.getName())) {
                    methods.add(method);
                }
            }
        }
    }

    /**
     * Set {@link PropertyValue} to the target {@link BeanDefinition}
     * 
     * @param beanDefinition
     *            target bean definition
     * @param applicationContext
     *            {@link ApplicationContext}
     * @since 2.1.3
     */
    public static void resolvePropertyValue(final BeanDefinition beanDefinition) {
        beanDefinition.setPropertyValues(resolvePropertyValue(beanDefinition.getBeanClass()));
    }

    /**
     * Process bean's property (field)
     * 
     * @param beanClass
     *            Bean class
     * @param applicationContext
     *            {@link ApplicationContext}
     * @since 2.1.2
     */
    public static PropertyValue[] resolvePropertyValue(Class<?> beanClass) {
        final Set<PropertyValue> propertyValues = new HashSet<>(32);
        for (final Field field : ClassUtils.getFields(beanClass)) {
            final PropertyValue created = createPropertyValue(field);
            // not required
            if (created != null) {
                ClassUtils.makeAccessible(field);
                propertyValues.add(created);
            }
        }
        return propertyValues.toArray(new PropertyValue[0]);
    }

    /**
     * Create property value
     * 
     * @param field
     *            Property
     * @param applicationContext
     *            {@link ApplicationContext}
     * @return A new {@link PropertyValue}
     */
    public static final PropertyValue createPropertyValue(Field field) {

        for (final PropertyValueResolver propertyValueResolver : getPropertyValueResolvers()) {
            if (propertyValueResolver.supports(field)) {
                return propertyValueResolver.resolveProperty(field);
            }
        }
        return null;
    }

    /**
     * Properties injection
     *
     * @param beanDefinition
     *            Target bean definition
     * @param environment
     *            Application {@link Environment}
     */
    public static void resolveProps(BeanDefinition beanDefinition, Environment environment) throws ConfigurationException {
        Class<?> beanClass = beanDefinition.getBeanClass();
        if (beanClass.isAnnotationPresent(Props.class)) {
            beanDefinition.addPropertyValue(resolveProps(beanClass, environment.getProperties()));
        }
    }

    /**
     * Resolve {@link PropertyValue}s from target {@link Method} or {@link Class}
     * 
     * @param annotatedElement
     *            Target {@link AnnotatedElement}
     * @param properties
     *            {@link Properties} variables source
     * @throws ConfigurationException
     *             If not support {@link AnnotatedElement}
     */
    public static List<PropertyValue> resolveProps(AnnotatedElement annotatedElement, Properties properties)
            throws ConfigurationException //
    {
        final Props props = annotatedElement.getAnnotation(Props.class);

        if (props == null) {
            return Collections.emptyList();
        }

        final Class<?> type;
        if (annotatedElement instanceof Class) {
            type = (Class<?>) annotatedElement;
        }
        else if (annotatedElement instanceof Method) {
            type = ((Method) annotatedElement).getReturnType();
        }
        else {
            throw new ConfigurationException("Not support annotated element: [" + annotatedElement + "]");
        }

        log.debug("Loading Properties For: [{}]", type.getName());

        final List<PropertyValue> propertyValues = new ArrayList<>();
        final String[] prefixs = props.prefix();
        final List<Class<?>> nested = Arrays.asList(props.nested());

        for (final Field declaredField : ClassUtils.getFields(type)) {
            final Object converted = resolveProps(declaredField, nested, prefixs, properties);
            if (converted != null) {
                ClassUtils.makeAccessible(declaredField);
                propertyValues.add(new PropertyValue(converted, declaredField));
            }
        }
        return propertyValues;
    }

    /**
     * Resolve target {@link Field} object
     * 
     * @param declaredField
     * @param nested
     *            Field class's field class
     * @param prefixs
     *            {@link Properties}'s prefix
     * @param properties
     *            {@link Properties} variables source
     * @return Resolved field object
     */
    public static Object resolveProps(final Field declaredField, //
            final List<Class<?>> nested, final String[] prefixs, Properties properties) //
    {
        final Class<?> fieldType = declaredField.getType();

        for (final String prefix : prefixs) {// maybe a default value: ""

            final String key = prefix + declaredField.getName();

            Object value = properties.get(key);
            if (value == null) { // just null not include empty
                // inject nested Props
                final DefaultProps nestedProps;
                if (declaredField.isAnnotationPresent(Props.class)) {
                    nestedProps = new DefaultProps(declaredField.getAnnotation(Props.class));
                }
                else {
                    if (!nested.contains(fieldType)) {
                        continue;
                    }
                    nestedProps = new DefaultProps();
                }
                final boolean replace = nestedProps.replace();
                String[] prefixsToUse = nestedProps.prefix();
                for (int i = 0; i < prefixsToUse.length; i++) {
                    String str = prefixsToUse[i];
                    if (StringUtils.isEmpty(str)) {
                        prefixsToUse[i] = key.concat(".");
                    }
                    else if (!replace) { // don't replace the parent prefix
                        prefixsToUse[i] = prefix.concat(str);
                    }
                }
                value = resolveProps(nestedProps.setPrefix(prefixsToUse), fieldType, properties);
            }

            log.debug("Found Property: [{}] = [{}]", key, value);

            if (value instanceof String) {
                return resolveValue((String) value, fieldType, properties);
            }
            if (value != null) {
                return ConvertUtils.convert(value, fieldType);
            }
        }
        return null;
    }

    /**
     * Resolve target object with {@link Props} and target object's class
     * 
     * @param prefixs
     *            {@link Props#prefix()}
     * @param beanClass
     *            Target class, must have default {@link Constructor}
     * @param properties
     *            {@link Properties} variables source
     * @since 2.1.5
     */
    public static <T> T resolveProps(final Props props, Class<T> beanClass, Properties properties) {
        return resolveProps(props, ClassUtils.newInstance(beanClass), properties);
    }

    /**
     * Resolve target object with {@link Props} and target object's instance
     * 
     * @param prefixs
     *            {@link Props#prefix()}
     * @param bean
     *            Bean instance
     * @param properties
     *            {@link Properties} variables source
     * @since 2.1.5
     */
    public static <T> T resolveProps(final Props props, T bean, Properties properties) {

        final String[] prefixs = props.prefix();
        final List<Class<?>> nested = Arrays.asList(props.nested());
        try {

            for (final Field declaredField : ClassUtils.getFields(bean)) {
                final Object converted = resolveProps(declaredField, nested, prefixs, properties);
                if (converted != null) {
                    declaredField.setAccessible(true);
                    declaredField.set(bean, converted);
                }
            }
            return bean;
        }
        catch (IllegalAccessException e) {
            throw new ContextException(e);
        }
    }

    /**
     * Load {@link Properties} from {@link Props} {@link Annotation}
     * 
     * @param props
     *            {@link Props}
     * @param aplicationProps
     *            Application's {@link Properties}
     * @since 2.1.5
     */
    public static Properties loadProps(Props props, Properties aplicationProps) {

        final Properties ret = new ConcurrentProperties();
        final String[] fileNames = props.value();

        final Properties propertiesToUse;
        if (fileNames.length == 0) {
            propertiesToUse = Objects.requireNonNull(aplicationProps);
        }
        else {
            propertiesToUse = new ConcurrentProperties();
            for (String fileName : fileNames) {
                if (StringUtils.isEmpty(fileName)) {
                    propertiesToUse.putAll(aplicationProps);
                    break;
                }
                try (InputStream inputStream = getResourceAsStream(StringUtils.checkPropertiesName(fileName))) {
                    propertiesToUse.load(inputStream);
                }
                catch (IOException e) {
                    throw new ContextException(e);
                }
            }
        }
        final String[] prefixs = props.prefix();
        final boolean replace = props.replace();

        for (Entry<Object, Object> entry : propertiesToUse.entrySet()) {
            Object key_ = entry.getKey();
            if (!(key_ instanceof String)) {
                continue;
            }

            String key = (String) key_;
            for (String prefix : prefixs) {
                if (Constant.BLANK.equals(prefix) || key.startsWith(prefix)) { // start with prefix
                    if (replace) {
                        // replace the prefix
                        key = key.replaceFirst(prefix, Constant.BLANK);
                    }
                    // resolvePlaceholder(propertiesToUse, (String) entry.getValue())
                    ret.put(key, resolveValue((String) entry.getValue(), Object.class, propertiesToUse));
                }
            }
        }
        return ret;
    }

    /**
     * Decide whether to load the bean
     * 
     * @param annotatedElement
     *            Target class or a method
     * @param applicationContext
     *            {@link ApplicationContext}
     * @return If matched
     */
    public static boolean conditional(AnnotatedElement annotatedElement, ConfigurableApplicationContext applicationContext) {

        final Collection<Conditional> annotations = //
                ClassUtils.getAnnotation(annotatedElement, Conditional.class, ConditionalImpl.class);

        OrderUtils.reversedSort((List<Conditional>) annotations);
        for (Conditional conditional : annotations) {

            for (final Class<? extends Condition> conditionClass : conditional.value()) {
                final Condition condition = ClassUtils.newInstance(conditionClass);
                if (!condition.matches(applicationContext, annotatedElement)) {
                    return false; // can't match
                }
            }
        }

        return true;
    }

    /**
     * Validate bean definition
     * 
     * @param beanDefinition
     *            Target {@link BeanDefinition}
     * @param applicationContext
     *            Application context
     */
    public static void validateBeanDefinition(BeanDefinition beanDefinition) {

        if (beanDefinition instanceof StandardBeanDefinition) {
            final StandardBeanDefinition standardBeanDefinition = ((StandardBeanDefinition) beanDefinition);

            if (StringUtils.isEmpty(standardBeanDefinition.getDeclaringName())) {
                throw new ConfigurationException("Declaring name can't be null");
            }
            if (standardBeanDefinition.getFactoryMethod() == null) {
                throw new ConfigurationException("Factory Method can't be null");
            }
        }

        if (beanDefinition.getBeanClass() == null) {
            throw new ConfigurationException("Definition's bean class can't be null");
        }
        if (beanDefinition.getDestroyMethods() == null) {
            beanDefinition.setDestroyMethods(new String[0]);
        }
        if (beanDefinition.getInitMethods() == null) {
            beanDefinition.setInitMethods(resolveInitMethod(beanDefinition.getBeanClass()));
        }
        if (beanDefinition.getPropertyValues() == null) {
            beanDefinition.setPropertyValues(resolvePropertyValue(beanDefinition.getBeanClass()));
        }
        if (beanDefinition.getScope() == null) {
            beanDefinition.setScope(Scope.SINGLETON);
        }
    }

    /**
     * Destroy bean instance
     * 
     * @param bean
     *            Bean instance
     * @param methods
     *            Bean class's methods
     * @throws Throwable
     *             When destroy a bean
     */
    public static void destroyBean(Object bean, Method[] methods) throws Throwable {

        // PreDestroy
        for (final Method method : methods) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                // fix: can not access a member @since 2.1.6
                ClassUtils.makeAccessible(method).invoke(bean);
            }
        }

        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }
    }

    /**
     * Is a context missed bean?
     * 
     * @param missingBean
     *            The {@link Annotation} declared on the class or a method
     * @param beanClass
     *            Missed bean class
     * @param beanFactory
     *            The {@link AbstractBeanFactory}
     * @return If the bean is missed in context
     * @since 2.1.6
     */
    public static boolean isMissedBean(final MissingBean missingBean, final Class<?> beanClass, //
            final ConfigurableBeanFactory beanFactory) //
    {
        if (missingBean == null) {
            return false;
        }

        final String beanName = missingBean.value();
        if (StringUtils.isNotEmpty(beanName) && beanFactory.containsBeanDefinition(beanName)) {
            return false;
        }
        final Class<?> type = missingBean.type();

        return !((type != void.class && beanFactory.containsBeanDefinition(type, !type.isInterface())) //
                || beanFactory.containsBeanDefinition(beanClass));
    }

    /**
     * @since 2.1.6
     */
    public static boolean equals(Class<?> one, Class<?> two) {
        return one.getName().equals(two.getName());
    }

    // bean definition

    /**
     * Build for a bean class with given default bean name
     * 
     * @param beanClass
     *            Target bean class
     * @param defaultName
     *            Default bean name
     * @return List of {@link BeanDefinition}s
     */
    public static List<BeanDefinition> buildBeanDefinitions(Class<?> beanClass, String defaultName) {

        final Collection<AnnotationAttributes> componentAttributes = //
                ClassUtils.getAnnotationAttributes(beanClass, Component.class);

        final List<BeanDefinition> ret = new ArrayList<>(componentAttributes.size());

        if (componentAttributes.isEmpty()) {
            ret.add(buildBeanDefinition(beanClass, null, defaultName));
        }
        else {

            for (AnnotationAttributes attributes : componentAttributes) {
                for (final String name : ContextUtils.findNames(defaultName, attributes.getStringArray(Constant.VALUE))) {

                    ret.add(buildBeanDefinition(beanClass, attributes, name));
                }
            }
        }
        return ret;
    }

    public static BeanDefinition buildBeanDefinition(Class<?> beanClass, AnnotationAttributes attributes, String beanName) {
        final BeanDefinition beanDefinition = new DefaultBeanDefinition(beanName, beanClass);//

        if (attributes == null) {
            beanDefinition.setDestroyMethods(Constant.EMPTY_STRING_ARRAY)//
                    .setInitMethods(ContextUtils.resolveInitMethod(beanClass));//
        }
        else {
            beanDefinition.setScope(attributes.getEnum(Constant.SCOPE))//
                    .setDestroyMethods(attributes.getStringArray(Constant.DESTROY_METHODS))//
                    .setInitMethods(ContextUtils.resolveInitMethod(beanClass, attributes.getStringArray(Constant.INIT_METHODS)));
        }

        beanDefinition.setPropertyValues(ContextUtils.resolvePropertyValue(beanClass));
        // fix missing @Props injection
        ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());
        return beanDefinition;
    }

}
