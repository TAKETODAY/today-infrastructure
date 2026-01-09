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

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ActiveProfiles;
import infra.test.context.ActiveProfilesResolver;
import infra.test.context.TestContextAnnotationUtils;

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
    Assert.notNull(testClass, "Class is required");
    TestContextAnnotationUtils.AnnotationDescriptor<ActiveProfiles> descriptor = TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ActiveProfiles.class);

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
