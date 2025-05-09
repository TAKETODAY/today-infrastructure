/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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