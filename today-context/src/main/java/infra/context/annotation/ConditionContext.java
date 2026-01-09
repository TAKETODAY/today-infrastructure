/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ConfigurableApplicationContext;
import infra.context.condition.ConditionEvaluationReport;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.env.StandardEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.util.ClassUtils;

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

  public ConditionContext(@Nullable BeanDefinitionRegistry registry,
          @Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {

    this.registry = registry;
    this.beanFactory = deduceBeanFactory(registry);
    this.environment = environment != null ? environment : deduceEnvironment(registry);
    this.resourceLoader = resourceLoader != null ? resourceLoader : deduceResourceLoader(registry);
    this.classLoader = deduceClassLoader(this.resourceLoader, this.beanFactory);
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
