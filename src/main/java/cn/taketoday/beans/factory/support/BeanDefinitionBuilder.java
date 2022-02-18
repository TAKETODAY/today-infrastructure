/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/10/2 22:45
 * @since 4.0
 */
public class BeanDefinitionBuilder {
  public static final Method[] EMPTY_METHOD = Constant.EMPTY_METHOD_ARRAY;

  /** bean name. */
  private String name;
  /** bean class. */
  private Class<?> beanClass;

  private String beanClassName;
  /** bean scope. */
  private String scope;

  /**
   * Invoke before {@link InitializingBean#afterPropertiesSet}
   *
   * @since 2.3.3
   */
  private String[] initMethods = Constant.EMPTY_STRING_ARRAY;

  /**
   * @since 2.3.3
   */
  private String destroyMethod;

  /**
   * Mark as a {@link FactoryBean}.
   *
   * @since 2.0.0
   */
  private boolean factoryBean = false;

  /** lazy init flag @since 3.0 */
  private boolean lazyInit;

  /** @since 3.0 bean instance supplier */
  private Supplier<?> instanceSupplier;

  /** @since 4.0 */
  private boolean synthetic = false;

  /** @since 4.0 */
  private int role = BeanDefinition.ROLE_APPLICATION;

  /** @since 4.0 */
  private boolean primary = false;

  private String factoryMethod;
  private String factoryBeanName;

  private AnnotationMetadata annotationMetadata;

  private PropertyValues propertyValues;

  public BeanDefinitionBuilder name(String name) {
    this.name = name;
    return this;
  }

  public BeanDefinitionBuilder instanceSupplier(Supplier<?> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
    return this;
  }

  public BeanDefinitionBuilder factoryBean(boolean factoryBean) {
    this.factoryBean = factoryBean;
    return this;
  }

  public BeanDefinitionBuilder lazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
    return this;
  }

  public BeanDefinitionBuilder synthetic(boolean synthetic) {
    this.synthetic = synthetic;
    return this;
  }

  public BeanDefinitionBuilder role(int role) {
    this.role = role;
    return this;
  }

  public BeanDefinitionBuilder primary(boolean primary) {
    this.primary = primary;
    return this;
  }

  public BeanDefinitionBuilder beanClassName(String beanClassName) {
    this.beanClassName = beanClassName;
    return this;
  }

  public BeanDefinitionBuilder annotationMetadata(AnnotationMetadata annotationMetadata) {
    this.annotationMetadata = annotationMetadata;
    return this;
  }

  public BeanDefinitionBuilder beanClass(Class<?> beanClass) {
    this.beanClass = beanClass;
    return this;
  }

  public BeanDefinitionBuilder factoryBeanName(String factoryBeanName) {
    this.factoryBeanName = factoryBeanName;
    return this;
  }

  public BeanDefinitionBuilder factoryMethod(String factoryMethod) {
    this.factoryMethod = factoryMethod;
    return this;
  }

  public BeanDefinitionBuilder scope(String scope) {
    this.scope = scope;
    return this;
  }

  /**
   * set scope 'singleton'
   *
   * @return this
   * @see Scope#SINGLETON
   */
  public BeanDefinitionBuilder singleton() {
    this.scope = Scope.SINGLETON;
    return this;
  }

  /**
   * set scope 'prototype'
   *
   * @return this
   * @see Scope#PROTOTYPE
   */
  public BeanDefinitionBuilder prototype() {
    this.scope = Scope.PROTOTYPE;
    return this;
  }

  public BeanDefinitionBuilder initMethods(String... initMethods) {
    this.initMethods = initMethods;
    return this;
  }

  public BeanDefinitionBuilder destroyMethod(String destroyMethod) {
    this.destroyMethod = destroyMethod;
    return this;
  }

  //

  /**
   * apply scope,initMethods,destroyMethods
   *
   * @param component AnnotationAttributes
   * @see #scope(String)
   * @see #initMethods(String...)
   * @see #destroyMethod(String)
   */
  public BeanDefinitionBuilder attributes(AnnotationAttributes component) {
    if (CollectionUtils.isNotEmpty(component)) {
      this.initMethods = component.getStringArray(BeanDefinition.INIT_METHODS);
      this.destroyMethod = component.getString(BeanDefinition.DESTROY_METHOD);
    }
    return this;
  }

  public BeanDefinitionBuilder annotation(MergedAnnotation<Component> annotation) {
    if (annotation.isPresent()) {
      this.initMethods = annotation.getStringArray(BeanDefinition.INIT_METHODS);
      this.destroyMethod = annotation.getString(BeanDefinition.DESTROY_METHOD);
    }
    return this;
  }

  public BeanDefinitionBuilder propertyValues(PropertyValues propertyValues) {
    this.propertyValues = propertyValues;
    return this;
  }

  // reset

  public void reset() {
    this.role = BeanDefinition.ROLE_APPLICATION;
    this.initMethods = Constant.EMPTY_STRING_ARRAY;
    this.destroyMethod = null;

    this.name = null;
    this.scope = null;
    this.beanClass = null;
    this.lazyInit = false;
    this.factoryMethod = null;
    this.factoryBeanName = null;
    this.instanceSupplier = null;

    this.primary = false;
    this.synthetic = false;
    this.factoryBean = false;

  }

  public void resetAttributes() {
    this.initMethods = Constant.EMPTY_STRING_ARRAY;
    this.destroyMethod = null;
  }

  // getter

  //---------------------------------------------------------------------
  // build
  //---------------------------------------------------------------------

  @NonNull
  private BeanDefinition create() {

    if (annotationMetadata != null) {
      AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(annotationMetadata);
      MergedAnnotation<Component> annotation = annotationMetadata.getAnnotation(Component.class);
      annotation(annotation);
      if (beanClass != null) {
        definition.setBeanClass(beanClass);
      }
      return definition;
    }
    if (beanClass != null) {
      AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(beanClass);
      MergedAnnotation<Component> annotation = definition.getMetadata().getAnnotation(Component.class);
      annotation(annotation);
      return definition;
    }
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClassName(beanClassName);
    if (name == null) {
      name = defaultBeanName(beanClassName);
    }
    return definition;
  }

  public BeanDefinition build() {
    return build(create());
  }

  public BeanDefinition build(BeanDefinition definition) {
    definition.setBeanName(name);
    definition.setRole(role);
    definition.setScope(scope);
    definition.setPrimary(primary);
    definition.setLazyInit(lazyInit);
    definition.setSynthetic(synthetic);
    definition.setInitMethods(initMethods);
    definition.setFactoryBean(factoryBean);
    definition.setDestroyMethod(destroyMethod);
    definition.setInstanceSupplier(instanceSupplier);

    definition.setFactoryMethodName(factoryMethod);
    definition.setFactoryBeanName(factoryBeanName);
    definition.setPropertyValues(propertyValues);
    return definition;
  }

  // BiConsumer

  public void build(
          String defaultName,
          AnnotationAttributes component,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    build(defaultName, new AnnotationAttributes[] { component }, consumer);
  }

  public void build(
          String defaultName,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer,
          AnnotationAttributes... components) {
    build(defaultName, components, consumer);
  }

  public void build(
          String defaultName, @Nullable AnnotationAttributes[] components,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    if (ObjectUtils.isEmpty(components)) {
      name(defaultName);
      BeanDefinition definition = build();
      consumer.accept(null, definition);
    }
    else {
      for (AnnotationAttributes component : components) {
        attributes(component);
        for (String name : determineName(defaultName, component.getStringArray(MergedAnnotation.VALUE))) {
          name(name);
          BeanDefinition definition = build();
          consumer.accept(component, definition);
        }
      }
    }
  }

  //

  public void build(
          String defaultName, AnnotatedElement annotated,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    AnnotationAttributes[] components = AnnotatedElementUtils.getMergedAttributesArray(annotated, Component.class);
    build(defaultName, components, consumer);
  }

  // Consumer

  public void build(
          String defaultName,
          AnnotationAttributes component,
          Consumer<BeanDefinition> consumer) {
    build(defaultName, new AnnotationAttributes[] { component }, consumer);
  }

  public void build(
          String defaultName,
          Consumer<BeanDefinition> consumer,
          AnnotationAttributes... components) {
    build(defaultName, components, consumer);
  }

  public void build(
          String defaultName,
          @Nullable AnnotationAttributes[] components,
          Consumer<BeanDefinition> consumer) {
    build(defaultName, components, (attributes, definition) -> consumer.accept(definition));
  }

  public void build(
          String defaultName, AnnotatedElement annotated, Consumer<BeanDefinition> consumer) {
    AnnotationAttributes[] components = AnnotatedElementUtils.getMergedAttributesArray(annotated, Component.class);
    build(defaultName, components, consumer);
  }

  public void build(
          String defaultName, MergedAnnotations annotated, Consumer<BeanDefinition> consumer) {
    AnnotationAttributes[] components = annotated.getAttributes(Component.class);
    build(defaultName, components, consumer);
  }

  //---------------------------------------------------------------------
  // static utils
  //---------------------------------------------------------------------

  /**
   * Find bean names
   *
   * @param defaultName Default bean name
   * @param names Annotation values
   * @return Bean names
   */
  public static String[] determineName(String defaultName, String... names) {
    if (ObjectUtils.isEmpty(names)) {
      return new String[] { defaultName }; // default name
    }
    HashSet<String> hashSet = new HashSet<>();
    CollectionUtils.addAll(hashSet, names);
    hashSet.remove(Constant.BLANK);
    if (hashSet.isEmpty()) {
      return new String[] { defaultName }; // default name
    }
    return names;
  }

  /**
   * search init methods
   * <p>
   * if {@code initMethods} has overload method select Minimal parameter method
   *
   * @param beanClass Bean class
   * @param initMethods Init Method name
   * @throws IllegalArgumentException init method not found
   * @since 2.1.7
   */
  public static Method[] computeInitMethod(@Nullable String[] initMethods, Class<?> beanClass) {
    if (ObjectUtils.isNotEmpty(initMethods)) {
      // @since 4.0 use ReflectionUtils.findMethodWithMinimalParameters
      ArrayList<Method> methods = new ArrayList<>(2);
      for (String initMethod : initMethods) {
        Method method = ReflectionUtils.findMethodWithMinimalParameters(beanClass, initMethod);
        if (method == null) {
          throw new IllegalArgumentException("bean init-method: '" + initMethod + "' not found in '" + beanClass.getName() + "'");
        }
        methods.add(method);
      }
      if (methods.isEmpty()) {
        return EMPTY_METHOD;
      }
      AnnotationAwareOrderComparator.sort(methods);
      return methods.toArray(EMPTY_METHOD);
    }
    else {
      return EMPTY_METHOD;
    }
  }

  public static BeanDefinition empty() {
    return new BeanDefinition();
  }

  public static BeanDefinition defaults(Class<?> candidate) {
    Assert.notNull(candidate, "bean-class must not be null");
    String defaultBeanName = defaultBeanName(candidate);
    return defaults(defaultBeanName, candidate, null);
  }

  public static BeanDefinition defaults(String name, Class<?> beanClass) {
    return defaults(name, beanClass, null);
  }

  public static BeanDefinition defaults(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(beanClass, "bean-class must not be null");
    AnnotatedBeanDefinition definition = new AnnotatedBeanDefinition(beanClass);
    definition.setBeanName(name);
    if (attributes != null) {
      definition.setDestroyMethod(attributes.getString(BeanDefinition.DESTROY_METHOD));
      definition.setInitMethods(attributes.getStringArray(BeanDefinition.INIT_METHODS));
    }
    return definition;
  }

  public static String defaultBeanName(String className) {
    return StringUtils.uncapitalize(ClassUtils.getSimpleName(className));
  }

  public static String defaultBeanName(Class<?> clazz) {
    String simpleName = clazz.getSimpleName();
    return StringUtils.uncapitalize(simpleName);
  }

  public static BeanDefinitionBuilder from(Class<?> beanClass) {
    return new BeanDefinitionBuilder().beanClass(beanClass);
  }

  public static BeanDefinitionBuilder from(String name, Class<?> beanClass) {
    return new BeanDefinitionBuilder()
            .name(name)
            .beanClass(beanClass);
  }

}
