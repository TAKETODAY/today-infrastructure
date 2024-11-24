/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.web.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;

/**
 * Shortcut for {@link HttpExchange @HttpExchange} for HTTP POST requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpExchange(method = "POST")
public @interface PostExchange {

  /**
   * Alias for {@link HttpExchange#value}.
   */
  @AliasFor(annotation = HttpExchange.class)
  String value() default "";

  /**
   * Alias for {@link HttpExchange#url()}.
   */
  @AliasFor(annotation = HttpExchange.class)
  String url() default "";

  /**
   * Alias for {@link HttpExchange#contentType()}.
   */
  @AliasFor(annotation = HttpExchange.class)
  String contentType() default "";

  /**
   * Alias for {@link HttpExchange#accept()}.
   */
  @AliasFor(annotation = HttpExchange.class)
  String[] accept() default {};

  /**
   * Alias for {@link HttpExchange#headers()}.
   */
  @AliasFor(annotation = HttpExchange.class)
  String[] headers() default {};

}
