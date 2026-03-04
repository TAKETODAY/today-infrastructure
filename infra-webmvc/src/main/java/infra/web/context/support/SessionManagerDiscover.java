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

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.lang.Assert;
import infra.session.SessionManager;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;

/**
 * Helper class to discover and obtain a {@link SessionManager} instance.
 * <p>It first attempts to locate the manager in the configured {@link BeanFactory},
 * utilizing a double-checked locking mechanism for thread-safe lazy initialization.
 * If not found in the bean factory, it can also attempt to retrieve it from the
 * current {@link RequestContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/30 17:41
 */
public class SessionManagerDiscover {

  private final BeanFactory beanFactory;

  private volatile @Nullable SessionManager sessionManager;

  private volatile boolean managerLoaded = false;

  /**
   * Constructs a new SessionManagerDiscover with the given BeanFactory.
   *
   * @param beanFactory the BeanFactory to search for the SessionManager
   */
  public SessionManagerDiscover(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    this.beanFactory = beanFactory;
  }

  /**
   * Obtains the {@link SessionManager} from the configured {@link #beanFactory}.
   * <p>This method uses double-checked locking to ensure thread-safe lazy loading.
   * It first tries to find the bean by name ({@link SessionManager#BEAN_NAME}),
   * and if not found, it searches by type.
   *
   * @return the discovered SessionManager, or {@code null} if not found
   */
  public @Nullable SessionManager find() {
    SessionManager sessionManager = this.sessionManager;
    if (sessionManager == null) {
      synchronized(this) {
        sessionManager = this.sessionManager;
        if (sessionManager == null) {
          if (managerLoaded) {
            return null;
          }
          sessionManager = BeanFactoryUtils.find(
                  beanFactory, SessionManager.BEAN_NAME, SessionManager.class);
          if (sessionManager == null) {
            sessionManager = BeanFactoryUtils.find(beanFactory, SessionManager.class);
          }
          this.sessionManager = sessionManager;
          this.managerLoaded = true;
        }
      }
    }
    return sessionManager;
  }

  /**
   * Obtains the {@link SessionManager}, first checking the configured {@link #beanFactory},
   * and if not found, attempting to retrieve it from the provided {@link RequestContext}.
   *
   * @param request the current request context to check for a SessionManager
   * @return the discovered SessionManager, or {@code null} if not found in either source
   */
  public @Nullable SessionManager find(RequestContext request) {
    SessionManager sessionManager = find();
    if (sessionManager == null) {
      sessionManager = RequestContextUtils.getSessionManager(request);
    }
    return sessionManager;
  }

  /**
   * Obtains the {@link SessionManager} for the given request.
   * <p>This method ensures a non-null result by throwing an exception if no
   * SessionManager can be found in the bean factory or the request context.
   *
   * @param request the current request context
   * @return the discovered SessionManager
   * @throws IllegalStateException if no SessionManager is found
   */
  public SessionManager obtain(RequestContext request) {
    SessionManager sessionManager = find(request);
    if (sessionManager == null) {
      throw new IllegalStateException("No SessionManager in context");
    }
    return sessionManager;
  }

}
