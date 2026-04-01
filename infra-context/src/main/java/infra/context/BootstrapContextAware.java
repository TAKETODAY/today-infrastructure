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

package infra.context;

import infra.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link BootstrapContext} that it runs in.
 *
 * <p>Implementing this interface allows an object to access the bootstrap context
 * programmatically, enabling initialization logic that depends on the context's
 * state or configuration before the main application context is refreshed.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/1 23:38
 */
public interface BootstrapContextAware extends Aware {

  /**
   * Set the {@link BootstrapContext} that this object runs in.
   *
   * <p>This method is invoked after the population of basic properties and
   * before any initialization methods such as {@code afterPropertiesSet} or
   * custom init-methods are called.
   *
   * @param context the bootstrap context instance
   */
  void setBootstrapContext(BootstrapContext context);

}
