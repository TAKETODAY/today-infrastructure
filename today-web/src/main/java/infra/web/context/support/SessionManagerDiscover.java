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
