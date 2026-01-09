/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Annotation which indicates that a method parameter should be bound to a web request parameter.
 * <p>
 * this Annotation provides {@code NamedValueInfo}
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.web.handler.method.NamedValueInfo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface RequestParam {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the request parameter to bind to.
   *
   * @since 4.0
   */
  @AliasFor("value")
  String name() default "";

  /**
   * Whether the parameter is required.
   * <p>Defaults to {@code false}, leading to an exception being thrown
   * if the parameter is missing in the request. Switch this to
   * {@code false} if you prefer a {@code null} value if the parameter is
   * not present in the request.
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
   * sets this flag to {@code false}.
   * If required == true when request parameter is null, will be throws exception
   */
  boolean required() default true;

  /**
   * The default value to use as a fallback when the request parameter is
   * not provided or has an empty value.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  String defaultValue() default Constant.DEFAULT_NONE;

}
