/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence.auditing;

import org.jspecify.annotations.Nullable;

/**
 * Provides the current user or system identity for entity auditing.
 *
 * @param <T> the auditor type; the auditor or selected property values must be
 * compatible with the annotated entity properties
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface AuditorAware<T> {

  /**
   * Return the current auditor, or {@code null} when unavailable.
   */
  @Nullable T getCurrentAuditor();

}
