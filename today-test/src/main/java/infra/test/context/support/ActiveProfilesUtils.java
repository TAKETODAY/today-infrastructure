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

package infra.test.context.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.context.annotation.Profile;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ActiveProfiles;
import infra.test.context.ActiveProfilesResolver;
import infra.test.context.TestContextAnnotationUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

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
   * @see Profile
   */
  static String[] resolveActiveProfiles(Class<?> testClass) {
    Assert.notNull(testClass, "Class is required");

    TestContextAnnotationUtils.AnnotationDescriptor<ActiveProfiles> descriptor = TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ActiveProfiles.class);
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
