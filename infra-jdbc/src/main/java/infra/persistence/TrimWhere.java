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

package infra.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * trim string property
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see String#trim()
 * @since 4.0 2024/2/24 22:45
 */
@Where
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimWhere {

  /**
   * The where-clause predicate.
   */
  @AliasFor(annotation = Where.class, attribute = "value")
  String value() default Constant.DEFAULT_NONE;

  @AliasFor(annotation = Where.class, attribute = "operator")
  String operator() default Constant.DEFAULT_NONE;

}
