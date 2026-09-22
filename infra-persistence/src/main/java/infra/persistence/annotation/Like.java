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
import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Indicates that a field, method, or class should be treated as a "like" query condition.
 * It is used in conjunction with query builders or ORM frameworks to generate SQL-like
 * conditions dynamically.
 *
 * <p>The annotation can specify the target column. If no column is given, the mapped
 * column of the property is used. The annotated value can be trimmed before processing
 * by combining it with {@link Trim @Trim}.</p>
 *
 * <p>Like matching — prefix, suffix, or substring — is selected by combining this
 * annotation with {@link PrefixLike @PrefixLike} or {@link SuffixLike @SuffixLike}.</p>
 *
 * <p>Usage on a field:
 * <pre>{@code
 * @Like
 * private String name;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PrefixLike
 * @see SuffixLike
 * @since 4.0 2024/2/28 22:48
 */
@Column
@Reflective
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Like {

  /**
   * The column that the between operand targets.
   *
   * <p>An alias for {@link Column#value()}. When blank, the property's mapped
   * column is used instead.
   *
   * @return the column name, or {@link Constant#BLANK} if not specified
   */
  @AliasFor(annotation = Column.class, attribute = "value")
  String value() default Constant.BLANK;

}
