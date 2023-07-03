/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import cn.taketoday.lang.Nullable;

/**
 * Callback interface that can be used to handle additional logic during element
 * {@link Binder binding}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface BindHandler {

  /**
   * Default no-op bind handler.
   */
  BindHandler DEFAULT = new BindHandler() {

  };

  /**
   * Called when binding of an element starts but before any result has been determined.
   *
   * @param <T> the bindable source type
   * @param name the name of the element being bound
   * @param target the item being bound
   * @param context the bind context
   * @return the actual item that should be used for binding (may be {@code null})
   */
  @Nullable
  default <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
    return target;
  }

  /**
   * Called when binding of an element ends with a successful result. Implementations
   * may change the ultimately returned result or perform addition validation.
   *
   * @param name the name of the element being bound
   * @param target the item being bound
   * @param context the bind context
   * @param result the bound result (never {@code null})
   * @return the actual result that should be used (may be {@code null})
   */
  @Nullable
  default Object onSuccess(ConfigurationPropertyName name,
          Bindable<?> target, BindContext context, Object result) {
    return result;
  }

  /**
   * Called when binding of an element ends with an unbound result and a newly created
   * instance is about to be returned. Implementations may change the ultimately
   * returned result or perform addition validation.
   *
   * @param name the name of the element being bound
   * @param target the item being bound
   * @param context the bind context
   * @param result the newly created instance (never {@code null})
   * @return the actual result that should be used (must not be {@code null})
   */
  default Object onCreate(ConfigurationPropertyName name,
          Bindable<?> target, BindContext context, Object result) {
    return result;
  }

  /**
   * Called when binding fails for any reason (including failures from
   * {@link #onSuccess} or {@link #onCreate} calls). Implementations may choose to
   * swallow exceptions and return an alternative result.
   *
   * @param name the name of the element being bound
   * @param target the item being bound
   * @param context the bind context
   * @param error the cause of the error (if the exception stands it may be re-thrown)
   * @return the actual result that should be used (may be {@code null}).
   * @throws Exception if the binding isn't valid
   */
  @Nullable
  default Object onFailure(ConfigurationPropertyName name,
          Bindable<?> target, BindContext context, Exception error) throws Exception {
    throw error;
  }

  /**
   * Called when binding finishes with either bound or unbound result. This method will
   * not be called when binding failed, even if a handler returns a result from
   * {@link #onFailure}.
   *
   * @param name the name of the element being bound
   * @param target the item being bound
   * @param context the bind context
   * @param result the bound result (may be {@code null})
   * @throws Exception if the binding isn't valid
   */
  default void onFinish(ConfigurationPropertyName name,
          Bindable<?> target, BindContext context, @Nullable Object result) throws Exception {
  }

}
