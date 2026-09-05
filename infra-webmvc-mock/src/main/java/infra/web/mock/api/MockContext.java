/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.mock.api;

import infra.context.ApplicationContext;
import infra.core.AttributeAccessor;
import infra.web.mock.MockDispatcherHandler;

/**
 * Mock context interface combining {@link AttributeAccessor}, {@link ApplicationContext}
 * and {@link MockDispatcherHandler dispatcher handler} access capabilities, used to store
 * and retrieve context attributes, access the application context and the dispatcher
 * handler in a web test environment.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/9/5
 */
public interface MockContext extends AttributeAccessor {

  /**
   * Return the {@link ApplicationContext application context} currently associated with
   * this context.
   *
   * @return the associated application context, never {@code null}
   */
  ApplicationContext getApplicationContext();

  /**
   * Return the {@link MockDispatcherHandler dispatcher handler} associated with this
   * context.
   *
   * @return the associated dispatcher handler, never {@code null}
   */
  MockDispatcherHandler getDispatcherHandler();

}
