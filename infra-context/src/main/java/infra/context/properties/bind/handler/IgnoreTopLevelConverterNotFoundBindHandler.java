/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.bind.handler;

import org.jspecify.annotations.Nullable;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.Bindable;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.core.conversion.ConverterNotFoundException;

/**
 * {@link BindHandler} that can be used to ignore top-level
 * {@link ConverterNotFoundException}s.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class IgnoreTopLevelConverterNotFoundBindHandler extends AbstractBindHandler {

  /**
   * Create a new {@link IgnoreTopLevelConverterNotFoundBindHandler} instance.
   */
  public IgnoreTopLevelConverterNotFoundBindHandler() { }

  /**
   * Create a new {@link IgnoreTopLevelConverterNotFoundBindHandler} instance with a
   * specific parent.
   *
   * @param parent the parent handler
   */
  public IgnoreTopLevelConverterNotFoundBindHandler(BindHandler parent) {
    super(parent);
  }

  @Override
  @Nullable
  public Object onFailure(
          ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
          throws Exception {
    if (context.getDepth() == 0 && error instanceof ConverterNotFoundException) {
      return null;
    }
    throw error;
  }

}
