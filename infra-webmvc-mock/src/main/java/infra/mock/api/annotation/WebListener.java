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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.mock.api.MockContextAttributeListener;
import infra.mock.api.MockContextListener;
import infra.mock.api.MockRequestAttributeListener;
import infra.mock.api.MockRequestListener;
import infra.mock.api.http.HttpSessionAttributeListener;
import infra.mock.api.http.HttpSessionIdListener;
import infra.mock.api.http.HttpSessionListener;

/**
 * This annotation is used to declare a WebListener.
 *
 * Any class annotated with WebListener must implement one or more of the
 * {@link MockContextListener}, {@link MockContextAttributeListener},
 * {@link MockRequestListener}, {@link MockRequestAttributeListener},
 * {@link HttpSessionListener}, or {@link HttpSessionAttributeListener}, or
 * {@link HttpSessionIdListener} interfaces.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebListener {
  /**
   * Description of the listener
   *
   * @return description of the listener
   */
  String value() default "";
}
