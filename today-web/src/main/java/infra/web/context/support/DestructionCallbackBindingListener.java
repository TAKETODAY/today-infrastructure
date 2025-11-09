/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.context.support;

import java.io.Serial;
import java.io.Serializable;

import infra.session.AttributeBindingListener;
import infra.session.Session;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 21:31
 */
public class DestructionCallbackBindingListener implements AttributeBindingListener, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Runnable destructionCallback;

  /**
   * Create a new DestructionCallbackBindingListener for the given callback.
   *
   * @param destructionCallback the Runnable to execute when this listener
   * object gets unbound from the session
   */
  public DestructionCallbackBindingListener(Runnable destructionCallback) {
    this.destructionCallback = destructionCallback;
  }

  @Override
  public void valueUnbound(Session session, String attributeName) {
    destructionCallback.run();
  }

}
