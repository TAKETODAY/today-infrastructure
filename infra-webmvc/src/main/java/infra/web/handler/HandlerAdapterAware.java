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

package infra.web.handler;

import infra.web.HandlerAdapter;

/**
 * An interface to be implemented by objects that wish to receive a {@link HandlerAdapter} instance.
 * <p>
 * This allows the {@code HandlerAdapter} to be injected at runtime, typically by the framework's
 * configuration or initialization process.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/7 23:15
 */
public interface HandlerAdapterAware {

  /**
   * Set the {@link HandlerAdapter} for this object.
   *
   * @param handlerAdapter the {@code HandlerAdapter} to use
   */
  void setHandlerAdapter(HandlerAdapter handlerAdapter);
}
