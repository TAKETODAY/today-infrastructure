/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.bind;

import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for {@link BindHandler} implementations.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractBindHandler implements BindHandler {

  private final BindHandler parent;

  /**
   * Create a new binding handler instance.
   */
  public AbstractBindHandler() {
    this(BindHandler.DEFAULT);
  }

  /**
   * Create a new binding handler instance with a specific parent.
   *
   * @param parent the parent handler
   */
  public AbstractBindHandler(BindHandler parent) {
    Assert.notNull(parent, "Parent is required");
    this.parent = parent;
  }

  @Override
  public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
    return this.parent.onStart(name, target, context);
  }

  @Override
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    return this.parent.onSuccess(name, target, context, result);
  }

  @Nullable
  @Override
  public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
          throws Exception {
    return this.parent.onFailure(name, target, context, error);
  }

  @Override
  public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
          throws Exception {
    this.parent.onFinish(name, target, context, result);
  }

}
