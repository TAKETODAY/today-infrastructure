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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;

/**
 * Marks an insert-only creator property. The auditor or selected property value
 * is assigned directly and must be compatible with the target property type.
 * An unavailable auditor leaves the existing value unchanged.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AuditorAware
 * @see AuditingEntityListener
 * @since 5.0
 */
@Documented
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedBy {

  /**
   * Property path to read from the current auditor, for example {@code "id"}
   * or {@code "name"}. An empty value uses the auditor itself.
   */
  String value() default "";

}
