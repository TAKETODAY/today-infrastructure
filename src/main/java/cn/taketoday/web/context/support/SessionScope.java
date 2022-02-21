/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.function.Supplier;

import cn.taketoday.beans.factory.Scope;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionManager;
import cn.taketoday.web.util.WebUtils;

/**
 * Session-backed {@link Scope} implementation.
 *
 * <p>Relies on a thread-bound {@link RequestContext} instance
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebSessionManager
 * @since 4.0 2022/2/21 11:40
 */
public class SessionScope extends AbstractRequestContextScope {
  private final WebSessionManager webSessionManager;

  public SessionScope(WebSessionManager webSessionManager) {
    Assert.notNull(webSessionManager, "webSessionManager is required");
    this.webSessionManager = webSessionManager;
  }

  @Override
  public String getConversationId() {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = webSessionManager.getSession(context, false);
    if (session != null) {
      return session.getId();
    }
    return null;
  }

  @Override
  public Object get(String name, Supplier<?> objectFactory) {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = webSessionManager.getSession(context, false);
    if (session != null) {
      Object sessionMutex = WebUtils.getSessionMutex(session);
      synchronized(sessionMutex) {
        return super.get(name, objectFactory);
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Object remove(String name) {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = webSessionManager.getSession(context, false);
    if (session != null) {
      Object sessionMutex = WebUtils.getSessionMutex(session);
      synchronized(sessionMutex) {
        return super.remove(context, name);
      }
    }
    return null;
  }

}
