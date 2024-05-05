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

package cn.taketoday.mock.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.Part;

/**
 * Annotation that may be specified on a {@link cn.taketoday.mock.api.Servlet} class, indicating that instances of the
 * <tt>Servlet</tt> expect requests that conform to the <tt>multipart/form-data</tt> MIME type.
 *
 * <p>
 * Servlets annotated with <tt>MultipartConfig</tt> may retrieve the {@link Part} components of a
 * given <tt>multipart/form-data</tt> request by calling {@link HttpMockRequest#getPart getPart}
 * or {@link HttpMockRequest#getParts getParts}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultipartConfig {

  /**
   * The directory location where files will be stored
   *
   * @return the directory location where files will be stored
   */
  String location() default "";

  /**
   * The maximum size allowed for uploaded files.
   *
   * <p>
   * The default is <tt>-1L</tt>, which means unlimited.
   *
   * @return the maximum size allowed for uploaded files
   */
  long maxFileSize() default -1L;

  /**
   * The maximum size allowed for <tt>multipart/form-data</tt> requests
   *
   * <p>
   * The default is <tt>-1L</tt>, which means unlimited.
   *
   * @return the maximum size allowed for <tt>multipart/form-data</tt> requests
   */
  long maxRequestSize() default -1L;

  /**
   * The size threshold after which the file will be written to disk
   *
   * @return the size threshold after which the file will be written to disk
   */
  int fileSizeThreshold() default 0;
}
