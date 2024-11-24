/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.properties.bind.handler;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.Bindable;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.core.conversion.ConverterNotFoundException;
import infra.lang.Nullable;

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
