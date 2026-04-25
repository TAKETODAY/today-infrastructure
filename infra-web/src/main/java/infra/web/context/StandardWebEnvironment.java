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

package infra.web.context;

import infra.core.env.Environment;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;

/**
 * {@link Environment} implementation to be used by {@code netty}-based web
 * applications. All web-related (netty-based) {@code ApplicationContext} classes
 * initialize an instance by default.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardWebEnvironment extends StandardEnvironment implements ConfigurableWebEnvironment {

  public StandardWebEnvironment() {
    super();
  }

  protected StandardWebEnvironment(PropertySources propertySources) {
    super(propertySources);
  }

}
