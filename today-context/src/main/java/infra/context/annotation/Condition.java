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

import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order
 * for a component to be registered.
 *
 * <p>Conditions are checked immediately before the bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Conditional
 * @since 2018-11-10 13:44
 */
@FunctionalInterface
public interface Condition {

  /**
   * Determine if the condition matches.
   *
   * @param context ConditionContext
   * @param metadata the metadata of the {@link AnnotationMetadata class}
   * * or {@link MethodMetadata method} being checked
   * @return Return {@code false} to indicate that the bean should not be
   * * registered
   * @since 4.0
   */
  boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
