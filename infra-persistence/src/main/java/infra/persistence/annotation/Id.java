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

/**
 * Specifies the primary key of an entity.
 *
 * <p>The annotated property or field should hold the entity's identifier, typically
 * of a primitive type, a primitive wrapper type, {@code String}
 *
 * <p>The mapped column is the primary key of the entity's primary table. When no
 * {@link Column @Column} is specified, the column name defaults to the name of the
 * annotated property or field.
 *
 * <p>Usage on a field:
 * <pre>{@code
 * @Id
 * private Long id;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Column
 * @since 4.0 2022/8/16 20:58
 */
@Reflective
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {

}
