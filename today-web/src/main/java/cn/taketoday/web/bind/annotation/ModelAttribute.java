/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.ui.Model;

/**
 * Annotation that binds a method parameter or method return value
 * to a named model attribute, exposed to a web view. Supported
 * for controller classes with {@link RequestMapping @RequestMapping}
 * methods.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * in the reference manual.
 *
 * <p>{@code @ModelAttribute} can be used to expose command objects to a web view,
 * using specific attribute names, by annotating corresponding parameters of an
 * {@link RequestMapping @RequestMapping} method.
 *
 * <p>{@code @ModelAttribute} can also be used to expose reference data to a web
 * view by annotating accessor methods in a controller class with
 * {@link RequestMapping @RequestMapping} methods. Such accessor
 * methods are allowed to have any arguments that
 * {@link RequestMapping @RequestMapping} methods support, returning
 * the model attribute value to expose.
 *
 * <p>Note however that reference data and all other model content are
 * not available to web views when request processing results in an
 * {@code Exception} since the exception could be raised at any time
 * making the content of the model unreliable. For this reason
 * {@link ExceptionHandler @ExceptionHandler} methods do not provide
 * access to a {@link Model} argument.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 22:50
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface ModelAttribute {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the model attribute to bind to.
   * <p>The default model attribute name is inferred from the declared
   * attribute type (i.e. the method parameter type or method return type),
   * based on the non-qualified class name:
   * e.g. "orderAddress" for class "mypackage.OrderAddress",
   * or "orderAddressList" for "List&lt;mypackage.OrderAddress&gt;".
   */
  @AliasFor("value")
  String name() default "";

  /**
   * Allows declaring data binding disabled directly on an {@code @ModelAttribute}
   * method parameter or on the attribute returned from an {@code @ModelAttribute}
   * method, both of which would prevent data binding for that attribute.
   * <p>By default this is set to {@code true} in which case data binding applies.
   * Set this to {@code false} to disable data binding.
   */
  boolean binding() default true;

}
