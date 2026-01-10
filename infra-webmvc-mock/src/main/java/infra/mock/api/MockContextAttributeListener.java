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

package infra.mock.api;

import java.util.EventListener;

import infra.mock.api.annotation.WebListener;

/**
 * Interface for receiving notification events about MockContext attribute changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link MockContext}.
 *
 * <p>
 * The order in which implementations of this interface are invoked is unspecified.
 *
 * @see MockContextAttributeEvent
 */
public interface MockContextAttributeListener extends EventListener {

  /**
   * Receives notification that an attribute has been added to the MockContext.
   *
   * @param event the MockContextAttributeEvent containing the MockContext to which the attribute was added, along
   * with the attribute name and value
   * @implSpec The default implementation takes no action.
   */
  default void attributeAdded(MockContextAttributeEvent event) {
  }

  /**
   * Receives notification that an attribute has been removed from the MockContext.
   *
   * @param event the MockContextAttributeEvent containing the MockContext from which the attribute was removed,
   * along with the attribute name and value
   * @implSpec The default implementation takes no action.
   */
  default void attributeRemoved(MockContextAttributeEvent event) {
  }

  /**
   * Receives notification that an attribute has been replaced in the MockContext.
   *
   * @param event the MockContextAttributeEvent containing the MockContext in which the attribute was replaced,
   * along with the attribute name and its old value
   * @implSpec The default implementation takes no action.
   */
  default void attributeReplaced(MockContextAttributeEvent event) {
  }
}
