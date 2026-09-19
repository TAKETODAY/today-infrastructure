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

import infra.beans.aot.RegisterBeanMetadata;
import infra.persistence.RefEntityMetadata;

/**
 * Declares that the annotated entity references the primary table of another entity.
 *
 * <p>Typically used on entities that do not declare their own ID property, but
 * instead share the primary key of the referenced entity. This is common for
 * partial views or update models of a base entity.
 *
 * <p>Usage:
 * <pre>{@code
 * @Table(name = "t_user")
 * public class User { ... }
 *
 * @EntityRef(User.class)
 * public class UpdateUser { ... }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RefEntityMetadata#findIdProperty()
 * @since 4.0 2024/4/11 13:36
 */
@RegisterBeanMetadata
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityRef {

  /**
   * The entity type whose primary table is referenced.
   *
   * @return the referenced entity class
   */
  Class<?> value();

}