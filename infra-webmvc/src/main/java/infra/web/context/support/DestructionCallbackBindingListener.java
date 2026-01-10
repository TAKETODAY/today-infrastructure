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
