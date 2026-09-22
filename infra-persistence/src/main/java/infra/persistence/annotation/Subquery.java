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
 * Generates a {@code targetId IN (SELECT select FROM junctionTable WHERE fieldColumn = ?)}
 * or {@code targetId NOT IN (SELECT ...)} predicate for a many-to-many relationship through a
 * junction table.
 *
 * <p>Used on a field of an {@link EntityRef @EntityRef} query class. The generated
 * predicate targets the referenced entity's ID column:
 *
 * <pre>{@code
 * @EntityRef(Label.class)
 * class TagQuery {
 *   @Subquery(table = "article_label", select = "label_id")
 *   public final Long articleId;
 * }
 * }</pre>
 *
 * <p>The junction table name can be specified directly via {@link #table()}, or
 * resolved from an entity class via {@link #entity()}:
 *
 * <pre>{@code
 * @EntityRef(Label.class)
 * class TagQuery {
 *   @Subquery(entity = ArticleLabel.class, select = "label_id")
 *   public final Long articleId;
 * }
 * }</pre>
 *
 * <p>The {@code WHERE} column in the junction table is resolved automatically
 * from the annotated field's mapped column name.
 *
 * <p>Given {@code Label} has {@code @Id Long id} mapped to column {@code id},
 * this generates:
 * <pre>{@code
 * id IN (SELECT label_id FROM article_label WHERE article_id = ?)
 * }</pre>
 *
 * <p>Set {@link #negative()} to {@code true} to generate {@code NOT IN} instead.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Column
@Reflective
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subquery {

  /**
   * The junction (many-to-many) table name.
   *
   * <p>Ignored when {@link #entity()} is set to a non-default value; the table
   * name is then resolved from that entity's {@link Table @Table} annotation.
   *
   * @return the junction table name
   */
  String table() default "";

  /**
   * An entity class whose {@link Table @Table} provides the junction table name.
   *
   * <p>When set to a non-default value, the table name is resolved from this
   * entity's {@code @Table} annotation, and {@link #table()} is ignored.
   *
   * @return the junction entity class
   */
  Class<?> entity() default void.class;

  /**
   * Column selected in the subquery, matching the referenced entity's ID.
   *
   * @return the select column name
   */
  String select();

  /**
   * The column that the not-between operand targets.
   *
   * <p>An alias for {@link Column#value()}. When blank, the property's mapped
   * column is used instead.
   *
   * @return the column name, or {@link Constant#BLANK} if not specified
   */
  @AliasFor(annotation = Column.class, attribute = "value")
  String where() default Constant.BLANK;

  /**
   * Whether to generate {@code NOT IN} instead of {@code IN}.
   *
   * @return {@code true} for {@code NOT IN (SELECT ...)}, {@code false} for
   * {@code IN (SELECT ...)}
   */
  boolean negative() default false;

}