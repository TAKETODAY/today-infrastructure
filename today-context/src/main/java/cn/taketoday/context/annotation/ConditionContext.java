/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.condition.ConditionEvaluationReport;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * For ConditionEvaluator Evaluation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/1 21:13
 */
public class ConditionContext {

  @Nullable
  private final BeanDefinitionRegistry registry;

  @Nullable
  private final ConfigurableBeanFactory beanFactory;

  private final Environment environment;

  private final ResourceLoader resourceLoader;

  @Nullable
  private final ClassLoader classLoader;

  @Nullable
  private ConditionEvaluationReport conditionEvaluationReport;

  public ConditionContext(ApplicationContext context, @Nullable BeanDefinitionRegistry registry) {
    this.registry = registry;
    this.resourceLoader = context;
    this.environment = context.getEnvironment();
    this.beanFactory = deduceBeanFactory(registry);
    this.classLoader = deduceClassLoader(resourceLoader, this.beanFactory);
  }

  public ConditionContext(@Nullable BeanDefinitionRegistry registry,
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
  private ClassLoader deduceClassLoader(@Nullable ResourceLoader resourceLoader, @Nullable ConfigurableBeanFactory beanFactory) {
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

  public ConfigurableBeanFactory getRequiredBeanFactory() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory != null, "No BeanFactory available");
    return beanFactory;
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

  @Nullable
  public ConditionEvaluationReport getEvaluationReport() {
    if (conditionEvaluationReport == null) {
      if (beanFactory != null) {
        conditionEvaluationReport = ConditionEvaluationReport.get(beanFactory);
      }
    }
    return conditionEvaluationReport;
  }

  void close() {
    conditionEvaluationReport = null;
  }

}
