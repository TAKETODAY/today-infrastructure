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
import infra.persistence.Order;

/**
 * Specifies the sort direction of an entity property when it takes part in the
 * ORDER BY clause of an example query.
 *
 * <p>Place on a mapped property (field or getter) to make example queries order
 * results by that property, using the declared direction. When several properties
 * carry the annotation, they are applied in declaration order, so an earlier
 * property takes precedence over later ones.
 *
 * <p>To declare a whole SQL ORDER BY fragment at the class level, use
 * {@link OrderByClause @OrderByClause} instead. Both annotations are independent;
 * a class-level clause overrides any property-level {@code @OrderBy}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OrderByClause
 * @since 4.0 2024/3/31 17:21
 */
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderBy {

  /**
   * The sort direction of the annotated property.
   */
  Order value() default Order.ASC;
}
