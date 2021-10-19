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

package cn.taketoday.context.loader;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2021/10/19 22:22
 * @since 4.0
 */
public class DefinitionLoadingContext {

  private final BeanDefinitionRegistry registry;

  private ConditionEvaluator conditionEvaluator;

  private final ApplicationContext applicationContext;

  private final MissingBeanRegistry missingBeanRegistry;

  public DefinitionLoadingContext(BeanDefinitionRegistry registry, ApplicationContext context) {
    this.registry = registry;
    this.applicationContext = context;
    this.missingBeanRegistry = new MissingBeanRegistry(this);
  }

  public DefinitionLoadingContext(
          BeanDefinitionRegistry registry, @Nullable ConditionEvaluator conditionEvaluator, ApplicationContext context) {
    this.registry = registry;
    this.applicationContext = context;
    this.conditionEvaluator = conditionEvaluator;
    this.missingBeanRegistry = new MissingBeanRegistry(this);
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public MissingBeanRegistry getMissingBeanRegistry() {
    return missingBeanRegistry;
  }

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type type
   * @return bean name
   * @see ClassUtils#getShortName(Class)
   */
  public String createBeanName(Class<?> type) {
    return BeanDefinitionBuilder.defaultBeanName(type);
  }

  public String createBeanName(String clazzName) {
    return BeanDefinitionBuilder.defaultBeanName(clazzName);
  }

  @NonNull
  public ConditionEvaluator getConditionEvaluator() {
    if (conditionEvaluator == null) {
      this.conditionEvaluator = new ConditionEvaluator(applicationContext, registry);
    }
    return conditionEvaluator;
  }

  public BeanDefinitionBuilder createBuilder() {
    return new BeanDefinitionBuilder(applicationContext);
  }

  public void registerBeanDefinition(BeanDefinition def) {
    registry.registerBeanDefinition(def);
  }

  public boolean containsBeanDefinition(Class<?> beanClass) {
    return registry.containsBeanDefinition(beanClass);
  }

  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    return registry.containsBeanDefinition(type, equals);
  }

  public boolean containsBeanDefinition(String beanName) {
    return registry.containsBeanDefinition(beanName);
  }

  public boolean passCondition(AnnotatedElement annotated) {
    return conditionEvaluator.passCondition(annotated);
  }

  public void addApplicationListener(ApplicationListener<?> importer) {
    applicationContext.addApplicationListener(importer);
  }

  public Object getBean(BeanDefinition def) {
    return applicationContext.getBean(def);
  }

  public void detectMissingBean(Method method) {
    missingBeanRegistry.detectMissingBean(method);
  }

  public boolean isMissingBeanInContext(AnnotationAttributes missingBean, AnnotatedElement annotated) {
    return missingBeanRegistry.isMissingBeanInContext(missingBean, annotated);
  }

}
