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

package cn.taketoday.web.annotation;

import java.beans.PropertyEditor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * Annotation that can be used to associate the part of a "multipart/form-data" request
 * with a method argument.
 *
 * <p>Supported method argument types include {@link MultipartFile} in
 * conjunction with multipart requests, or otherwise for any other method
 * argument, the content of the part is passed through an {@link HttpMessageConverter}
 * taking into consideration the 'Content-Type' header of the request part. This is
 * analogous to what @{@link RequestBody} does to resolve an argument based on the
 * content of a non-multipart regular request.
 *
 * <p>Note that @{@link RequestParam} annotation can also be used to associate the part
 * of a "multipart/form-data" request with a method argument supporting the same method
 * argument types. The main difference is that when the method argument is not a String
 * or raw {@code MultipartFile} / {@code Part}, {@code @RequestParam} relies on type
 * conversion via a registered {@link Converter} or {@link PropertyEditor} while
 * {@link RequestPart} relies on {@link HttpMessageConverter HttpMessageConverters}
 * taking into consideration the 'Content-Type' header of the request part.
 * {@link RequestParam} is likely to be used with name-value form fields while
 * {@link RequestPart} is likely to be used with parts containing more complex content
 * e.g. JSON, XML).
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestParam
 * @since 4.0 2022/3/2 18:20
 */
@Documented
@RequestParam
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPart {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default "";

  /**
   * The name of the part in the {@code "multipart/form-data"} request to bind to.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
  String name() default "";

  /**
   * Whether the part is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown
   * if the part is missing in the request. Switch this to
   * {@code false} if you prefer a {@code null} value if the part is
   * not present in the request.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

}

