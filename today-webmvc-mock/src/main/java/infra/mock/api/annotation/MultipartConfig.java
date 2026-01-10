/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.mock.api.MockApi;
import infra.mock.api.http.HttpMockRequest;
import infra.web.multipart.Part;

/**
 * Annotation that may be specified on a {@link MockApi} class, indicating that instances of the
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
