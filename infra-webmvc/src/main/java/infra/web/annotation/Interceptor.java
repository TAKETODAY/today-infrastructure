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

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.web.HandlerInterceptor;

/**
 * Declarative interceptor configuration annotation.
 * <p>
 * This annotation is used to declare {@link infra.web.HandlerInterceptor} implementations
 * to be applied to a class or method. It supports specifying interceptors by type
 * or by bean name, and allows for both inclusion and exclusion of interceptors.
 * <p>
 * The order of interceptor execution is determined
 * by the order in which they are specified.
 *
 * <pre>{@code
 * // Example usage:
 * @Interceptor(value = {MyInterceptor.class}, excludeNames = "auditInterceptor")
 * public class MyController {
 *
 * }
 * }</pre>
 *
 * @author TODAY
 * @since 2018-11-17 21:23
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Interceptor {

  /**
   * Specify {@link HandlerInterceptor} types to include.
   * <p>
   * The order of execution follows the order of declaration.
   *
   * @return array of HandlerInterceptor classes to include
   */
  @AliasFor(attribute = "include")
  Class<? extends HandlerInterceptor>[] value() default {};

  /**
   * Specify {@link HandlerInterceptor} types to include.
   * <p>
   * The order of execution follows the order of declaration.
   *
   * @return array of HandlerInterceptor classes to include
   */
  @AliasFor(attribute = "value")
  Class<? extends HandlerInterceptor>[] include() default {};

  /**
   * Specify bean names of {@link HandlerInterceptor} to include.
   * <p>
   * The order of interceptors execution is related to the position of the interceptor
   *
   * @return array of bean names to include
   */
  String[] includeNames() default {};

  /**
   * Specify {@link HandlerInterceptor} types to exclude.
   *
   * @return array of HandlerInterceptor classes to exclude
   */
  Class<? extends HandlerInterceptor>[] exclude() default {};

  /**
   * Specify bean names of {@link HandlerInterceptor} to exclude.
   *
   * @return array of bean names to exclude
   */
  String[] excludeNames() default {};

}
