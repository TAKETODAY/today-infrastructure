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
 * Help to find SessionManager in BeanFactory and request-context
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/30 17:41
 */
public class SessionManagerDiscover {

  private final BeanFactory beanFactory;

  @Nullable
  private volatile SessionManager sessionManager;

  private volatile boolean managerLoaded = false;

  public SessionManagerDiscover(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    this.beanFactory = beanFactory;
  }

  /**
   * Obtain SessionManager in {@link #beanFactory}
   */
  @Nullable
  public SessionManager find() {
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
   * Obtain SessionManager
   */
  public SessionManager obtain(RequestContext request) {
    SessionManager sessionManager = find();
    if (sessionManager == null) {
      sessionManager = RequestContextUtils.getSessionManager(request);
      if (sessionManager == null) {
        throw new IllegalStateException("No SessionManager in context");
      }
    }
    return sessionManager;
  }

}
