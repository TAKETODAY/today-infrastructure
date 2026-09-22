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
 * Generates a {@code column IN (SELECT column FROM table WHERE sourceColumn = ?)}
 * predicate for a many-to-many relationship through a junction table.
 *
 * <p>Used on a field of an {@link EntityRef @EntityRef} query class. The generated
 * predicate targets the referenced entity's ID column:
 *
 * <pre>{@code
 * @EntityRef(Label.class)
 * class TagQuery {
 *   @Subquery(table = "article_label", column = "label_id", sourceColumn = "article_id")
 *   public final Long articleId;
 * }
 * }</pre>
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
   * @return the junction table name
   */
  String table();

  /**
   * Column in the junction table that matches the referenced entity's ID.
   *
   * @return the column name
   */
  String column();

  /**
   * Column in the junction table that maps to the annotated property value.
   *
   * @return the source column name
   */
  String sourceColumn();

}