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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * for ConditionEvaluator Evaluation
 *
 * @author TODAY 2021/10/1 21:13
 * @since 4.0
 */
public class ConditionEvaluationContext {

  @Nullable
  private final BeanDefinitionRegistry registry;

  @Nullable
  private final ConfigurableBeanFactory beanFactory;

  private final Environment environment;

  private final ResourceLoader resourceLoader;

  @Nullable
  private final ClassLoader classLoader;

  private ExpressionEvaluator evaluator;

  public ConditionEvaluationContext(ApplicationContext context, BeanDefinitionRegistry registry) {
    this.registry = registry;
    this.resourceLoader = context;
    this.environment = context.getEnvironment();
    this.beanFactory = deduceBeanFactory(registry);
    this.classLoader = deduceClassLoader(resourceLoader, this.beanFactory);
  }

  public ConditionEvaluationContext(
          @Nullable BeanDefinitionRegistry registry,
          @Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {

    this.registry = registry;
    this.beanFactory = deduceBeanFactory(registry);
    this.environment = environment != null ? environment : deduceEnvironment(registry);
    this.resourceLoader = resourceLoader != null ? resourceLoader : deduceResourceLoader(registry);
    this.classLoader = deduceClassLoader(resourceLoader, this.beanFactory);
  }

  @Nullable
  private ConfigurableBeanFactory deduceBeanFactory(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof ConfigurableBeanFactory) {
      return (ConfigurableBeanFactory) source;
    }
    if (source instanceof ConfigurableApplicationContext) {
      return (((ConfigurableApplicationContext) source).getBeanFactory());
    }
    return null;
  }

  private Environment deduceEnvironment(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof EnvironmentCapable) {
      return ((EnvironmentCapable) source).getEnvironment();
    }
    return new StandardEnvironment();
  }

  private ResourceLoader deduceResourceLoader(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof ResourceLoader) {
      return (ResourceLoader) source;
    }
    return new DefaultResourceLoader();
  }

  @Nullable
  private ClassLoader deduceClassLoader(
          @Nullable ResourceLoader resourceLoader,
          @Nullable ConfigurableBeanFactory beanFactory) {

    if (resourceLoader != null) {
      ClassLoader classLoader = resourceLoader.getClassLoader();
      if (classLoader != null) {
        return classLoader;
      }
    }
    if (beanFactory != null) {
      return beanFactory.getBeanClassLoader();
    }
    return ClassUtils.getDefaultClassLoader();
  }

  public BeanDefinitionRegistry getRegistry() {
    Assert.state(this.registry != null, "No BeanDefinitionRegistry available");
    return this.registry;
  }

  @Nullable
  public ConfigurableBeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  public Environment getEnvironment() {
    return this.environment;
  }

  public ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  @Nullable
  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public <T> T evaluateExpression(String expression, Class<T> booleanClass) {
    if (evaluator == null) {
      if (resourceLoader instanceof ApplicationContext context) {
        this.evaluator = context.getExpressionEvaluator();
      }
      else if (registry instanceof ApplicationContext context) {
        this.evaluator = context.getExpressionEvaluator();
      }
      else {
        evaluator = new ExpressionEvaluator();
      }
    }
    return evaluator.evaluate(expression, booleanClass); // TODO
  }

}
