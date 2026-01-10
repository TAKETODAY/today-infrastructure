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
 * Interface for receiving notification events about MockContext lifecycle changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link MockContext}.
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #contextInitialized} method in the order in which they
 * have been declared, and at their {@link #contextDestroyed} method in reverse order.
 *
 * @see MockContextEvent
 */
public interface MockContextListener extends EventListener {

  /**
   * Receives notification that the web application initialization process is starting.
   *
   * <p>
   * All MockContextListeners are notified of context initialization before any filters or servlets in the web
   * application are initialized.
   *
   * @param sce the MockContextEvent containing the MockContext that is being initialized
   * @implSpec The default implementation takes no action.
   */
  default public void contextInitialized(MockContextEvent sce) {
  }

  /**
   * Receives notification that the MockContext is about to be shut down.
   *
   * <p>
   * All servlets and filters will have been destroyed before any MockContextListeners are notified of context
   * destruction.
   *
   * @param sce the MockContextEvent containing the MockContext that is being destroyed
   * @implSpec The default implementation takes no action.
   */
  default public void contextDestroyed(MockContextEvent sce) {
  }
}
