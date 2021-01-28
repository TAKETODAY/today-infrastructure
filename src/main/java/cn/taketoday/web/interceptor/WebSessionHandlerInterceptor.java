/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.interceptor;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionManager;

/**
 * @author TODAY <br>
 * 2020-04-20 20:58
 */
public abstract class WebSessionHandlerInterceptor implements HandlerInterceptor {

  private final WebSessionManager sessionManager;

  public WebSessionHandlerInterceptor(WebSessionManager sessionManager) {
    Assert.notNull(sessionManager, "sessionManager must not be null");
    this.sessionManager = sessionManager;
  }

  public WebSessionManager getSessionManager() {
    return sessionManager;
  }

  public WebSession getSession(RequestContext context) {
    return getSessionManager().getSession(context);
  }

  public Object getAttribute(WebSession session, String name) {
    return session.getAttribute(name);
  }

  public Object getAttribute(RequestContext context, String name) {
    return getAttribute(getSession(context), name);
  }

}
