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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConcurrentProperties;
import cn.taketoday.context.Condition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConditionalImpl;
import cn.taketoday.context.annotation.DefaultProps;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
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
 * @author Today <br>
 * 
 *         2019-01-16 20:04
 */
@Slf4j
public abstract class ContextUtils {

	/**
	 * Find names
	 * 
	 * @param defaultName
	 *            default bean name
	 * @param names
	 *            annotation values
	 * @return
	 */
	public static String[] findNames(String defaultName, String... names) {
		if (StringUtils.isArrayEmpty(names)) {
			return new String[] { defaultName }; // default name
		}
		return names;
	}

	/**
	 * Resolve parameters list
	 * 
	 * @param executable
	 *            target executable instance {@link Method} or a {@link Constructor}
	 * @param beanFactory
	 *            bean factory
	 * @since 2.1.2
	 * @return parameter list
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
			final Autowired autowiredOnParamter = parameter.getAnnotation(Autowired.class); // @Autowired on parameter
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

			boolean required = true;

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
					bean = resolveProps(props.prefix(), bean, loadProps(props, System.getProperties()));
				}
				else {
					bean = resolveProps(props.prefix(), type, loadProps(props, System.getProperties()));
				}
			}
			if (bean == null && required) {
				// if it is required
				LoggerFactory.getLogger(ContextUtils.class)//
						.error("[{}] is required.", parameter);
				throw new NoSuchBeanDefinitionException(type);
			}
			args[i] = bean;
		}

		return args;
	}

	/**
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	public static final InputStream getResourceAsStream(String resource) throws IOException {

		InputStream in = ClassUtils.getClassLoader().getResourceAsStream(resource);
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

		try (InputStream in = ClassUtils.getClassLoader().getResourceAsStream(StringUtils.checkPropertiesName(resource))) {
			props.load(in);
		}

		return props;
	}

	/**
	 * 
	 * @param urlString
	 * @return
	 * @throws IOException
	 */
	public static final InputStream getUrlAsStream(String urlString) throws IOException {
		return new URL(urlString).openConnection().getInputStream();
	}

	/**
	 * 
	 * @param urlString
	 * @return
	 * @throws IOException
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
	 * @return
	 * @throws ConfigurationException
	 */
	public static String resolvePlaceholder(Map<Object, Object> properties, String value) throws ConfigurationException {
		return resolvePlaceholder(properties, value, true);
	}

	/**
	 * Resolve placeholder s
	 * 
	 * @param properties
	 * @param input
	 * @param throw_
	 *            If there doesn't exist the key throw {@link Exception}
	 * @return
	 * @throws ConfigurationException
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
					throw new ConfigurationException("Properties -> [{}] , must specify a value.", key);
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

	private static final Map<Class<? extends Annotation>, PropertyValueResolver> PROPERTY_VALUE_RESOLVERS;//

	static {
		PROPERTY_VALUE_RESOLVERS = new HashMap<>(4, 1.0f);

		final AutowiredPropertyResolver autowired = new AutowiredPropertyResolver();

		PROPERTY_VALUE_RESOLVERS.put(Resource.class, autowired);
		PROPERTY_VALUE_RESOLVERS.put(Autowired.class, autowired);
		PROPERTY_VALUE_RESOLVERS.put(Value.class, new ValuePropertyResolver());
		PROPERTY_VALUE_RESOLVERS.put(Props.class, new PropsPropertyResolver());
	}

	/**
	 * 
	 * @param beanDefinition
	 * @param initMethods
	 * @since 2.1.3
	 */
	public static void resolveInitMethod(BeanDefinition beanDefinition, String... initMethods) {
		beanDefinition.setInitMethods(resolveInitMethod(beanDefinition.getBeanClass(), initMethods));
	}

	/**
	 * @param beanClass
	 *            bean class
	 * @param initMethods
	 *            init Method s
	 * @since 2.1.2
	 */
	public static Method[] resolveInitMethod(Class<?> beanClass, String... initMethods) {

		if (initMethods == null) {
			initMethods = new String[0];
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
	 *            method list
	 * @param beanClass
	 *            bean class
	 * @param initMethods
	 *            init Method name
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
	 * @param beanDefinition
	 *            bean definition
	 * @param applicationContext
	 * @since 2.1.3
	 */
	public static void resolvePropertyValue(final BeanDefinition beanDefinition, ApplicationContext applicationContext) {
		beanDefinition.setPropertyValues(resolvePropertyValue(beanDefinition.getBeanClass(), applicationContext));
	}

	/**
	 * Process bean's property (field)
	 * 
	 * @param beanClass
	 *            bean class
	 * @param applicationContext
	 * @since 2.1.2
	 */
	public static PropertyValue[] resolvePropertyValue(Class<?> beanClass, //
			ApplicationContext applicationContext) //
	{
		final Set<PropertyValue> propertyValues = new HashSet<>(32, 1.0f);
		for (final Field field : ClassUtils.getFields(beanClass)) {
			final PropertyValue created = createPropertyValue(field, applicationContext);
			// not required
			if (created != null) {
				field.setAccessible(true);
				propertyValues.add(created);
			}
		}
		return propertyValues.toArray(new PropertyValue[0]);
	}

	/**
	 * Create property value
	 * 
	 * @param field
	 *            property
	 * @param applicationContext
	 * @return a new {@link PropertyValue}
	 * @throws Exception
	 */
	public static final PropertyValue createPropertyValue(Field field, ApplicationContext applicationContext) {

		final Map<Class<? extends Annotation>, PropertyValueResolver> propertyValueResolvers = PROPERTY_VALUE_RESOLVERS;
		for (final Annotation annotation : field.getAnnotations()) {
			final PropertyValueResolver propertyValueResolver = propertyValueResolvers.get(annotation.annotationType());
			if (propertyValueResolver != null) {
				return propertyValueResolver.resolveProperty(applicationContext, field);
			}
		}
		return null;
	}

	/**
	 * Properties injection
	 *
	 * @param beanDefinition
	 *            target bean definition
	 * @param environment
	 */
	public static void resolveProps(BeanDefinition beanDefinition, Environment environment) throws ConfigurationException {
		Class<?> beanClass = beanDefinition.getBeanClass();
		if (beanClass.isAnnotationPresent(Props.class)) {
			beanDefinition.addPropertyValue(resolveProps(beanClass, environment.getProperties()));
		}
	}

	/**
	 * Resolve Properties
	 * 
	 * @param annotatedElement
	 * @param properties
	 * @throws ConfigurationException
	 */
	public static List<PropertyValue> resolveProps(AnnotatedElement annotatedElement, Properties properties)
			throws ConfigurationException //
	{
		final Props props = annotatedElement.getAnnotation(Props.class);

		if (props == null) {
			return new ArrayList<>();
		}

		Class<?> annotatedClass = null;
		if (annotatedElement instanceof Class) {
			annotatedClass = (Class<?>) annotatedElement;
		}
		else if (annotatedElement instanceof Method) {
			annotatedClass = ((Method) annotatedElement).getReturnType();
		}
		else {
			throw new ConfigurationException("Not support annotated element: [{}]", annotatedElement);
		}

		log.debug("Loading Properties For: [{}]", annotatedClass.getName());

		final List<PropertyValue> propertyValues = new ArrayList<>();
		final String[] prefixs = props.prefix();

		for (final Field declaredField : ClassUtils.getFields(annotatedClass)) {
			for (final String prefix : prefixs) { // maybe a default value: ""

				final String key = prefix + declaredField.getName();
				final String value = properties.getProperty(key);
				if (value == null) { // just null not include empty
					continue;
				}
				log.debug("Found Properties key: [{}]", key);

				declaredField.setAccessible(true);
				final Object converted = //
						ConvertUtils.convert(ContextUtils.resolvePlaceholder(properties, value), //
								declaredField.getType());

				propertyValues.add(new PropertyValue(converted, declaredField));
			}
		}
		return propertyValues;
	}

	/**
	 * @param prefixs
	 *            {@link Props#prefix()}
	 * @param beanClass
	 *            target class, must have default {@link Constructor}
	 * @param properties
	 *            {@link Properties} source
	 * @return
	 * @since 2.1.5
	 */
	public static <T> T resolveProps(String[] prefixs, Class<T> beanClass, Properties properties) {
		return resolveProps(prefixs, ClassUtils.newInstance(beanClass), properties);
	}

	/**
	 * @param prefixs
	 *            {@link Props#prefix()}
	 * @param bean
	 *            bean instance
	 * @param properties
	 *            {@link Properties} source
	 * @return
	 * @since 2.1.5
	 */
	public static <T> T resolveProps(String[] prefixs, T bean, Properties properties) {

		try {

			for (final Field declaredField : ClassUtils.getFields(bean)) {
				for (final String prefix : prefixs) { // maybe a default value: ""

					final String key = prefix + declaredField.getName();
					Object value = properties.get(key);
					Class<?> type = declaredField.getType();
					if (value == null) { // just null not include empty
						// inject nested Props
						final Props props = declaredField.getAnnotation(Props.class);
						if (props == null) {
							continue;
						}
						final boolean replace = props.replace();
						String[] prefixsToUse = props.prefix();
						if (StringUtils.isArrayEmpty(prefixsToUse)) {
							prefixsToUse = new String[] { key.concat(".") };
						}
						else {
							for (int i = 0; i < prefixsToUse.length; i++) {
								String str = prefixsToUse[i];
								if (StringUtils.isEmpty(str)) {
									prefixsToUse[i] = key.concat(".");
								}
								else if (!replace) { // replace the parent prefix
									prefixsToUse[i] = prefix.concat(str);
								}
							}
						}
						value = resolveProps(prefixsToUse, type, properties);
					}
					final Object converted;
					if (value instanceof String) {
						log.debug("Found Properties key: [{}]", key);
						converted = ConvertUtils.convert(//
								ContextUtils.resolvePlaceholder(properties, (String) value), type//
						);
					}
					else if (type.isInstance(value)) {
						converted = value;
					}
					else {
						continue;
					}

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
	 *            application's {@link Properties}
	 * @return
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
					ret.put(key, //
							ContextUtils.resolvePlaceholder(ret, (String) entry.getValue()));
				}
			}
		}
		return ret;
	}

	/**
	 * If matched
	 * 
	 * @param annotatedElement
	 *            target class or a method
	 * @param applicationContext
	 *            {@link ApplicationContext}
	 * @return If matched
	 */
	public static boolean conditional(AnnotatedElement annotatedElement, ConfigurableApplicationContext applicationContext) {

		final Iterator<Conditional> iterator = //
				ClassUtils.getAnnotation(annotatedElement, Conditional.class, ConditionalImpl.class)//
						.iterator();

		while (iterator.hasNext()) {
			final Conditional conditional = iterator.next();
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
	 * @param applicationContext
	 */
	public static void validateBeanDefinition(BeanDefinition beanDefinition, ApplicationContext applicationContext) {

		if (beanDefinition instanceof StandardBeanDefinition) {
			final StandardBeanDefinition standardBeanDefinition = ((StandardBeanDefinition) beanDefinition);

			if (StringUtils.isEmpty(standardBeanDefinition.getDeclaringName())) {
				throw new ConfigurationException("Declaring name can't be null", beanDefinition);
			}
			if (standardBeanDefinition.getFactoryMethod() == null) {
				throw new ConfigurationException("Factory Method can't be null", beanDefinition);
			}
		}

		if (beanDefinition.getBeanClass() == null) {
			throw new ConfigurationException("Definition's bean class can't be null", beanDefinition);
		}
		if (beanDefinition.getDestroyMethods() == null) {
			beanDefinition.setDestroyMethods(new String[0]);
		}
		if (beanDefinition.getInitMethods() == null) {
			beanDefinition.setInitMethods(resolveInitMethod(beanDefinition.getBeanClass()));
		}
		if (beanDefinition.getPropertyValues() == null) {
			beanDefinition.setPropertyValues(resolvePropertyValue(beanDefinition.getBeanClass(), applicationContext));
		}
		if (beanDefinition.getScope() == null) {
			beanDefinition.setScope(Scope.SINGLETON);
		}
	}

	/**
	 * Destroy bean instance
	 * 
	 * @param bean
	 *            bean instance
	 * @param methods
	 *            methods
	 * @throws Throwable
	 */
	public static void destroyBean(Object bean, Method[] methods) throws Throwable {

		// PreDestroy
		for (final Method method : methods) {
			if (method.isAnnotationPresent(PreDestroy.class)) {
				// fix: can not access a member @since 2.1.6
				method.setAccessible(true);
				method.invoke(bean);
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
	 *            the {@link Annotation} declaredon the class or a method
	 * @param beanClass
	 *            missed bean class
	 * @param beanFactory
	 *            the {@link AbstractBeanFactory}
	 * @return if the bean is missed in context
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

		return !((type != void.class && beanFactory.containsBeanDefinition(type, true)) //
				|| beanFactory.containsBeanDefinition(beanClass));
	}

	/**
	 * @since 2.1.6
	 */
	public static boolean equals(Class<?> one, Class<?> two) {
		return one.getName().equals(two.getName());
	}

}
