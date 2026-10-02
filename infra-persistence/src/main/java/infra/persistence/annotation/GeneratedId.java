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
import infra.persistence.GenerationType;
import infra.persistence.IdGenerator;

/**
 * Specifies a generated primary key of an entity.
 * By default the database generates the ID. Application generators run after
 * pre-persist callbacks and preserve existing non-null IDs.
 *
 * <p>This annotation may be used as a meta-annotation. Composed annotations
 * can override its attributes through {@link infra.core.annotation.AliasFor}.
 *
 * <pre>{@code
 * @GeneratedId(generator = OrderIdGenerator.class)
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.FIELD,ElementType.METHOD})
 * public @interface OrderId {
 *
 *   @AliasFor(annotation = GeneratedId.class, attribute = "generatorName")
 *   String value() default "";
 * }
 * }</pre>
 *
 * <p>The mapped column for the primary key of the entity is assumed
 * to be the primary key of the primary table. If no <code>Column</code> annotation
 * is specified, the primary key column name is assumed to be the name
 * of the primary key property or field.
 *
 * <pre>{@code
 *   // Example:
 *
 *   @GeneratedId
 *   public Long getId() {
 *     return id;
 *   }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/14 21:11
 */
@Id
@Reflective
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedId {

  /**
   * Built-in strategy, used when no custom generator is specified.
   */
  GenerationType strategy() default GenerationType.IDENTITY;

  /**
   * Generator type. An existing bean is preferred; otherwise dependency
   * injection creates an instance. The interface itself means unspecified.
   * A custom generator overrides the default IDENTITY strategy and cannot
   * be combined with another built-in strategy.
   */
  Class<? extends IdGenerator> generator() default IdGenerator.class;

  /**
   * Name of an existing generator bean. When a generator type is also supplied,
   * it constrains the bean type. A missing named bean is an error.
   */
  String generatorName() default "";

}
