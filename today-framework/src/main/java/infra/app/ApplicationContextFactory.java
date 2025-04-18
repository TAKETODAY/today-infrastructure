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

package infra.app;

import java.util.List;
import java.util.function.Supplier;

import infra.aot.AotDetector;
import infra.beans.BeanUtils;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.lang.Nullable;
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
      if (applicationType == ApplicationType.NETTY_WEB) {
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
