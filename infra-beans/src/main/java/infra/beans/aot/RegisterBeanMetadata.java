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
 * <p>Usage example on a bean class:
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
 * <p>This annotation can also be used as a meta-annotation on custom stereotypes:
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
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(BeanMetadataReflectiveProcessor.class)
public @interface RegisterBeanMetadata {

}
