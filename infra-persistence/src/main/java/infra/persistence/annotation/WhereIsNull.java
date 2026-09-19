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

package infra.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;

/**
 * Indicates that a {@code null} value of the annotated entity property should
 * contribute an {@code IS NULL} (or {@code IS NOT NULL}) predicate to an example
 * query, instead of being ignored.
 *
 * <p>By default a {@code null} property value takes no part in an example query.
 * Annotating the property with {@code @WhereIsNull} opts it into nullness matching
 * on its own column: {@code @WhereIsNull} renders {@code column IS NULL}, while
 * {@code @WhereIsNull(not = true)} renders {@code column IS NOT NULL}.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 *   // WHERE deleted_at IS NULL
 *   @WhereIsNull
 *   private Instant deletedAt;
 *
 *   // WHERE status IS NOT NULL
 *   @WhereIsNull(not = true)
 *   private Integer status;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@Reflective
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface WhereIsNull {

  /**
   * Whether to negate the nullness match. When {@code true} the query renders
   * {@code IS NOT NULL}; otherwise it renders {@code IS NULL}.
   *
   * @return {@code true} to match non-null values, {@code false} to match null values
   */
  boolean not() default false;

}