/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.properties.bind.BindConstructorProvider;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.annotation.Validated;

/**
 * Provides access to {@link ConfigurationProperties @ConfigurationProperties} bean
 * details, regardless of if the annotation was used directly or on a {@link Bean @Bean}
 * factory method. This class can be used to access {@link #getAll(ApplicationContext)
 * all} configuration properties beans in an ApplicationContext, or
 * {@link #get(ApplicationContext, Object, String) individual beans} on a case-by-case
 * basis (for example, in a {@link BeanPostProcessor}).
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getAll(ApplicationContext)
 * @see #get(ApplicationContext, Object, String)
 * @since 4.0
 */
public final class ConfigurationPropertiesBean {

  private final String name;

  @Nullable
  private final Object instance;

  private final ConfigurationProperties annotation;

  private final Bindable<?> bindTarget;

  private final BindMethod bindMethod;

  private ConfigurationPropertiesBean(String name,
          @Nullable Object instance, ConfigurationProperties annotation,
          Bindable<?> bindTarget, @Nullable BindMethod bindMethod) {
    this.name = name;
    this.instance = instance;
    this.annotation = annotation;
    this.bindTarget = bindTarget;
    this.bindMethod = (bindMethod != null) ? bindMethod : BindMethod.get(bindTarget);
  }

  /**
   * Return the name of the Framework bean.
   *
   * @return the bean name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the actual Framework bean instance.
   *
   * @return the bean instance
   */
  @Nullable
  public Object getInstance() {
    return this.instance;
  }

  /**
   * Return the bean type.
   *
   * @return the bean type
   */
  @Nullable
  Class<?> getType() {
    return this.bindTarget.getType().resolve();
  }

  /**
   * Return the property binding method that was used for the bean.
   *
   * @return the bind type
   */
  public BindMethod getBindMethod() {
    return this.bindMethod;
  }

  /**
   * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
   * may be defined on the bean itself or from the factory method that create the bean
   * (usually a {@link Bean @Bean} method).
   *
   * @return the configuration properties annotation
   */
  public ConfigurationProperties getAnnotation() {
    return this.annotation;
  }

  /**
   * Return a {@link Bindable} instance suitable that can be used as a target for the
   * {@link Binder}.
   *
   * @return a bind target for use with the {@link Binder}
   */
  public Bindable<?> asBindTarget() {
    return this.bindTarget;
  }

  /**
   * Return all {@link ConfigurationProperties @ConfigurationProperties} beans contained
   * in the given application context. Both directly annotated beans, as well as beans
   * that have {@link ConfigurationProperties @ConfigurationProperties} annotated
   * factory methods are included.
   *
   * @param applicationContext the source application context
   * @return a map of all configuration properties beans keyed by the bean name
   */
  public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {
    Assert.notNull(applicationContext, "ApplicationContext must not be null");
    if (applicationContext instanceof ConfigurableApplicationContext) {
      return getAll((ConfigurableApplicationContext) applicationContext);
    }

    var propertiesBeans = new LinkedHashMap<String, ConfigurationPropertiesBean>();
    var beansWithAnnotation = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
    for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
      Object bean = entry.getValue();
      String beanName = entry.getKey();
      ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
      if (propertiesBean != null) {
        propertiesBeans.put(beanName, propertiesBean);
      }
    }
    return propertiesBeans;
  }

  private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
    var propertiesBeans = new LinkedHashMap<String, ConfigurationPropertiesBean>();
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
    while (beanNames.hasNext()) {
      String beanName = beanNames.next();
      if (isConfigurationPropertiesBean(beanFactory, beanName)) {
        try {
          Object bean = beanFactory.getBean(beanName);
          BindMethod bindMethod = getBindMethod(beanFactory, beanName);
          ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName, bindMethod);
          if (propertiesBean != null) {
            propertiesBeans.put(beanName, propertiesBean);
          }
        }
        catch (Exception ignored) { }
      }
    }
    return propertiesBeans;
  }

  @Nullable
  private static BindMethod getBindMethod(ConfigurableBeanFactory beanFactory, String beanName) {
    try {
      BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
      return (BindMethod) beanDefinition.getAttribute(BindMethod.class.getName());
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

  private static boolean isConfigurationPropertiesBean(ConfigurableBeanFactory beanFactory, String beanName) {
    try {
      if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
        return false;
      }
      if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class).isPresent()) {
        return true;
      }
      Method factoryMethod = findFactoryMethod(beanFactory, beanName);
      return findMergedAnnotation(factoryMethod, ConfigurationProperties.class).isPresent();
    }
    catch (NoSuchBeanDefinitionException ex) {
      return false;
    }
  }

  /**
   * Return a {@link ConfigurationPropertiesBean @ConfigurationPropertiesBean} instance
   * for the given bean details or {@code null} if the bean is not a
   * {@link ConfigurationProperties @ConfigurationProperties} object. Annotations are
   * considered both on the bean itself, as well as any factory method (for example a
   * {@link Bean @Bean} method).
   *
   * @param applicationContext the source application context
   * @param bean the bean to consider
   * @param beanName the bean name
   * @return a configuration properties bean or {@code null} if the neither the bean or
   * factory method are annotated with
   * {@link ConfigurationProperties @ConfigurationProperties}
   */
  @Nullable
  public static ConfigurationPropertiesBean get(
          ApplicationContext applicationContext, Object bean, String beanName) {
    return get(applicationContext, bean, beanName, null);
  }

  @Nullable
  private static ConfigurationPropertiesBean get(ApplicationContext applicationContext,
          Object bean, String beanName, @Nullable BindMethod bindMethod) {
    Method factoryMethod = findFactoryMethod(applicationContext, beanName);
    return create(beanName, bean, bean.getClass(), factoryMethod, bindMethod);
  }

  @Nullable
  private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
    if (applicationContext instanceof ConfigurableApplicationContext) {
      return findFactoryMethod((ConfigurableApplicationContext) applicationContext, beanName);
    }
    return null;
  }

  @Nullable
  private static Method findFactoryMethod(ConfigurableApplicationContext applicationContext, String beanName) {
    return findFactoryMethod(applicationContext.getBeanFactory(), beanName);
  }

  @Nullable
  private static Method findFactoryMethod(ConfigurableBeanFactory beanFactory, String beanName) {
    if (beanFactory.containsBeanDefinition(beanName)) {
      BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
      if (beanDefinition instanceof RootBeanDefinition merged) {
        Method resolvedFactoryMethod = merged.getResolvedFactoryMethod();
        if (resolvedFactoryMethod != null) {
          return resolvedFactoryMethod;
        }
        return findFactoryMethodUsingReflection(beanFactory, beanDefinition);
      }
    }
    return null;
  }

  @Nullable
  private static Method findFactoryMethodUsingReflection(
          ConfigurableBeanFactory beanFactory, BeanDefinition beanDefinition) {
    String factoryMethodName = beanDefinition.getFactoryMethodName();
    String factoryBeanName = beanDefinition.getFactoryBeanName();
    if (factoryMethodName == null || factoryBeanName == null) {
      return null;
    }
    Class<?> factoryType = beanFactory.getType(factoryBeanName);
    if (factoryType != null && factoryType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
      factoryType = factoryType.getSuperclass();
    }
    AtomicReference<Method> factoryMethod = new AtomicReference<>();
    ReflectionUtils.doWithMethods(factoryType, (method) -> {
      if (method.getName().equals(factoryMethodName)) {
        factoryMethod.set(method);
      }
    });
    return factoryMethod.get();
  }

  static ConfigurationPropertiesBean forValueObject(Class<?> beanClass, String beanName) {
    ConfigurationPropertiesBean propertiesBean = create(beanName, null, beanClass, null, null);
    if (!(propertiesBean != null && propertiesBean.getBindMethod() == BindMethod.VALUE_OBJECT)) {
      throw new IllegalStateException("Bean '" + beanName + "' is not a @ConfigurationProperties value object");
    }
    return propertiesBean;
  }

  @Nullable
  private static ConfigurationPropertiesBean create(String name,
          @Nullable Object instance, Class<?> type,
          @Nullable Method factory, @Nullable BindMethod bindMethod) {
    ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
    if (annotation == null) {
      return null;
    }
    Validated validated = findAnnotation(instance, type, factory, Validated.class);
    Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
                                                   : new Annotation[] { annotation };
    ResolvableType bindType = (factory != null) ? ResolvableType.forReturnType(factory)
                                                : ResolvableType.forClass(type);
    Bindable<Object> bindable = Bindable.of(bindType).withAnnotations(annotations);
    bindMethod = (factory != null) ? BindMethod.JAVA_BEAN : bindMethod;
    if (instance != null && bindMethod != BindMethod.VALUE_OBJECT) {
      bindable = bindable.withExistingValue(instance);
    }
    return new ConfigurationPropertiesBean(name, instance, annotation, bindable, bindMethod);
  }

  @Nullable
  private static <A extends Annotation> A findAnnotation(
          @Nullable Object instance, Class<?> type, @Nullable Method factory, Class<A> annotationType) {
    MergedAnnotation<A> annotation = MergedAnnotation.missing();
    if (factory != null) {
      annotation = findMergedAnnotation(factory, annotationType);
    }
    if (!annotation.isPresent()) {
      annotation = findMergedAnnotation(type, annotationType);
    }
    if (!annotation.isPresent() && AopUtils.isAopProxy(instance)) {
      annotation = MergedAnnotations.from(AopUtils.getTargetClass(instance), SearchStrategy.TYPE_HIERARCHY)
              .get(annotationType);
    }
    return annotation.isPresent() ? annotation.synthesize() : null;
  }

  private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(
          @Nullable AnnotatedElement element, Class<A> annotationType) {
    return element != null
           ? MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).get(annotationType)
           : MergedAnnotation.missing();
  }

  /**
   * The binding method that is used for the bean.
   */
  public enum BindMethod {

    /**
     * Java Bean using getter/setter binding.
     */
    JAVA_BEAN,

    /**
     * Value object using constructor binding.
     */
    VALUE_OBJECT;

    static BindMethod get(Bindable<?> bindable) {
      return get(BindConstructorProvider.DEFAULT.getBindConstructor(bindable, false));
    }

    private static BindMethod get(@Nullable Constructor<?> bindConstructor) {
      return bindConstructor != null ? VALUE_OBJECT : JAVA_BEAN;
    }

    public static BindMethod forType(Class<?> type) {
      return get(BindConstructorProvider.DEFAULT.getBindConstructor(type, false));
    }

  }

}
