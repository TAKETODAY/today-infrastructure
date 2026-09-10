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
 * Specifies the version property of an entity class that serves as its optimistic
 * lock value. The version is used to ensure integrity when performing update and
 * delete operations and for optimistic concurrency control.
 *
 * <p>Only a single {@code Version} property is supported per entity class; entities
 * with more than one {@code Version} property are rejected with an
 * {@link IllegalEntityException}.
 *
 * <p>The following types are supported out of the box:
 * <ul>
 *   <li>{@code int}, {@code Integer}</li>
 *   <li>{@code long}, {@code Long}</li>
 *   <li>{@code short}, {@code Short}</li>
 *   <li>{@link java.time.Instant}</li>
 *   <li>{@link java.time.LocalDateTime}</li>
 *   <li>{@link java.time.ZonedDateTime}</li>
 *   <li>{@link java.time.OffsetDateTime}</li>
 * </ul>
 *
 * <p>For custom version types, implement a {@link VersionIncrementStrategy} and
 * configure it on the {@link DefaultEntityManager}:
 *
 * <pre>{@code
 *    // Custom strategy for a string-based version
 *    entityManager.setVersionIncrementStrategy(currentVersion -> {
 *      String v = (String) currentVersion;
 *      return v + "_updated";
 *    });
 *
 *    // Or compose with the built-in defaults as fallback
 *    entityManager.setVersionIncrementStrategy(
 *        myCustomStrategy.and(new DefaultVersionIncrementStrategy()));
 * }</pre>
 *
 * <p>Example:
 * <pre>{@code
 *    // Numeric version
 *    @Version
 *    @Column(name = "OPTLOCK")
 *    protected int getVersionNum() {
 *      return versionNum;
 *    }
 *
 *    // Date-time version
 *    @Version
 *    protected Instant lastModified;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see VersionIncrementStrategy
 * @see infra.persistence.support.DefaultVersionIncrementStrategy
 * @since 4.0 2022/8/16 21:07
 */
@Documented
@Reflective
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {

}
