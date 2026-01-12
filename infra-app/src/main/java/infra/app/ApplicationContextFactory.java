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

import java.util.List;
import java.util.function.Supplier;

import infra.aot.AotDetector;
import infra.beans.BeanUtils;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.lang.TodayStrategies;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.server.context.GenericWebServerApplicationContext;
import infra.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import infra.web.server.reactive.context.ReactiveWebServerApplicationContext;

/**
 * Strategy interface for creating the {@link ConfigurableApplicationContext} used by a
 * {@link Application}. Created contexts should be returned in their default form,
 * with the {@code Application} responsible for configuring and refreshing the
 * context.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/15 14:56
 */
public interface ApplicationContextFactory {

  /**
   * Creates the {@link ConfigurableApplicationContext application context} for a
   * {@link Application}, respecting the given {@code ApplicationType}.
   *
   * @param type the application type
   * @return the newly created application context
   */
  @Nullable
  ConfigurableApplicationContext create(ApplicationType type);

  /**
   * Creates a default {@link ApplicationContextFactory} implementation that will
   * create an appropriate context for the {@link ApplicationType}.
   */
  static ApplicationContextFactory forDefault() {
    return new Default();
  }

  /**
   * Creates an {@code ApplicationContextFactory} that will create contexts by
   * instantiating the given {@code contextClass} via its primary constructor.
   *
   * @param contextClass the context class
   * @return the factory that will instantiate the context class
   * @see BeanUtils#newInstance(Class)
   */
  static ApplicationContextFactory fromClass(Class<? extends ConfigurableApplicationContext> contextClass) {
    return from(() -> BeanUtils.newInstance(contextClass));
  }

  /**
   * Creates an {@code ApplicationContextFactory} that will create contexts by calling
   * the given {@link Supplier}.
   *
   * @param supplier the context supplier, for example
   * {@code AnnotationConfigApplicationContext::new}
   * @return the factory that will instantiate the context class
   */
  static ApplicationContextFactory from(Supplier<ConfigurableApplicationContext> supplier) {
    return applicationType -> supplier.get();
  }

  /**
   * Default ApplicationContextFactory
   */
  class Default implements ApplicationContextFactory {

    @Override
    public ConfigurableApplicationContext create(ApplicationType applicationType) {
      List<ApplicationContextFactory> factories = TodayStrategies.find(ApplicationContextFactory.class);
      for (ApplicationContextFactory factory : factories) {
        ConfigurableApplicationContext context = factory.create(applicationType);
        if (context != null) {
          return context;
        }
      }

      // fallback to defaults
      if (applicationType == ApplicationType.WEB) {
        if (AotDetector.useGeneratedArtifacts()) {
          return new GenericWebServerApplicationContext();
        }
        return new AnnotationConfigWebServerApplicationContext();
      }
      else if (applicationType == ApplicationType.REACTIVE_WEB) {
        if (AotDetector.useGeneratedArtifacts()) {
          return new ReactiveWebServerApplicationContext();
        }
        return new AnnotationConfigReactiveWebServerApplicationContext();
      }
      else {
        if (AotDetector.useGeneratedArtifacts()) {
          return new GenericApplicationContext();
        }
        return new AnnotationConfigApplicationContext();
      }

    }

  }

}
