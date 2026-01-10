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

package infra.web.mock;

import infra.beans.factory.Aware;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContextAware;
import infra.mock.api.MockConfig;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link MockConfig} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * <p>Note: Only satisfied if actually running within a Servlet-specific
 * WebApplicationContext. Otherwise, no ServletConfig will be set.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockContextAware
 * @since 4.0 2022/2/20 20:59
 */
public interface MockConfigAware extends Aware {

  /**
   * Set the {@link MockConfig} that this object runs in.
   * <p>Invoked after population of normal bean properties but before an init
   * callback like InitializingBean's {@code afterPropertiesSet} or a
   * custom init-method. Invoked after ApplicationContextAware's
   * {@code setApplicationContext}.
   *
   * @param mockConfig the {@link MockConfig} to be used by this object
   * @see InitializingBean#afterPropertiesSet
   * @see ApplicationContextAware#setApplicationContext
   */
  void setMockConfig(MockConfig mockConfig);

}

