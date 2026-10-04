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
 * Declares a generated primary key on an entity field or accessor method.
 *
 * <p>By default, the database generates the ID and the persistence layer reads
 * it back through JDBC generated keys. An application generator runs before
 * insertion, after pre-persist callbacks, and is invoked only when the ID is
 * {@code null}. Existing non-null IDs are preserved by application generators.
 *
 * <p>A custom {@link IdGenerator} can generate local or distributed IDs and
 * may be selected by {@linkplain #generator() type} or
 * {@linkplain #generatorName() bean name}.
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
   * The built-in strategy to use when no custom generator is specified.
   * <p>Defaults to database-generated {@link GenerationType#IDENTITY IDENTITY}.
   */
  GenerationType strategy() default GenerationType.IDENTITY;

  /**
   * The custom generator type, or {@link IdGenerator} itself when unspecified.
   * <p>Without a {@link #generatorName() bean name}, a matching container bean
   * is preferred. If none exists, an instance is created through dependency
   * injection and reused within the entity manager. Ambiguous bean candidates
   * must be resolved through container configuration or an explicit bean name.
   * <p>When a bean name is also specified, this type constrains the named bean
   * instead of selecting or creating another instance.
   * <p>A custom generator overrides the default {@link GenerationType#IDENTITY
   * IDENTITY} strategy and cannot be combined with another built-in strategy.
   */
  Class<? extends IdGenerator> generator() default IdGenerator.class;

  /**
   * The name of an existing {@link IdGenerator} bean, or an empty string when
   * no bean is selected by name.
   * <p>A non-empty name takes precedence over type-based lookup. The bean must
   * implement {@link IdGenerator} and match any explicitly specified
   * {@link #generator() generator type}. A missing or incompatible bean is an
   * error; no fallback instance is created. The resolved instance is cached per
   * generator type and bean name within the resolver, even for prototype beans,
   * and must be thread-safe. Existing beans are not destroyed by the resolver.
   */
  String generatorName() default "";

}
