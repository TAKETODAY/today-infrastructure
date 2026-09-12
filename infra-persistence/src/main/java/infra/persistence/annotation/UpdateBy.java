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
import infra.persistence.EntityManager;

/**
 * Marks a property used as the matching condition ({@code WHERE}) when updating an
 * entity, instead of updating it.
 *
 * <p>When {@link EntityManager#update(Object)} is called on an entity that has no
 * usable ID value, the properties annotated with {@code @UpdateBy} are collected into
 * the update statement's {@code WHERE} clause (their values are taken from the entity),
 * while the remaining properties selected by the update strategy form the {@code SET}
 * clause. At least one {@code @UpdateBy} property is required in that case, otherwise an
 * {@link infra.dao.InvalidDataAccessApiUsageException} is thrown.
 *
 * <pre>{@code
 *    // Example:
 *
 *    @UpdateBy
 *    @Column(name = "name")
 *    public String getName() {
 *      return name;
 *    }
 * }</pre>
 *
 * <pre>{@code
 *    // Example:
 *
 *    @UpdateBy
 *    private String name;
 *
 *    public String getName() {
 *      return name;
 *    }
 *
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EntityManager#update
 * @since 4.0 2024/4/11 10:43
 */
@Documented
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateBy {

}
