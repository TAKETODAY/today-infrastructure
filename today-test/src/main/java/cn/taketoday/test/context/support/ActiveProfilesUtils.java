/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ActiveProfilesResolver;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;

/**
 * Utility methods for working with {@link ActiveProfiles @ActiveProfiles} and
 * {@link ActiveProfilesResolver ActiveProfilesResolvers}.
 *
 * <p>Although {@code ActiveProfilesUtils} was first introduced in Infra
 * 4.1, the initial implementations of methods in this class were based on the
 * existing code base in {@code ContextLoaderUtils}.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 * @since 4.0
 */
abstract class ActiveProfilesUtils {

  private static final Logger log = LoggerFactory.getLogger(ActiveProfilesUtils.class);

  private static final DefaultActiveProfilesResolver defaultActiveProfilesResolver = new DefaultActiveProfilesResolver();

  /**
   * Resolve <em>active bean definition profiles</em> for the supplied {@link Class}.
   * <p>Note that the {@link ActiveProfiles#inheritProfiles inheritProfiles} flag of
   * {@link ActiveProfiles @ActiveProfiles} will be taken into consideration.
   * Specifically, if the {@code inheritProfiles} flag is set to {@code true}, profiles
   * defined in the test class will be merged with those defined in superclasses.
   *
   * @param testClass the class for which to resolve the active profiles (must not be
   * {@code null})
   * @return the set of active profiles for the specified class, including active
   * profiles from superclasses if appropriate (never {@code null})
   * @see ActiveProfiles
   * @see ActiveProfilesResolver
   * @see cn.taketoday.context.annotation.Profile
   */
  static String[] resolveActiveProfiles(Class<?> testClass) {
    Assert.notNull(testClass, "Class must not be null");

    AnnotationDescriptor<ActiveProfiles> descriptor = findAnnotationDescriptor(testClass, ActiveProfiles.class);
    List<String[]> profileArrays = new ArrayList<>();

    if (descriptor == null && log.isDebugEnabled()) {
      log.debug("Could not find an 'annotation declaring class' for annotation type [{}] and class [{}]",
              ActiveProfiles.class.getName(), testClass.getName());
    }

    while (descriptor != null) {
      Class<?> rootDeclaringClass = descriptor.getRootDeclaringClass();
      ActiveProfiles annotation = descriptor.getAnnotation();

      if (log.isTraceEnabled()) {
        log.trace("Retrieved @ActiveProfiles [{}] for declaring class [{}]",
                annotation, descriptor.getDeclaringClass().getName());
      }

      ActiveProfilesResolver resolver;
      Class<? extends ActiveProfilesResolver> resolverClass = annotation.resolver();
      if (ActiveProfilesResolver.class == resolverClass) {
        resolver = defaultActiveProfilesResolver;
      }
      else {
        try {
          resolver = BeanUtils.newInstance(resolverClass);
        }
        catch (Exception ex) {
          String msg = String.format("Could not instantiate ActiveProfilesResolver of type [%s] " +
                  "for test class [%s]", resolverClass.getName(), rootDeclaringClass.getName());
          log.error(msg);
          throw new IllegalStateException(msg, ex);
        }
      }

      String[] profiles = resolver.resolve(rootDeclaringClass);
      if (ObjectUtils.isNotEmpty(profiles)) {
        // Prepend to the list so that we can later traverse "down" the hierarchy
        // to ensure that we retain the top-down profile registration order
        // within a test class hierarchy.
        profileArrays.add(0, profiles);
      }

      descriptor = (annotation.inheritProfiles() ? descriptor.next() : null);
    }

    Set<String> activeProfiles = new LinkedHashSet<>();
    for (String[] profiles : profileArrays) {
      for (String profile : profiles) {
        if (StringUtils.hasText(profile)) {
          activeProfiles.add(profile.trim());
        }
      }
    }

    return StringUtils.toStringArray(activeProfiles);
  }

}
