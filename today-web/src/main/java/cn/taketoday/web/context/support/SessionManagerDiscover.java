/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.context.support;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;

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
            if (sessionManager == null) {
              throw new IllegalStateException("No SessionManager in context");
            }
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
