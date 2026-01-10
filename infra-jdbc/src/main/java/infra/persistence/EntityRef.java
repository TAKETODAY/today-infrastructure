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

/**
 * Specifies the primary table ref for the annotated entity
 *
 * <pre> {@code
 *    Example:
 *
 *    @Table(name="t_user")
 *    public class User {
 *      ...
 *    }
 *
 *    @EntityRef(User.class)
 *    public class UpdateUser {
 *      ...
 *    }
 *
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/11 13:36
 */
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityRef {

  /**
   * Returns the class object representing the entity type
   * referenced by this annotation.
   *
   * <p>This method is typically used to obtain the primary
   * table reference for an annotated entity. It allows frameworks
   * or libraries to dynamically resolve and interact with the
   * specified entity class at runtime.
   *
   * <p>Example usage:
   * <pre>{@code
   *    @EntityRef(User.class)
   *    public class UpdateUser {
   *      // Class body
   *    }
   *
   *    // Retrieving the referenced entity class
   *    EntityRef entityRef = UpdateUser.class.getAnnotation(EntityRef.class);
   *    Class<?> entityClass = entityRef.value();
   *
   *    System.out.println("Referenced entity: " + entityClass.getName());
   * }</pre>
   *
   * @return the {@link Class} object representing the referenced entity type
   */
  Class<?> value();

}