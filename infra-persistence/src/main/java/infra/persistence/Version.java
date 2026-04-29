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

package infra.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;

/**
 * Specifies the version field or property of an entity class that
 * serves as its optimistic lock value. The version is used to ensure
 * integrity when performing update and delete operations and for
 * optimistic concurrency control.
 *
 * <p>Only a single {@code Version} property or field
 * should be used per class; applications that use more than one
 * {@code Version} property or field will not be portable.
 *
 * <p>The {@code Version} property should be mapped to
 * the primary table for the entity class; applications that
 * map the {@code Version} property to a table other than
 * the primary table will not be portable.
 *
 * <p>The following types are supported out of the box:
 * <ul>
 *   <li>{@code int}, {@code Integer}</li>
 *   <li>{@code short}, {@code Short}</li>
 *   <li>{@code long}, {@code Long}</li>
 *   <li>{@link java.sql.Timestamp}</li>
 *   <li>{@link java.time.Instant}</li>
 * </ul>
 *
 * <p>For custom version types, implement and configure a
 * {@link VersionIncrementStrategy} on the
 * {@link DefaultEntityManager#setVersionIncrementStrategy DefaultEntityManager}:
 *
 * <pre>{@code
 *    // Custom strategy for string-based version
 *    entityManager.setVersionIncrementStrategy(currentVersion -> {
 *      String v = (String) currentVersion;
 *      return v + "_updated";
 *    });
 *
 *    // Or compose with built-in defaults as fallback
 *    entityManager.setVersionIncrementStrategy(
 *      myCustomStrategy.or(VersionIncrementStrategy.defaults()));
 * }</pre>
 *
 * <p>Example:
 * <pre>{@code
 *    // Built-in type
 *    @Version
 *    @Column(name = "OPTLOCK")
 *    protected int getVersionNum() {
 *      return versionNum;
 *    }
 *
 *    // Java 8 Instant
 *    @Version
 *    protected Instant lastModified;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:07
 */
@Documented
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {

}
