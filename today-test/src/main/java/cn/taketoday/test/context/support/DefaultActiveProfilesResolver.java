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

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ActiveProfilesResolver;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;

import static cn.taketoday.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;

/**
 * Default implementation of the {@link ActiveProfilesResolver} strategy that
 * resolves <em>active bean definition profiles</em> based solely on profiles
 * configured declaratively via {@link ActiveProfiles#profiles} or
 * {@link ActiveProfiles#value}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 * @since 4.0
 */
public class DefaultActiveProfilesResolver implements ActiveProfilesResolver {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private static final Logger log = LoggerFactory.getLogger(DefaultActiveProfilesResolver.class);

  /**
   * Resolve the <em>bean definition profiles</em> for the given {@linkplain
   * Class test class} based on profiles configured declaratively via
   * {@link ActiveProfiles#profiles} or {@link ActiveProfiles#value}.
   *
   * @param testClass the test class for which the profiles should be resolved;
   * never {@code null}
   * @return the array of bean definition profiles to use when loading the
   * {@code ApplicationContext}; never {@code null}
   */
  @Override
  public String[] resolve(Class<?> testClass) {
    Assert.notNull(testClass, "Class must not be null");
    AnnotationDescriptor<ActiveProfiles> descriptor = findAnnotationDescriptor(testClass, ActiveProfiles.class);

    if (descriptor == null) {
      if (log.isDebugEnabled()) {
        log.debug("Could not find an 'annotation declaring class' for annotation type [{}] and class [{}]",
                ActiveProfiles.class.getName(), testClass.getName());
      }
      return EMPTY_STRING_ARRAY;
    }
    else {
      ActiveProfiles annotation = descriptor.getAnnotation();
      if (log.isTraceEnabled()) {
        log.trace("Retrieved @ActiveProfiles [{}] for declaring class [{}].", annotation,
                descriptor.getDeclaringClass().getName());
      }
      return annotation.profiles();
    }
  }

}
