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
import infra.util.MultiValueMap;

/**
 * Annotation which indicates that a method parameter should be bound to a web request header.
 *
 * <p>Supported for annotated handler methods in Web MVC and Infra WebFlux.
 *
 * <p>If the method parameter is {@link java.util.Map Map&lt;String, String&gt;},
 * {@link MultiValueMap MultiValueMap&lt;String, String&gt;},
 * or {@link infra.http.HttpHeaders HttpHeaders} then the map is
 * populated with all header names and values.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY<br>
 * @see RequestMapping
 * @see RequestParam
 * @see CookieValue
 * @since 2018-08-21 19:19
 */
@Documented
@RequestParam
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface RequestHeader {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default "";

  /**
   * The name of the request header to bind to.
   *
   * @since 4.0
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
  String name() default "";

  /**
   * Whether the header is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown
   * if the header is missing in the request. Switch this to
   * {@code false} if you prefer a {@code null} value if the header is
   * not present in the request.
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
   * sets this flag to {@code false}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

  /**
   * The default value to use as a fallback.
   * <p>Supplying a default value implicitly sets {@link #required} to
   * {@code false}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "defaultValue")
  String defaultValue() default Constant.DEFAULT_NONE;

}
