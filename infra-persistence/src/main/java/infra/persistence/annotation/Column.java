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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Specifies the mapped column for a persistent property or field.
 *
 * <p>When the annotation is absent, the mapped column defaults to the property
 * or field name. The column can be given through either {@link #name()} or its
 * alias {@link #value()}.
 *
 * <p>Usage on a field:
 * <pre>{@code
 * @Column(name = "DESC")
 * private String description;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/1/27 22:32
 */
@Reflective
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface Column {

  /**
   * The name of the column.
   *
   * <p>An alias for {@link #name()}. When set to {@link Constant#BLANK}, the
   * mapped column defaults to the property or field name.
   *
   * @return the column name, or {@link Constant#BLANK} if not specified
   */
  @AliasFor("name")
  String value() default Constant.BLANK;

  /**
   * The name of the column.
   *
   * <p>An alias for {@link #value()}. When set to {@link Constant#BLANK}, the
   * mapped column defaults to the property or field name.
   *
   * @return the column name, or {@link Constant#BLANK} if not specified
   */
  @AliasFor("value")
  String name() default Constant.BLANK;

}
