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

import infra.beans.factory.BeanFactory;
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
 * Context for condition evaluation, providing access to the {@link BeanDefinitionRegistry},
 * {@link Environment}, {@link ResourceLoader}, and other relevant infrastructure components.
 * <p>This context is used by {@link Condition} implementations to determine whether a bean
 * should be registered based on the current application state.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/1 21:13
 */
public class ConditionContext {

  private final Environment environment;

  private final ResourceLoader resourceLoader;

  private final @Nullable ClassLoader classLoader;

  private final @Nullable BeanDefinitionRegistry registry;

  private final @Nullable ConfigurableBeanFactory beanFactory;

  private @Nullable ConditionEvaluationReport conditionEvaluationReport;

  public ConditionContext(@Nullable BeanDefinitionRegistry registry,
          @Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {

    this.registry = registry;
    this.beanFactory = deduceBeanFactory(registry);
    this.environment = environment != null ? environment : deduceEnvironment(registry);
    this.resourceLoader = resourceLoader != null ? resourceLoader : deduceResourceLoader(registry);
    this.classLoader = deduceClassLoader(this.resourceLoader, this.beanFactory);
  }

  private static @Nullable ConfigurableBeanFactory deduceBeanFactory(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof ConfigurableBeanFactory) {
      return (ConfigurableBeanFactory) source;
    }
    if (source instanceof ConfigurableApplicationContext) {
      return (((ConfigurableApplicationContext) source).getBeanFactory());
    }
    return null;
  }

  private static Environment deduceEnvironment(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof EnvironmentCapable) {
      return ((EnvironmentCapable) source).getEnvironment();
    }
    return new StandardEnvironment();
  }

  private static ResourceLoader deduceResourceLoader(@Nullable BeanDefinitionRegistry source) {
    if (source instanceof ResourceLoader) {
      return (ResourceLoader) source;
    }
    return new DefaultResourceLoader();
  }

  private static @Nullable ClassLoader deduceClassLoader(@Nullable ResourceLoader resourceLoader, @Nullable ConfigurableBeanFactory beanFactory) {
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

  /**
   * Return the {@link BeanDefinitionRegistry} that will hold the bean definition
   * should the condition match.
   *
   * @throws IllegalStateException if no registry is available (which is unusual:
   * only the case with a plain {@link ClassPathScanningCandidateComponentProvider})
   */
  public BeanDefinitionRegistry getRegistry() {
    Assert.state(this.registry != null, "No BeanDefinitionRegistry available");
    return this.registry;
  }

  /**
   * Return the {@link ConfigurableBeanFactory} that will hold the bean
   * definition should the condition match, or {@code null} if the bean factory is
   * not available (or not downcastable to {@code ConfigurableBeanFactory}).
   */
  public @Nullable ConfigurableBeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  public ConfigurableBeanFactory getRequiredBeanFactory() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory != null, "No BeanFactory available");
    return beanFactory;
  }

  /**
   * Return the {@link Environment} for which the current application is running.
   */
  public Environment getEnvironment() {
    return this.environment;
  }

  /**
   * Return the {@link ResourceLoader} currently being used.
   */
  public ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  /**
   * Return the {@link ClassLoader} that should be used to load additional classes
   * (only {@code null} if even the system ClassLoader isn't accessible).
   *
   * @see infra.util.ClassUtils#forName(String, ClassLoader)
   */
  public @Nullable ClassLoader getClassLoader() {
    return this.classLoader;
  }

  /**
   * Return the {@link ConditionEvaluationReport} for this context, creating it if necessary.
   * <p>If no report is available and a {@link BeanFactory} is present, a new report will be
   * obtained from the bean factory. Otherwise, {@code null} is returned.
   *
   * @return the condition evaluation report, or {@code null} if not available
   */
  public @Nullable ConditionEvaluationReport getEvaluationReport() {
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
