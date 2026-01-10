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

import infra.mock.api.MockContainerInitializer;

/**
 * This annotation is used to declare the class types that a {@link MockContainerInitializer
 * ServletContainerInitializer} can handle.
 *
 * @see MockContainerInitializer
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HandlesTypes {

  /**
   * The classes in which a {@link MockContainerInitializer ServletContainerInitializer} has expressed
   * interest.
   *
   * <p>
   * If an implementation of <tt>ServletContainerInitializer</tt> specifies this annotation, the Servlet container must
   * pass the <tt>Set</tt> of application classes that extend, implement, or have been annotated with the class types
   * listed by this annotation to the {@link MockContainerInitializer#onStartup} method of the
   * ServletContainerInitializer (if no matching classes are found, <tt>null</tt> must be passed instead)
   *
   * @return the classes in which {@link MockContainerInitializer ServletContainerInitializer} has
   * expressed interest
   */
  Class<?>[] value();
}
