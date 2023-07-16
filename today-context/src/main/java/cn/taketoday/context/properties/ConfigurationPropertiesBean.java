/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.context.properties.bind.BindMethod;
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

import static cn.taketoday.context.properties.bind.BindConstructorProvider.DEFAULT;

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

  private final Bindable<?> bindTarget;

  private ConfigurationPropertiesBean(String name,
          @Nullable Object instance, Bindable<?> bindTarget) {
    this.name = name;
    this.instance = instance;
    this.bindTarget = bindTarget;
  }

  /**
   * Return the name of the bean.
   *
   * @return the bean name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the actual Spring bean instance.
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
   * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
   * may be defined on the bean itself or from the factory method that create the bean
   * (usually a {@link Bean @Bean} method).
   *
   * @return the configuration properties annotation
   */
  public ConfigurationProperties getAnnotation() {
    return this.bindTarget.getAnnotation(ConfigurationProperties.class);
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
    if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
      return getAll(configurableContext);
    }
    Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
    applicationContext.getBeansWithAnnotation(ConfigurationProperties.class).forEach((name, instance) -> {
      ConfigurationPropertiesBean propertiesBean = get(applicationContext, instance, name);
      if (propertiesBean != null) {
        propertiesBeans.put(name, propertiesBean);
      }
    });
    return propertiesBeans;
  }

  private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
    Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
    ConfigurableBeanFactory beanFactory = applicationContext.getBeanFactory();
    Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
    while (beanNames.hasNext()) {
      String beanName = beanNames.next();
      if (isConfigurationPropertiesBean(beanFactory, beanName)) {
        try {
          Object bean = beanFactory.getBean(beanName);
          ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
          if (propertiesBean != null) {
            propertiesBeans.put(beanName, propertiesBean);
          }
        }
        catch (Exception ignored) { }
      }
    }
    return propertiesBeans;
  }

  private static boolean isConfigurationPropertiesBean(ConfigurableBeanFactory beanFactory, String beanName) {
    try {
      if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
        return false;
      }
      if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class) != null) {
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
   * @return a configuration properties bean or {@code null} if the neither the bean nor
   * factory method are annotated with
   * {@link ConfigurationProperties @ConfigurationProperties}
   */
  @Nullable
  public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
    Method factoryMethod = findFactoryMethod(applicationContext, beanName);
    Bindable<Object> bindTarget = createBindTarget(bean, bean.getClass(), factoryMethod);
    if (bindTarget == null) {
      return null;
    }
    bindTarget = bindTarget.withBindMethod(BindMethodAttribute.get(applicationContext, beanName));
    if (bindTarget.getBindMethod() == null && factoryMethod != null) {
      bindTarget = bindTarget.withBindMethod(BindMethod.JAVA_BEAN);
    }
    if (bindTarget.getBindMethod() != BindMethod.VALUE_OBJECT) {
      bindTarget = bindTarget.withExistingValue(bean);
    }
    return create(beanName, bean, bindTarget);
  }

  @Nullable
  private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
    if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
      return findFactoryMethod(configurableContext, beanName);
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
      if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
        Method resolvedFactoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
        if (resolvedFactoryMethod != null) {
          return resolvedFactoryMethod;
        }
      }
      return findFactoryMethodUsingReflection(beanFactory, beanDefinition);
    }
    return null;
  }

  @Nullable
  private static Method findFactoryMethodUsingReflection(ConfigurableBeanFactory beanFactory,
          BeanDefinition beanDefinition) {
    String factoryMethodName = beanDefinition.getFactoryMethodName();
    String factoryBeanName = beanDefinition.getFactoryBeanName();
    if (factoryMethodName == null || factoryBeanName == null) {
      return null;
    }
    Class<?> factoryType = beanFactory.getType(factoryBeanName);
    if (factoryType != null) {
      if (factoryType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
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
    return null;
  }

  static ConfigurationPropertiesBean forValueObject(Class<?> beanType, String beanName) {
    Bindable<Object> bindTarget = createBindTarget(null, beanType, null);

    if (bindTarget == null || deduceBindMethod(bindTarget) != BindMethod.VALUE_OBJECT) {
      throw new IllegalStateException("Bean '" + beanName + "' is not a @ConfigurationProperties value object");
    }

    return create(beanName, null, bindTarget.withBindMethod(BindMethod.VALUE_OBJECT));
  }

  @Nullable
  private static Bindable<Object> createBindTarget(@Nullable Object bean, Class<?> beanType, @Nullable Method factoryMethod) {
    ResolvableType type = factoryMethod != null
                          ? ResolvableType.forReturnType(factoryMethod)
                          : ResolvableType.forClass(beanType);
    Annotation[] annotations = findAnnotations(bean, beanType, factoryMethod);
    return (annotations != null) ? Bindable.of(type).withAnnotations(annotations) : null;
  }

  @Nullable
  private static Annotation[] findAnnotations(@Nullable Object instance, Class<?> type, @Nullable Method factory) {
    ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
    if (annotation == null) {
      return null;
    }
    Validated validated = findAnnotation(instance, type, factory, Validated.class);
    return (validated != null) ? new Annotation[] { annotation, validated } : new Annotation[] { annotation };
  }

  @Nullable
  private static <A extends Annotation> A findAnnotation(@Nullable Object instance, Class<?> type,
          @Nullable Method factory, Class<A> annotationType) {
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

  @Nullable
  private static ConfigurationPropertiesBean create(String name, @Nullable Object instance, @Nullable Bindable<Object> bindTarget) {
    return bindTarget != null
           ? new ConfigurationPropertiesBean(name, instance, bindTarget)
           : null;
  }

  /**
   * Deduce the {@code BindMethod} that should be used for the given type.
   *
   * @param type the source type
   * @return the bind method to use
   */
  public static BindMethod deduceBindMethod(Class<?> type) {
    return deduceBindMethod(DEFAULT.getBindConstructor(type, false));
  }

  /**
   * Deduce the {@code BindMethod} that should be used for the given {@link Bindable}.
   *
   * @param bindable the source bindable
   * @return the bind method to use
   */
  static BindMethod deduceBindMethod(Bindable<Object> bindable) {
    return deduceBindMethod(DEFAULT.getBindConstructor(bindable, false));
  }

  private static BindMethod deduceBindMethod(@Nullable Constructor<?> bindConstructor) {
    return bindConstructor != null ? BindMethod.VALUE_OBJECT : BindMethod.JAVA_BEAN;
  }

}
