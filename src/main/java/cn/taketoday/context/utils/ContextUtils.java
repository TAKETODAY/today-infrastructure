/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConcurrentProperties;
import cn.taketoday.context.Condition;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.DefaultProps;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.factory.DefaultPropertyValue;
import cn.taketoday.context.factory.DestructionBeanPostProcessor;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.factory.StandardBeanDefinition;
import cn.taketoday.context.loader.ArrayParameterResolver;
import cn.taketoday.context.loader.AutowiredParameterResolver;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.CollectionParameterResolver;
import cn.taketoday.context.loader.ExecutableParameterResolver;
import cn.taketoday.context.loader.MapParameterResolver;
import cn.taketoday.context.loader.ObjectSupplierParameterResolver;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionProcessor;

import static cn.taketoday.context.Constant.VALUE;
import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.context.loader.DelegatingParameterResolver.delegate;
import static cn.taketoday.context.utils.ClassUtils.getAnnotationAttributesArray;
import static cn.taketoday.context.utils.OrderUtils.reversedSort;
import static cn.taketoday.context.utils.ReflectionUtils.makeAccessible;
import static cn.taketoday.context.utils.ResourceUtils.getResource;
import static java.util.Objects.requireNonNull;

/**
 * This class provides el, {@link Properties} loading, {@link Parameter}
 * resolving
 *
 * @author TODAY <br>
 * 2019-01-16 20:04
 */
public abstract class ContextUtils {

  private static final Logger log = LoggerFactory.getLogger(ContextUtils.class);

  // @since 2.1.6 shared applicationContext
  private static ApplicationContext lastStartupContext;

  private static ExecutableParameterResolver[] parameterResolvers;

  static {

    setParameterResolvers(new MapParameterResolver(),
                          new ArrayParameterResolver(),
                          new CollectionParameterResolver(),
                          new ObjectSupplierParameterResolver(),
                          new AutowiredParameterResolver(),
                          delegate(p -> p.isAnnotationPresent(Env.class),
                                   (p, b) -> resolveValue(p.getAnnotation(Env.class), p.getType())),
                          delegate(p -> p.isAnnotationPresent(Value.class),
                                   (p, b) -> resolveValue(p.getAnnotation(Value.class), p.getType()))//
    );
  }

  /**
   * Get {@link ApplicationContext}
   *
   * @return {@link ApplicationContext}
   */
  public static ApplicationContext getLastStartupContext() {
    return lastStartupContext;
  }

  public static void setLastStartupContext(ApplicationContext lastStartupContext) {
    ContextUtils.lastStartupContext = lastStartupContext;
  }

  /**
   * Get shared {@link ExpressionProcessor}
   *
   * @return Shared {@link ExpressionProcessor}
   */
  public static ExpressionProcessor getExpressionProcessor() {
    final ApplicationContext ctx = getLastStartupContext();
    Assert.notNull(ctx, "There isn't a ApplicationContext");
    return ctx.getEnvironment().getExpressionProcessor();
  }

  // PropertyValueResolver
  // -----------------------------------------

  /**
   * Find bean names
   *
   * @param defaultName
   *         Default bean name
   * @param names
   *         Annotation values
   *
   * @return Bean names
   */
  public static String[] findNames(final String defaultName, final String... names) {
    if (StringUtils.isArrayEmpty(names)) {
      return new String[] { defaultName }; // default name
    }
    return names;
  }

  /**
   * Resolve {@link Env} {@link Annotation}
   *
   * @param value
   *         {@link Env} {@link Annotation}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @throws ConfigurationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public static <T> T resolveValue(final Env value, final Class<T> expectedType) {

    final T resolveValue = resolveValue(
            new StringBuilder()
                    .append(Constant.PLACE_HOLDER_PREFIX)
                    .append(value.value())
                    .append(Constant.PLACE_HOLDER_SUFFIX).toString(), expectedType
    );

    if (resolveValue != null) {
      return resolveValue;
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
   *         {@link Value} {@link Annotation}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @throws ConfigurationException
   *         Can't resolve expression
   * @since 2.1.6
   */
  public static <T> T resolveValue(final Value value, final Class<T> expectedType) {

    final T resolveValue = resolveValue(value.value(), expectedType);
    if (resolveValue != null) {
      return resolveValue;
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
   *         expression {@link String}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @throws ConfigurationException
   *         ExpressionException
   * @since 2.1.6
   */
  public static <T> T resolveValue(final String expression, final Class<T> expectedType) {
    return resolveValue(expression, expectedType, getLastStartupContext().getEnvironment().getProperties());
  }

  /**
   * replace a placeholder or eval el
   *
   * @param expression
   *         expression {@link String}
   * @param expectedType
   *         expected value type
   *
   * @return A resolved value object
   *
   * @since 2.1.6
   */
  @SuppressWarnings("unchecked")
  public static <T> T resolveValue(final String expression,
                                   final Class<T> expectedType,
                                   final Properties variables) {
    if (expression.contains(Constant.PLACE_HOLDER_PREFIX)) {
      final String replaced = resolvePlaceholder(variables, expression, false);
      return (T) ConvertUtils.convert(replaced, expectedType);
    }

    if (expression.contains(Constant.EL_PREFIX)) {
      try {
        return getExpressionProcessor().getValue(expression, expectedType);
      }
      catch (ExpressionException e) {
        throw new ConfigurationException(e);
      }
    }
    return (T) ConvertUtils.convert(expression, expectedType);
  }

  /**
   * Get a {@link InputStream} from given resource string
   *
   * @param resource
   *         Target resource string
   *
   * @return A {@link InputStream}
   *
   * @throws IOException
   *         If any IO {@link Exception} occurred
   */
  public static InputStream getResourceAsStream(final String resource) throws IOException {

    InputStream in = getResource(resource).getInputStream();
    if (in == null) {
      throw new IOException("Could not find resource " + resource);
    }
    return in;
  }

  public static Properties getResourceAsProperties(final String resource) throws IOException {
    ConcurrentProperties props = new ConcurrentProperties();

    try (InputStream in = getResource(StringUtils.checkPropertiesName(resource)).getInputStream()) {
      props.load(in);
    }

    return props;
  }

  /**
   * Get {@link InputStream} from a url stirng
   *
   * @param urlString
   *         Target url string
   *
   * @return {@link InputStream}
   *
   * @throws IOException
   *         If can't get the stream
   */
  public static InputStream getUrlAsStream(final String urlString) throws IOException {
    return new URL(urlString).openConnection().getInputStream();
  }

  /**
   * Load {@link Properties} from a url string
   *
   * @param urlString
   *         Target url string
   *
   * @return {@link Properties}
   *
   * @throws IOException
   *         If any IO {@link Exception} occurred
   */
  public static Properties getUrlAsProperties(final String urlString) throws IOException {
    ConcurrentProperties props = new ConcurrentProperties();
    try (InputStream in = getUrlAsStream(StringUtils.checkPropertiesName(urlString))) {
      props.load(in);
    }
    return props;
  }

  /**
   * Resolve placeholder s
   *
   * @param properties
   *         {@link Properties}
   * @param value
   *         the value will as a key, if don't exist return itself
   *
   * @return A resolved string
   *
   * @throws ConfigurationException
   *         If not exist target property
   */
  public static String resolvePlaceholder(final Map<Object, Object> properties, final String value) {
    return resolvePlaceholder(properties, value, true);
  }

  /**
   * Resolve placeholder s
   *
   * @param properties
   *         {@link Properties} variables source
   * @param input
   *         Input expression
   * @param throw_
   *         If there doesn't exist the key throw {@link Exception}
   *
   * @return A resolved string
   *
   * @throws ConfigurationException
   *         If not exist target property
   */
  public static String resolvePlaceholder(final Map<Object, Object> properties,
                                          String input, final boolean throw_) {
    if (input == null || input.length() <= 3) { // #{} > 3
      return input;
    }
    int prefixIndex;
    int suffixIndex;

    final StringBuilder builder = new StringBuilder();
    while ((prefixIndex = input.indexOf(Constant.PLACE_HOLDER_PREFIX)) > -1 //
            && (suffixIndex = input.indexOf(Constant.PLACE_HOLDER_SUFFIX)) > -1) {

      builder.append(input, 0, prefixIndex);

      final String key = input.substring(prefixIndex + 2, suffixIndex);

      final Object property = properties.get(key);
      if (property == null) {
        if (throw_) {
          throw new ConfigurationException("Properties -> [" + key + "] , must specify a value.");
        }
        log.debug("There is no property for key: [{}]", key);
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
   * @param def
   *         Target {@link BeanDefinition}
   * @param initMethods
   *         Resolved init methods
   *
   * @since 2.1.3
   */
  public static void resolveInitMethod(final BeanDefinition def, final String... initMethods) {
    def.setInitMethods(resolveInitMethod(initMethods, def.getBeanClass()));
  }

  /**
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method s
   *
   * @since 2.1.2
   */
  public static Method[] resolveInitMethod(final Class<?> beanClass, final String... initMethods) {
    return resolveInitMethod(initMethods, beanClass);
  }

  /**
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method s
   *
   * @since 2.1.7
   */
  public static Method[] resolveInitMethod(String[] initMethods, Class<?> beanClass) {
    final ArrayList<Method> methods = new ArrayList<>(2);
    do {
      addInitMethod(methods, beanClass, initMethods);
    }
    while ((beanClass = beanClass.getSuperclass()) != null && beanClass != Object.class); // all methods

    if (methods.isEmpty()) {
      return BeanDefinition.EMPTY_METHOD;
    }

    reversedSort(methods);
    return methods.toArray(new Method[methods.size()]);
  }

  /**
   * Add a method which annotated with {@link PostConstruct}
   *
   * @param methods
   *         Method list
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method name
   *
   * @since 2.1.2
   */
  private static void addInitMethod(
          final List<Method> methods, final Class<?> beanClass, final String[] initMethods
  ) {
    final boolean initMethodsNotEmpty = StringUtils.isArrayNotEmpty(initMethods);
    for (final Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
      if (ClassUtils.isAnnotationPresent(method, PostConstruct.class)
              || AutowiredPropertyResolver.isInjectable(method)) { // method Injection
        methods.add(method);
        continue;
      }
      if (initMethodsNotEmpty) {
        final String name = method.getName();
        for (final String initMethod : initMethods) {
          if (initMethod.equals(name)) { // equals
            methods.add(method);
          }
        }
      }
    }
  }

  /**
   * Properties injection
   *
   * @param def
   *         Target bean definition
   * @param env
   *         Application {@link Environment}
   */
  public static void resolveProps(final BeanDefinition def, final Environment env) {
    def.addPropertyValue(resolveProps(def, env.getProperties()));
  }

  /**
   * Resolve {@link PropertyValue}s from target {@link Method} or {@link Class}
   *
   * @param annotated
   *         Target {@link AnnotatedElement}
   * @param properties
   *         {@link Properties} variables source
   *
   * @throws ConfigurationException
   *         If not support {@link AnnotatedElement}
   */
  public static List<PropertyValue> resolveProps(final AnnotatedElement annotated,
                                                 final Properties properties) {
    Assert.notNull(annotated, "AnnotatedElement must not be null");

    final Props props = annotated.getAnnotation(Props.class);

    if (props == null) {
      return Collections.emptyList();
    }

    final Class<?> type = getBeanClass(annotated);
    if (log.isDebugEnabled()) {
      log.debug("Loading Properties For: [{}]", type.getName());
    }
    final List<PropertyValue> propertyValues = new ArrayList<>();
    final String[] prefixs = props.prefix();
    final List<Class<?>> nested = Arrays.asList(props.nested());

    for (final Field declaredField : ReflectionUtils.getFields(type)) {
      final Object converted = resolveProps(declaredField, nested, prefixs, properties);
      if (converted != null) {
        propertyValues.add(new DefaultPropertyValue(converted, makeAccessible(declaredField)));
      }
    }
    return propertyValues;
  }

  private static Class<?> getBeanClass(final AnnotatedElement annotated) {
    if (annotated instanceof Class) {
      return (Class<?>) annotated;
    }
    if (annotated instanceof BeanDefinition) {
      return ((BeanDefinition) annotated).getBeanClass();
    }
    if (annotated instanceof Method) {
      return ((Method) annotated).getReturnType();
    }
    throw new ConfigurationException("Not support annotated element: [" + annotated + "]");
  }

  /**
   * Resolve target {@link Field} object
   *
   * @param declaredField
   *         Target Field
   * @param nested
   *         Field class's field class
   * @param prefixs
   *         {@link Properties}'s prefix
   * @param properties
   *         {@link Properties} variables source
   *
   * @return Resolved field object
   */
  public static Object resolveProps(
          final Field declaredField,
          final List<Class<?>> nested,
          final String[] prefixs,
          final Properties properties
  ) {
    final Class<?> fieldType = declaredField.getType();
    final boolean debugEnabled = log.isDebugEnabled();
    for (final String prefix : prefixs) {// maybe a default value: ""

      final String key = prefix.concat(declaredField.getName());

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
      if (debugEnabled) {
        log.debug("Found Property: [{}] = [{}]", key, value);
      }
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
   * @param props
   *         {@link Props}
   * @param beanClass
   *         Target class, must have default {@link Constructor}
   * @param properties
   *         {@link Properties} variables source
   *
   * @since 2.1.5
   */
  public static <T> T resolveProps(final Props props, final Class<T> beanClass, final Properties properties) {
    return resolveProps(props, ClassUtils.newInstance(beanClass), properties);
  }

  /**
   * Resolve target object with {@link Props} and target object's instance
   *
   * @param bean
   *         Bean instance
   * @param properties
   *         {@link Properties} variables source
   *
   * @since 2.1.5
   */
  public static <T> T resolveProps(final Props props, final T bean, final Properties properties) {

    final String[] prefixs = props.prefix();
    final List<Class<?>> nested = Arrays.asList(props.nested());

    for (final Field declaredField : ReflectionUtils.getFields(bean)) {
      final Object converted = resolveProps(declaredField, nested, prefixs, properties);
      if (converted != null) {
        ReflectionUtils.setField(makeAccessible(declaredField), bean, converted);
      }
    }
    return bean;
  }

  /**
   * Load {@link Properties} from {@link Props} {@link Annotation}
   *
   * @param props
   *         {@link Props}
   * @param applicationProps
   *         Application's {@link Properties}
   *
   * @since 2.1.5
   */
  public static Properties loadProps(final Props props, final Properties applicationProps) {

    final Properties ret = new ConcurrentProperties();
    final String[] fileNames = props.value();

    final Properties propertiesToUse;
    if (fileNames.length == 0) {
      propertiesToUse = requireNonNull(applicationProps);
    }
    else {
      propertiesToUse = new ConcurrentProperties();
      for (String fileName : fileNames) {
        if (StringUtils.isEmpty(fileName)) {
          propertiesToUse.putAll(applicationProps);
          break;
        }
        try (InputStream inputStream = getResourceAsStream(StringUtils.checkPropertiesName(fileName))) {
          propertiesToUse.load(inputStream);
        }
        catch (IOException e) {
          throw new ContextException("IO exception occurred", e);
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
      final String blank = Constant.BLANK;
      for (String prefix : prefixs) {
        if (blank.equals(prefix) || key.startsWith(prefix)) { // start with prefix
          if (replace) {
            // replace the prefix
            key = key.replaceFirst(prefix, blank);
          }
          // resolvePlaceholder(propertiesToUse, (String) entry.getValue())
          final Object value = entry.getValue();
          if (value instanceof String) { // fix only support String
            ret.put(key, resolveValue((String) value, Object.class, propertiesToUse));
          }
          else {
            ret.put(key, value);
          }
        }
      }
    }
    return ret;
  }

  /**
   * Decide whether to load the bean
   *
   * @param annotated
   *         Target class or a method
   *
   * @return If matched
   */
  public static boolean conditional(final AnnotatedElement annotated) {
    return conditional(annotated, getLastStartupContext());
  }

  /**
   * Decide whether to load the bean
   *
   * @param annotated
   *         Target class or a method
   * @param context
   *         {@link ApplicationContext}
   *
   * @return If matched
   */
  public static boolean conditional(final AnnotatedElement annotated, final ApplicationContext context) {
    final AnnotationAttributes[] attributes = getAnnotationAttributesArray(annotated, Conditional.class);
    if (ObjectUtils.isEmpty(attributes)) {
      return true;
    }
    if (attributes.length == 1) {
      return conditional(annotated, context, attributes[0].getClassArray(VALUE));
    }
    for (final AnnotationAttributes conditional : reversedSort(attributes)) {
      if (!conditional(annotated, context, conditional.getClassArray(VALUE))) {
        return false; // can't match
      }
    }
    return true;
  }

  public static boolean conditional(
          final AnnotatedElement annotated,
          final ApplicationContext context,
          final Class<? extends Condition>[] condition
  ) {
    Assert.notNull(condition, "Condition Class must not be null");

    for (final Class<? extends Condition> conditionClass : condition) {
      if (!ClassUtils.newInstance(conditionClass, context).matches(context, annotated)) {
        return false; // can't match
      }
    }
    return true;
  }

  /**
   * Validate bean definition
   *
   * @param def
   *         Target {@link BeanDefinition}
   */
  public static void validateBeanDefinition(BeanDefinition def) {

    if (def instanceof StandardBeanDefinition) {
      final StandardBeanDefinition standardDef = ((StandardBeanDefinition) def);

      if (StringUtils.isEmpty(standardDef.getDeclaringName())) {
        throw new ConfigurationException("Declaring name can't be null in: " + standardDef);
      }
      nonNull(standardDef.getFactoryMethod(), "Factory Method can't be null");
    }

    nonNull(def.getName(), "Definition's bean name can't be null");
    nonNull(def.getBeanClass(), "Definition's bean class can't be null");

    if (def.getDestroyMethods() == null) {
      def.setDestroyMethods(Constant.EMPTY_STRING_ARRAY);
    }
    if (def.getInitMethods() == null) {
      def.setInitMethods(resolveInitMethod(null, def.getBeanClass()));
    }
  }

  /**
   * Destroy bean instance
   *
   * @param obj
   *         Bean instance
   *
   * @throws Exception
   *         When destroy a bean
   */
  public static void destroyBean(final Object obj) throws Exception {
    destroyBean(obj, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj
   *         Bean instance
   *
   * @throws Exception
   *         When destroy a bean
   */
  public static void destroyBean(final Object obj, final BeanDefinition def) throws Exception {
    destroyBean(obj, def, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj
   *         Bean instance
   *
   * @throws Exception
   *         When destroy a bean
   */
  public static void destroyBean(final Object obj,
                                 final BeanDefinition def,
                                 final List<BeanPostProcessor> postProcessors) throws Exception {

    Assert.notNull(obj, "bean instance must not be null");

    if (!CollectionUtils.isEmpty(postProcessors)) {
      for (final BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof DestructionBeanPostProcessor) {
          final DestructionBeanPostProcessor destruction = (DestructionBeanPostProcessor) postProcessor;
          if (destruction.requiresDestruction(obj)) {
            destruction.postProcessBeforeDestruction(obj, def);
          }
        }
      }
    }

    // use real class
    final Class<?> beanClass = ClassUtils.getUserClass(obj);
    final List<String> destroyMethods = def != null ? Arrays.asList(def.getDestroyMethods()) : null;

    for (final Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
      if (((destroyMethods != null && destroyMethods.contains(method.getName()))
              || method.isAnnotationPresent(PreDestroy.class)) // PreDestroy
              && method.getParameterCount() == 0) { // 一个参数
        // fix: can not access a member @since 2.1.6
        makeAccessible(method).invoke(obj);
      }
    }

    if (obj instanceof DisposableBean) {
      ((DisposableBean) obj).destroy();
    }
  }

  /**
   * Is a context missed bean?
   *
   * @param missingBean
   *         The {@link Annotation} declared on the class or a method
   * @param annotated
   *         Missed bean class or method
   * @param beanFactory
   *         The {@link AbstractBeanFactory}
   *
   * @return If the bean is missed in context
   *
   * @since 2.1.6
   */
  public static boolean isMissedBean(
          final MissingBean missingBean,
          final AnnotatedElement annotated,
          final ConfigurableBeanFactory beanFactory
  ) {
    if (missingBean == null || !conditional(annotated)) { // fix @Conditional not
      return false;
    }

    final String beanName = missingBean.value();
    if (StringUtils.isNotEmpty(beanName) && beanFactory.containsBeanDefinition(beanName)) {
      return false;
    }
    final Class<?> type = missingBean.type();

    return !((type != void.class && beanFactory.containsBeanDefinition(type, !type.isInterface())) //
            || beanFactory.containsBeanDefinition(getBeanClass(annotated)));
  }

  // bean definition

  /**
   * Build for a bean class with given default bean name
   *
   * @param beanClass
   *         Target bean class
   * @param defaultName
   *         Default bean name
   *
   * @return List of {@link BeanDefinition}s
   */
  public static List<BeanDefinition> createBeanDefinitions(
          final String defaultName, final Class<?> beanClass
  ) {
    return createBeanDefinitions(defaultName, beanClass, getLastStartupContext());
  }

  public static List<BeanDefinition> createBeanDefinitions(
          final String defaultName,
          final Class<?> beanClass,
          final ApplicationContext context
  ) {
    return createBeanDefinitions(defaultName, beanClass, context.getEnvironment().getBeanDefinitionLoader());
  }

  public static List<BeanDefinition> createBeanDefinitions(
          final String defaultName,
          final Class<?> beanClass,
          final BeanDefinitionLoader beanDefinitionLoader
  ) {

    final AnnotationAttributes[] componentAttributes = getAnnotationAttributesArray(beanClass, Component.class);
    if (ObjectUtils.isEmpty(componentAttributes)) {
      return Collections.singletonList(beanDefinitionLoader.createBeanDefinition(defaultName, beanClass));
    }
    final ArrayList<BeanDefinition> ret = new ArrayList<>(componentAttributes.length);
    for (final AnnotationAttributes attributes : componentAttributes) {
      for (final String name : findNames(defaultName, attributes.getStringArray(VALUE))) {
        ret.add(beanDefinitionLoader.createBeanDefinition(name, beanClass, attributes));
      }
    }
    return ret;
  }

  public static BeanDefinition createBeanDefinition(String beanName, Class<?> beanClass) {
    return createBeanDefinition(beanName, beanClass, getLastStartupContext());
  }

  public static BeanDefinition createBeanDefinition(
          final String beanName, final Class<?> beanClass, final ApplicationContext ctx
  ) {
    return createBeanDefinition(beanName, beanClass, null, ctx);
  }

  public static BeanDefinition createBeanDefinition(
          final String name, final Class<?> bean, final AnnotationAttributes attributes
  ) {
    return createBeanDefinition(name, bean, attributes, getLastStartupContext());
  }

  public static BeanDefinition createBeanDefinition(
          final String beanName,
          final Class<?> beanClass,
          final AnnotationAttributes attributes,
          final ApplicationContext applicationContext
  ) {
    Assert.notNull(applicationContext, "ApplicationContext must not be null");
    return applicationContext
            .getEnvironment()
            .getBeanDefinitionLoader()
            .createBeanDefinition(beanName, beanClass, attributes);
  }

  // META-INF
  // ----------------------

  /**
   * Scan classes set from META-INF/xxx
   *
   * @param resource
   *         Resource file start with 'META-INF'
   *
   * @return Class set from META-INF/xxx
   *
   * @throws ContextException
   *         If any {@link IOException} occurred
   */
  public static Set<Class<?>> loadFromMetaInfo(final String resource) {

    if (requireNonNull(resource).startsWith("META-INF")) {

      final Set<Class<?>> ret = new HashSet<>();
      final ClassLoader classLoader = ClassUtils.getClassLoader();
      final Charset charset = Constant.DEFAULT_CHARSET;
      try {
        final Enumeration<URL> resources = classLoader.getResources(resource);
        while (resources.hasMoreElements()) {
          try (final BufferedReader reader = //
                  new BufferedReader(new InputStreamReader(resources.nextElement().openStream(), charset))) {

            String str;
            while ((str = reader.readLine()) != null) {
              ret.add(classLoader.loadClass(str));
            }
          }
        }
        return ret;
      }
      catch (IOException | ClassNotFoundException e) {
        throw new ContextException("Exception occurred when load from '" + resource + "' Msg: " + e, e);
      }
    }
    throw new ConfigurationException("Resource must start with 'META-INF'");
  }

  // ExecutableParameterResolver @since 2.17
  // ----------------------------------------------

  public static ExecutableParameterResolver[] getParameterResolvers() {
    return parameterResolvers;
  }

  public static void setParameterResolvers(ExecutableParameterResolver... resolvers) {
    synchronized (ContextUtils.class) {
      parameterResolvers = reversedSort(nonNull(resolvers, "ExecutableParameterResolver must not null"));
    }
  }

  public static void addParameterResolvers(ExecutableParameterResolver... resolvers) {

    if (ObjectUtils.isNotEmpty(resolvers)) {

      final List<ExecutableParameterResolver> newResolvers = new ArrayList<>();

      if (getParameterResolvers() != null) {
        Collections.addAll(newResolvers, getParameterResolvers());
      }

      Collections.addAll(newResolvers, resolvers);
      setParameterResolvers(newResolvers.toArray(new ExecutableParameterResolver[newResolvers.size()]));
    }
  }

  /**
   * Resolve parameters list
   *
   * @param executable
   *         Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory
   *         Bean factory
   *
   * @return Parameter list objects
   *
   * @since 2.1.2
   */
  public static Object[] resolveParameter(final Executable executable, final BeanFactory beanFactory) {
    return resolveParameter(executable, beanFactory, null);
  }

  /**
   * Resolve parameters list
   *
   * @param executable
   *         Target executable instance {@link Method} or a {@link Constructor}
   * @param beanFactory
   *         Bean factory
   * @param providedArgs
   *         provided args
   *
   * @return Parameter list objects
   *
   * @since 3.0
   */
  public static Object[] resolveParameter(final Executable executable, final BeanFactory beanFactory, Object[] providedArgs) {
    Assert.notNull(executable, "Executable must not be null");

    final int parameterLength = executable.getParameterCount();
    if (parameterLength == 0) {
      return null;
    }
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    // parameter list
    final Object[] args = new Object[parameterLength];
    int i = 0;
    for (final Parameter parameter : executable.getParameters()) {
      final Object argument = findProvidedArgument(parameter, providedArgs);
      args[i++] = argument != null ? argument : getParameterResolver(parameter).resolve(parameter, beanFactory);
    }
    return args;
  }

  protected static Object findProvidedArgument(Parameter parameter, Object[] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      final Class<?> parameterType = parameter.getType();
      for (final Object providedArg : providedArgs) {
        if (parameterType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }

    return null;
  }

  public static ExecutableParameterResolver getParameterResolver(final Parameter parameter) {

    for (final ExecutableParameterResolver resolver : getParameterResolvers()) {
      if (resolver.supports(parameter)) {
        return resolver;
      }
    }
    throw new ConfigurationException("Target parameter:[" + parameter + "] not supports in this context.");
  }

}
