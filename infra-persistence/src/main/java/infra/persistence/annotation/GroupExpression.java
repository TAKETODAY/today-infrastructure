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
 * Declares the whole non-linear {@code WHERE} structure of an example query as
 * a single boolean expression over its property names.
 *
 * <p>Unlike {@link Group @Group}, which is annotated property-by-property,
 * {@code @GroupExpression} describes the complete grouping in one place, so
 * complex inter-group relationships are easier to express and read:
 *
 * <pre>{@code
 * @EntityRef(UserModel.class)
 * @GroupExpression("a AND b OR (c AND d) AND (s OR b or f)")
 * class UserQuery { ... }
 * }</pre>
 *
 * <p>Every token in the expression is the {@linkplain
 * infra.persistence.EntityProperty#getName() name} of an example property.
 * Tokens are joined with {@code AND} / {@code &&}, {@code OR} / {@code ||} and
 * {@code XOR} (case insensitive for words), negated with unary {@code NOT}, and
 * parenthesized sub-expressions become nested groups. Precedence follows SQL:
 * {@code NOT} binds tighter than {@code AND}, which binds tighter than
 * {@code XOR}, which binds tighter than {@code OR} — so {@code NOT a OR b AND c}
 * means {@code (NOT a) OR (b AND c)}.
 *
 * <p>Only properties named by the expression take part in the query. A property
 * whose value is {@code null} is skipped (its leaf is dropped); when every leaf
 * of a group is dropped the whole group is omitted. A token that does not match
 * any property is rejected. A property that is referenced more than once — for
 * example {@code a AND (b OR a)} — shares the same condition fragment, so both
 * references bind the same value.
 *
 * <p>This annotation is purely structural, exactly like {@link Group @Group}.
 * The shape of each leaf predicate is decided by the other property-level
 * annotations (for example {@link Where @Where} and {@link Trim @Trim}),
 * resolved through the configured
 * {@link infra.persistence.PropertyConditionStrategy strategies}.
 *
 * <p>When {@code @GroupExpression} is present, the property-level
 * {@link Group @Group} / {@link GroupOR @GroupOR} / {@link OR @OR} structure is
 * ignored in favor of the expression.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Group
 * @see Where
 * @see infra.persistence.ConditionGroup
 * @see infra.persistence.ConditionTree
 * @since 5.0
 */
@Reflective
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupExpression {

  /**
   * The boolean expression over property names that describes the WHERE
   * structure.
   *
   * @return the expression
   */
  String value();

}
