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
 * Declares a whole SQL ORDER BY fragment on an entity class, applied as-is when
 * the entity is queried by example.
 *
 * <p>The clause is a raw SQL fragment (e.g. {@code "name ASC, age DESC"} or
 * {@code "CASE WHEN status='active' THEN 1 ELSE 2 END"}) and is rendered without
 * validation or dialect quoting. It overrides any property-level
 * {@link OrderBy @OrderBy} ordering, while an
 * {@link infra.persistence.sql.OrderSpecSource} implemented by the example object
 * takes precedence over it.
 *
 * <p>For ordering by individual mapped properties, use {@link OrderBy @OrderBy}
 * on the properties instead.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see OrderBy
 * @see infra.persistence.sql.OrderSpecSource
 * @since 5.0 2026/9/18 22:08
 */
@Reflective
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderByClause {

  /**
   * The raw SQL ORDER BY fragment.
   */
  String value();
}
