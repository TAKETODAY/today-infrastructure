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
 * Generates a {@code targetId IN (SELECT referencingColumn FROM junctionTable WHERE fieldColumn = ?)}
 * predicate for a many-to-many relationship through a junction table.
 *
 * <p>Used on a field of an {@link EntityRef @EntityRef} query class. The generated
 * predicate targets the referenced entity's ID column:
 *
 * <pre>{@code
 * @EntityRef(Label.class)
 * class TagQuery {
 *   @Subquery(table = "article_label", referencingColumn = "label_id")
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
 *   @Subquery(entity = ArticleLabel.class, referencingColumn = "label_id")
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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
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
   * Column in the junction table that references the target entity (the entity
   * referenced by {@link EntityRef @EntityRef}).
   *
   * @return the referencing column name
   */
  String referencingColumn();

}