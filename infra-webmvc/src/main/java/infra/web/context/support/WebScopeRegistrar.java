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
import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.session.Session;
import infra.web.AbstractDelegatingServerHttpExchange;
import infra.web.ServerHttpExchange;
import infra.web.HttpContextHolder;

/**
 * Registers web scopes and injectable request-context dependencies with a BeanFactory.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class WebScopeRegistrar {

  private WebScopeRegistrar() {
  }

  /**
   * Register the {@code request} and {@code session} scopes, along with
   * resolvable dependencies for {@link ServerHttpExchange} and {@link Session}.
   * <p>Registration does not require an active request or create a session.
   * Dependencies resolve against {@link HttpContextHolder} when accessed.
   *
   * @param beanFactory the BeanFactory to configure
   */
  public static void registerScopes(ConfigurableBeanFactory beanFactory) {
    beanFactory.registerScope(ServerHttpExchange.SCOPE_REQUEST, RequestScope.instance);
    beanFactory.registerScope(ServerHttpExchange.SCOPE_SESSION, new SessionScope());

    beanFactory.registerResolvableDependency(Session.class, new WebSessionProvider());
    beanFactory.registerResolvableDependency(ServerHttpExchange.class, new InjectableServerHttpExchange());
  }

  /**
   * Factory that exposes the current web-session object on demand.
   */
  private static final class WebSessionProvider implements Supplier<Session>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Session get() {
      return HttpContextHolder.required().getSession();
    }

    @Override
    public String toString() {
      return "Current Session";
    }
  }

  /**
   * Factory that exposes the current request-context object on demand.
   */
  private static final class InjectableServerHttpExchange extends AbstractDelegatingServerHttpExchange {

    @Override
    public ServerHttpExchange delegate() {
      return HttpContextHolder.required();
    }

    @Override
    public String toString() {
      return "Current ServerHttpExchange";
    }
  }

}
