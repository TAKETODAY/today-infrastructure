/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;

/**
 * Utility methods for working with
 * {@link ApplicationContextInitializer ApplicationContextInitializers}.
 *
 * <p>Although {@code ApplicationContextInitializerUtils} was first introduced
 * in Spring Framework 4.1, the initial implementations of methods in this class
 * were based on the existing code base in {@code ContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @see ContextConfiguration#initializers
 * @since 4.0
 */
abstract class ApplicationContextInitializerUtils {

  private static final Log logger = LogFactory.getLog(ApplicationContextInitializerUtils.class);

  /**
   * Resolve the set of merged {@code ApplicationContextInitializer} classes for the
   * supplied list of {@code ContextConfigurationAttributes}.
   * <p>Note that the {@link ContextConfiguration#inheritInitializers inheritInitializers}
   * flag of {@link ContextConfiguration @ContextConfiguration} will be taken into
   * consideration. Specifically, if the {@code inheritInitializers} flag is set to
   * {@code true} for a given level in the class hierarchy represented by the provided
   * configuration attributes, context initializer classes defined at the given level
   * will be merged with those defined in higher levels of the class hierarchy.
   *
   * @param configAttributesList the list of configuration attributes to process; must
   * not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
   * (i.e., as if we were traversing up the class hierarchy)
   * @return the set of merged context initializer classes, including those from
   * superclasses if appropriate (never {@code null})
   * @since 4.0
   */
  static Set<Class<? extends ApplicationContextInitializer>> resolveInitializerClasses(
          List<ContextConfigurationAttributes> configAttributesList) {

    Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes List must not be empty");
    Set<Class<? extends ApplicationContextInitializer>> initializerClasses = new LinkedHashSet<>();

    for (ContextConfigurationAttributes configAttributes : configAttributesList) {
      if (logger.isTraceEnabled()) {
        logger.trace("Processing context initializers for configuration attributes " + configAttributes);
      }
      Collections.addAll(initializerClasses, configAttributes.getInitializers());
      if (!configAttributes.isInheritInitializers()) {
        break;
      }
    }

    return initializerClasses;
  }

}
