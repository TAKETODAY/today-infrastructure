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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ConditionalImpl;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.PropertyValueResolver;
import cn.taketoday.context.loader.PropsPropertyResolver;
import cn.taketoday.context.loader.ValuePropertyResolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.LoggerFactory;

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
	 * @param method
	 *            target method
	 * @param beanFactory
	 *            bean factory
	 * @since 2.1.2
	 * @return parameter list
	 */
	public static Object[] resolveParameter(Method method, BeanFactory beanFactory) {

		final int parameterLength = method.getParameterCount();
		if (parameterLength == 0) {
			return null;
		}

		// parameter list
		Object[] args = new Object[parameterLength];
		Parameter[] parameters = method.getParameters();

		for (int i = 0; i < parameterLength; i++) {

			Parameter parameter = parameters[i];
			Autowired autowiredOnParamter = parameter.getAnnotation(Autowired.class); // @Autowired on parameter

			if (autowiredOnParamter != null) {
				String name = autowiredOnParamter.value();
				if (StringUtils.isNotEmpty(name)) {
					final Object bean = beanFactory.getBean(name, parameter.getType());
					if (bean == null && autowiredOnParamter.required()) {

						LoggerFactory.getLogger(ContextUtils.class)//
								.error("[{}] is required.", parameter);

						throw new NoSuchBeanDefinitionException(name);
					}
					args[i] = bean; // use name and bean type to get bean
					continue;
				}
			}
			// use parameter type to obtain a bean
			args[i] = beanFactory.getBean(parameter.getType());
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
		Properties props = new Properties();

		try (InputStream in = ClassUtils.getClassLoader().getResourceAsStream(resource)) {
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
		Properties props = new Properties();
		try (InputStream in = getUrlAsStream(urlString)) {
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
		int indexPrefix = 0;
		int indexSuffix = 0;

		StringBuilder builder = new StringBuilder();
		while ((indexPrefix = input.indexOf(Constant.PLACE_HOLDER_PREFIX)) > -1 //
				&& (indexSuffix = input.indexOf(Constant.PLACE_HOLDER_SUFFIX)) > -1) {

			builder.append(input.substring(0, indexPrefix));

			final String key = input.substring(indexPrefix + 2, indexSuffix);

			Object property = properties.get(key);
			if (property == null) {
				if (throw_) {
					throw new ConfigurationException("Properties -> [{}] , must specify a value.", key);
				}
				LoggerFactory.getLogger(ContextUtils.class).info("There is no property for key: [{}]", key);
				return null;
			}

			// find
			builder.append(resolvePlaceholder(properties, (property instanceof String) ? (String) property : null, throw_));
			input = input.substring(indexSuffix + 1);
		}
		return builder.append(input).toString();
	}

	// ----------------- loader

	//@off
	@SuppressWarnings("serial")
	private static final Map<Class<? extends Annotation>, PropertyValueResolver> PROPERTY_VALUE_RESOLVERS = //
		new HashMap<Class<? extends Annotation>, PropertyValueResolver>(4, 1.0f) {{
			final AutowiredPropertyResolver autowired = new AutowiredPropertyResolver();
			put(Resource.class, autowired);
			put(Autowired.class, autowired);
			put(Value.class, new ValuePropertyResolver());
			put(Props.class, new PropsPropertyResolver());
		}
	};
	//@on

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
	static void addInitMethod(List<Method> methods, Class<?> beanClass, String... initMethods) {
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
			if (supportsProperty(field)) {
				final PropertyValue created = createPropertyValue(field, applicationContext);
				// not required
				if (created != null) {
					field.setAccessible(true);
					propertyValues.add(created);
				}
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
	 * @return
	 * @throws Exception
	 */
	static PropertyValue createPropertyValue(Field field, ApplicationContext applicationContext) {

		for (Annotation annotation : field.getAnnotations()) {

			PropertyValueResolver propertyValueResolver = PROPERTY_VALUE_RESOLVERS.get(annotation.annotationType());
			if (propertyValueResolver != null) {
				return propertyValueResolver//
						.resolveProperty(applicationContext, field);
			}
		}
		throw new AnnotationException("Without regulation annotation present.");
	}

	/**
	 * Supports property?
	 * 
	 * @param field
	 *            property
	 * @return
	 */
	static boolean supportsProperty(Field field) {
		for (final Annotation annotation : field.getAnnotations()) {
			if (PROPERTY_VALUE_RESOLVERS.containsKey(annotation.annotationType())) {
				return true;
			}
		}
		return false;
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
	 * If matched
	 * 
	 * @param annotatedElement
	 *            target class or a method
	 * @param applicationContext
	 *            {@link ApplicationContext}
	 * @return If matched
	 */
	public static boolean conditional(AnnotatedElement annotatedElement, ConfigurableApplicationContext applicationContext) {

		Collection<Conditional> conditionals = ClassUtils.getAnnotation(annotatedElement, Conditional.class, ConditionalImpl.class);
		if (conditionals.isEmpty()) {
			return true;
		}

		for (Conditional conditional : conditionals) {
			for (Class<? extends Condition> conditionClass : conditional.value()) {
				Condition condition = ClassUtils.newInstance(conditionClass);
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

}
