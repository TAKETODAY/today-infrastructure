/*
 * Copyright 2012-present the original author or authors.
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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import infra.aot.AotDetector;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.lang.Contract;
import infra.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 15:21
 */
class DefaultApplicationContextFactory implements ApplicationContextFactory {

  @Override
  public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(@Nullable ApplicationType webApplicationType) {
    return getFromSpringFactories(webApplicationType, ApplicationContextFactory::getEnvironmentType, null);
  }

  @Override
  public @Nullable ConfigurableEnvironment createEnvironment(@Nullable ApplicationType webApplicationType) {
    return getFromSpringFactories(webApplicationType, ApplicationContextFactory::createEnvironment, null);
  }

  @Override
  public @Nullable ConfigurableApplicationContext create(@Nullable ApplicationType webApplicationType) {
    try {
      return getFromSpringFactories(webApplicationType, ApplicationContextFactory::create,
              this::createDefaultApplicationContext);
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable create a default ApplicationContext instance, "
              + "you may need a custom ApplicationContextFactory", ex);
    }
  }

  private ConfigurableApplicationContext createDefaultApplicationContext() {
    if (!AotDetector.useGeneratedArtifacts()) {
      return new AnnotationConfigApplicationContext();
    }
    return new GenericApplicationContext();
  }

  @Contract("_, _, !null -> !null")
  private <T> @Nullable T getFromSpringFactories(@Nullable ApplicationType webApplicationType,
          BiFunction<ApplicationContextFactory, @Nullable ApplicationType, @Nullable T> action,
          @Nullable Supplier<T> defaultResult) {
    for (ApplicationContextFactory candidate : TodayStrategies.find(ApplicationContextFactory.class, getClass().getClassLoader())) {
      T result = action.apply(candidate, webApplicationType);
      if (result != null) {
        return result;
      }
    }
    return defaultResult != null ? defaultResult.get() : null;
  }

}
