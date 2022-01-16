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

package cn.taketoday.framework;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.BeanUtils;
import cn.taketoday.context.ConfigurableApplicationContext;

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
   * A default {@link ApplicationContextFactory} implementation that will create an
   * appropriate context for the {@link ApplicationType}.
   */
  ApplicationContextFactory DEFAULT = (webApplicationType) -> {
    try {
      return switch (webApplicationType) {
        case SERVLET -> new AnnotationConfigServletWebServerApplicationContext();
        case REACTIVE -> new AnnotationConfigReactiveWebServerApplicationContext();
        default -> new AnnotationConfigApplicationContext();
      };
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable create a default ApplicationContext instance, "
              + "you may need a custom ApplicationContextFactory", ex);
    }
  };

  /**
   * Creates the {@link ConfigurableApplicationContext application context} for a
   * {@link Application}, respecting the given {@code webApplicationType}.
   *
   * @param webApplicationType the web application type
   * @return the newly created application context
   */
  ConfigurableApplicationContext create(ApplicationType webApplicationType);

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
   * {@code StandardApplicationContext::new}
   * @return the factory that will instantiate the context class
   */
  static ApplicationContextFactory from(Supplier<ConfigurableApplicationContext> supplier) {
    return (webApplicationType) -> supplier.get();
  }

}
