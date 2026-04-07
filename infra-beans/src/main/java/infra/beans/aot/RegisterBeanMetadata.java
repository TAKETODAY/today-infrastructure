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

package infra.beans.aot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.core.annotation.AliasFor;

/**
 * Register reflection hints for bean metadata and its associated bytecode-generated classes.
 *
 * <p>This annotation triggers the registration of reflection hints for:
 * <ul>
 *   <li>The annotated bean class itself</li>
 *   <li>All bean properties (getters/setters/fields)</li>
 *   <li>Property types that require instantiation</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 *
 * <p>1. Register hints for the annotated class (default behavior):
 * <pre>{@code
 * @Component
 * @RegisterBeanMetadata
 * public class UserService {
 *     private String name;
 *     private UserRepository repository;
 *
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 * }
 * }</pre>
 *
 * <p>2. Register hints for additional classes along with the current class:
 * <pre>{@code
 * @Configuration
 * @RegisterBeanMetadata(extra = {User.class, Order.class})
 * public class AppConfig {
 *     // AppConfig, User, and Order will all have metadata registered
 * }
 * }</pre>
 *
 * <p>3. Register hints using class names:
 * <pre>{@code
 * @Configuration
 * @RegisterBeanMetadata(extraNames = {"com.example.User", "com.example.Order"})
 * public class AppConfig {
 *     // AppConfig, User, and Order will all have metadata registered
 * }
 * }</pre>
 *
 * <p>4. Use as a meta-annotation on custom stereotypes:
 * <pre>{@code
 * @Target(ElementType.TYPE)
 * @Retention(RetentionPolicy.RUNTIME)
 * @RegisterBeanMetadata
 * public @interface Entity {
 *     // ...
 * }
 *
 * @Entity
 * public class User {
 *     // ...
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see BeanMetadataReflectiveProcessor
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD,
        ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(BeanMetadataReflectiveProcessor.class)
public @interface RegisterBeanMetadata {

  /**
   * Extra classes for which bean metadata reflection hints should be registered,
   * in addition to the annotated class itself.
   *
   * <p>The annotated class is always included. This attribute allows you to specify
   * extra classes that also need their bean metadata registered.
   *
   * <p>If both {@code extra} and {@code extraNames} are specified, they are merged
   * into a single set of additional target classes.
   *
   * @return extra classes to register reflection hints for
   * @see #extraNames()
   */
  @AliasFor("extra")
  Class<?>[] value() default {};

  /**
   * Extra classes for which bean metadata reflection hints should be registered,
   * in addition to the annotated class itself.
   *
   * <p>The annotated class is always included. This attribute allows you to specify
   * extra classes that also need their bean metadata registered.
   *
   * <p>If both {@code extra} and {@code extraNames} are specified, they are merged
   * into a single set of additional target classes.
   *
   * @return extra classes to register reflection hints for
   * @see #value()
   * @see #extraNames()
   */
  @AliasFor("value")
  Class<?>[] extra() default {};

  /**
   * Alternative to {@link #extra()} to specify extra classes as fully qualified class names.
   *
   * <p>This is useful when the target classes are not directly accessible in the current scope
   * or when you want to avoid compile-time dependencies on those classes.
   *
   * <p>The annotated class is always included. This attribute allows you to specify
   * extra classes that also need their bean metadata registered.
   *
   * <p>If both {@code extra} and {@code extraNames} are specified, they are merged
   * into a single set of additional target classes.
   *
   * @return extra fully qualified class names to register reflection hints for
   * @see #extra()
   */
  String[] extraNames() default {};

}
