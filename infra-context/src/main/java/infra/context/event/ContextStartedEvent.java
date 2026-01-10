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

package infra.context.event;

import infra.context.ApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets started.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-10 10:52
 */
@SuppressWarnings("serial")
public class ContextStartedEvent extends ApplicationContextEvent {

  /**
   * Create a new ContextStartedEvent.
   *
   * @param source the {@code ApplicationContext} that has been started
   * (must not be {@code null})
   */
  public ContextStartedEvent(ApplicationContext source) {
    super(source);
  }

}
