/*
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
package cn.taketoday.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
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

import cn.taketoday.beans.Component;
import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.factory.DestructionBeanPostProcessor;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.factory.StandardBeanDefinition;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConcurrentProperties;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionUtils;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * ApplicationContext Utils
 * <p>
 *
 * This class provides el, {@link Properties} loading, {@link Parameter}
 * resolving
 *
 * @author TODAY <br>
 * 2019-01-16 20:04
 */
public abstract class ContextUtils {

  public static final Class<? extends Annotation>
          PreDestroy = ClassUtils.loadClass("javax.annotation.PreDestroy");

  public static final Class<? extends Annotation>
          PostConstruct = ClassUtils.loadClass("javax.annotation.PostConstruct");

  private static final Logger log = LoggerFactory.getLogger(ContextUtils.class);

  // @since 2.1.6 shared applicationContext
  private static ApplicationContext lastStartupContext;

  /**
   * @since 3.0
   */
  private static ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

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
    expressionEvaluator = new ExpressionEvaluator(lastStartupContext);
  }

  /**
   * set Global {@link ExpressionEvaluator}
   *
   * @param expressionEvaluator
   *         a none null ExpressionEvaluator
   *
   * @since 3.0
   */
  public static void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator) {
    Assert.notNull(expressionEvaluator, "ExpressionEvaluator must not be null");
    ContextUtils.expressionEvaluator = expressionEvaluator;
  }

  /**
   * @since 3.0
   */
  public static ExpressionEvaluator getExpressionEvaluator() {
    return expressionEvaluator;
  }

  /**
   * Get shared {@link ExpressionProcessor}
   *
   * @return Shared {@link ExpressionProcessor}
   *
   * @throws IllegalStateException
   *         There isn't a ApplicationContext
   */
  public static ExpressionProcessor getExpressionProcessor() {
    final ApplicationContext ctx = getLastStartupContext();
    Assert.state(ctx != null, "There isn't a ApplicationContext");
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
  public static <T> T resolveValue(final String expression,
                                   final Class<T> expectedType,
                                   final Properties variables) {
    return expressionEvaluator.evaluate(expression, expectedType, variables);
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
    InputStream in = ResourceUtils.getResource(resource).getInputStream();
    if (in == null) {
      throw new IOException("Could not find resource " + resource);
    }
    return in;
  }

  public static Properties getResourceAsProperties(final String resource) throws IOException {
    ConcurrentProperties props = new ConcurrentProperties();
    try (InputStream in = ResourceUtils.getResource(StringUtils.checkPropertiesName(resource)).getInputStream()) {
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
  public static String resolvePlaceholder(final Map<Object, Object> properties, String input, final boolean throw_) {
    return expressionEvaluator.resolvePlaceholder(properties, input, throw_);
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
   * Add a method which annotated with {@link javax.annotation.PostConstruct}
   * or {@link cn.taketoday.beans.Autowired}
   *
   * @param beanClass
   *         Bean class
   * @param initMethods
   *         Init Method name
   *
   * @see AutowiredPropertyResolver#isInjectable(AnnotatedElement)
   * @since 2.1.7
   */
  public static Method[] resolveInitMethod(@Nullable String[] initMethods, Class<?> beanClass) {
    final ArrayList<Method> methods = new ArrayList<>(2);
    final boolean initMethodsNotEmpty = ObjectUtils.isNotEmpty(initMethods);
    do {
      for (final Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
        if (AnnotationUtils.isPresent(method, PostConstruct)
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
    while ((beanClass = beanClass.getSuperclass()) != null && beanClass != Object.class); // all methods

    if (methods.isEmpty()) {
      return BeanDefinition.EMPTY_METHOD;
    }
    AnnotationAwareOrderComparator.sort(methods);
    return methods.toArray(new Method[methods.size()]);
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
    def.addPropertySetter(resolveProps(def, env.getProperties()));
  }

  /**
   * Resolve {@link PropertySetter}s from target {@link Method} or {@link Class}
   *
   * @param annotated
   *         Target {@link AnnotatedElement}
   * @param properties
   *         {@link Properties} variables source
   *
   * @throws ConfigurationException
   *         If not support {@link AnnotatedElement}
   */
  public static List<PropertySetter> resolveProps(
          final AnnotatedElement annotated, final Properties properties) {
    Assert.notNull(annotated, "AnnotatedElement must not be null");
    final Props props = annotated.getAnnotation(Props.class);
    if (props == null) {
      return Collections.emptyList();
    }
    final Class<?> type = getBeanClass(annotated);
    if (log.isDebugEnabled()) {
      log.debug("Loading Properties For: [{}]", type.getName());
    }
    final List<PropertySetter> propertySetters = new ArrayList<>();
    final String[] prefixs = props.prefix();
    final List<Class<?>> nested = Arrays.asList(props.nested());

    for (final Field declaredField : ReflectionUtils.getFields(type)) {
      final Object converted = resolveProps(declaredField, nested, prefixs, properties);
      if (converted != null) {
        propertySetters.add(
                new DefaultPropertySetter(converted, ReflectionUtils.makeAccessible(declaredField)));
      }
    }
    return propertySetters;
  }

  public static Class<?> getBeanClass(final AnnotatedElement annotated) {
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
        return ConversionUtils.convert(value, fieldType);
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
    return resolveProps(props, BeanUtils.newInstance(beanClass), properties);
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
        ReflectionUtils.setField(ReflectionUtils.makeAccessible(declaredField), bean, converted);
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

    // choose properties to use
    final Properties propertiesToUse;
    if (fileNames.length == 0) {
      Assert.notNull(applicationProps, "Application properties must not be null");
      propertiesToUse = applicationProps;
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
          throw new ApplicationContextException("IO exception occurred", e);
        }
      }
    }

    // process -----------------

    final String[] prefixs = props.prefix();
    final boolean replace = props.replace();

    for (final Entry<Object, Object> entry : propertiesToUse.entrySet()) {
      final Object key_ = entry.getKey();
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
  public static boolean passCondition(final AnnotatedElement annotated) {
    return passCondition(annotated, getLastStartupContext());
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
  public static boolean passCondition(final AnnotatedElement annotated, final ApplicationContext context) {
    final AnnotationAttributes[] attributes =
            AnnotationUtils.getAttributesArray(annotated, Conditional.class);
    if (ObjectUtils.isNotEmpty(attributes)) {
      if (attributes.length == 1) {
        return passCondition(annotated, context, attributes[0].getClassArray(Constant.VALUE));
      }
      AnnotationAwareOrderComparator.sort(attributes);
      for (final AnnotationAttributes conditional : attributes) {
        if (!passCondition(annotated, context, conditional.getClassArray(Constant.VALUE))) {
          return false; // can't match
        }
      }
    }
    return true;
  }

  public static boolean passCondition(
          final AnnotatedElement annotated,
          final ApplicationContext context,
          final Class<? extends Condition>[] condition
  ) {
    Assert.notNull(condition, "Condition Class must not be null");

    for (final Class<? extends Condition> conditionClass : condition) {
      if (!BeanUtils.newInstance(conditionClass, context).matches(context, annotated)) {
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
   *
   * @throws ConfigurationException
   *         invalid {@link BeanDefinition}
   */
  public static void validateBeanDefinition(BeanDefinition def) {

    if (def instanceof StandardBeanDefinition) {
      final StandardBeanDefinition standardDef = ((StandardBeanDefinition) def);

      if (StringUtils.isEmpty(standardDef.getDeclaringName())) {
        throw new ConfigurationException("Declaring name can't be null in: " + standardDef);
      }
      ConfigurationException.nonNull(standardDef.getFactoryMethod(), "Factory Method can't be null");
    }
    ConfigurationException.nonNull(def.getName(), "Definition's bean name can't be null");
    ConfigurationException.nonNull(def.getBeanClass(), "Definition's bean class can't be null");

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

    if (CollectionUtils.isNotEmpty(postProcessors)) {
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
              || AnnotationUtils.isPresent(method, PreDestroy)) // PreDestroy
              && method.getParameterCount() == 0) { // 0参数
        // fix: can not access a member @since 2.1.6
        ReflectionUtils.makeAccessible(method).invoke(obj);
      }
    }

    if (obj instanceof DisposableBean) {
      ((DisposableBean) obj).destroy();
    }
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

    final AnnotationAttributes[] componentAttributes =
            AnnotationUtils.getAttributesArray(beanClass, Component.class);
    if (ObjectUtils.isEmpty(componentAttributes)) {
      return Collections.singletonList(beanDefinitionLoader.createBeanDefinition(defaultName, beanClass));
    }
    final ArrayList<BeanDefinition> ret = new ArrayList<>(componentAttributes.length);
    for (final AnnotationAttributes attributes : componentAttributes) {
      for (final String name : findNames(defaultName, attributes.getStringArray(Constant.VALUE))) {
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
   * @throws ApplicationContextException
   *         If any {@link IOException} occurred
   */
  public static Set<Class<?>> loadFromMetaInfo(final String resource) {
    Assert.notNull(resource, "META-INF resource must not be null");

    if (resource.startsWith("META-INF")) {

      final Set<Class<?>> ret = new HashSet<>();
      final ClassLoader classLoader = ClassUtils.getClassLoader();
      final Charset charset = Constant.DEFAULT_CHARSET;
      try {
        final Enumeration<URL> resources = classLoader.getResources(resource);
        while (resources.hasMoreElements()) {
          final URL url = resources.nextElement();
          String className = null;
          try (final BufferedReader reader = //
                  new BufferedReader(new InputStreamReader(url.openStream(), charset))) {

            while ((className = reader.readLine()) != null) {
              if (StringUtils.isNotEmpty(className)) { // @since 3.0 FIX empty lines
                ret.add(classLoader.loadClass(className));
              }
            }
          }
          catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class file: '" + className + "' not in " + url);
          }
        }
        return ret;
      }
      catch (IOException e) {
        throw new ApplicationContextException("Exception occurred when load from '" + resource + '\'', e);
      }
    }
    throw new ConfigurationException("Resource must start with 'META-INF'");
  }

  /**
   * Scan beans set from META-INF/xxx
   *
   * @param resource
   *         Resource file start with 'META-INF'
   *
   * @return bean set from META-INF/xxx
   *
   * @throws ApplicationContextException
   *         If any {@link IOException} occurred
   * @since 3.0
   */
  public static <T> Set<T> loadBeansFromMetaInfo(String resource) {
    return loadBeansFromMetaInfo(resource, getLastStartupContext());
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> loadBeansFromMetaInfo(String resource, BeanFactory beanFactory) {
    final Set<Class<?>> classes = loadFromMetaInfo(resource);
    Set<T> ret = new HashSet<>();
    for (final Class<?> aClass : classes) {
      final Object obj = BeanUtils.newInstance(aClass, beanFactory);
      ret.add((T) obj);
    }
    return ret;
  }

}
