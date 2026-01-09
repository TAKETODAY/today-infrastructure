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

package infra.aop.aspectj.annotation;

import org.jspecify.annotations.Nullable;

import infra.aop.aspectj.AspectInstanceFactory;

/**
 * Subinterface of {@link AspectInstanceFactory}
 * that returns {@link AspectMetadata} associated with AspectJ-annotated classes.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 * @since 4.0
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

  /**
   * Return the AspectJ AspectMetadata for this factory's aspect.
   *
   * @return the aspect metadata
   */
  AspectMetadata getAspectMetadata();

  /**
   * Return the best possible creation mutex for this factory.
   *
   * @return the mutex object (may be {@code null} for no mutex to use)
   */
  @Nullable
  Object getAspectCreationMutex();

}
