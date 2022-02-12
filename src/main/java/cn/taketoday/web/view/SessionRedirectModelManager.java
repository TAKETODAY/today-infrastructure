/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.view;

import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionManager;
import cn.taketoday.web.util.WebUtils;

/**
 * Store {@link RedirectModel} in {@link cn.taketoday.web.session.WebSession}
 *
 * @author TODAY 2021/4/2 21:55
 * @since 3.0
 */
public class SessionRedirectModelManager extends AbstractRedirectModelManager implements RedirectModelManager {
  private static final String SESSION_ATTRIBUTE = SessionRedirectModelManager.class.getName() + ".RedirectModel";

  @Nullable
  private WebSessionManager sessionManager;

  public SessionRedirectModelManager() { }

  public SessionRedirectModelManager(@Nullable WebSessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
    WebSession session = getSession(request, false);
    if (session != null) {
      return (List<RedirectModel>) session.getAttribute(SESSION_ATTRIBUTE);
    }
    return null;
  }

  @Override
  protected void updateRedirectModel(List<RedirectModel> redirectModel, RequestContext request) {
    if (CollectionUtils.isEmpty(redirectModel)) {
      WebSession session = getSession(request, false);
      if (session != null) {
        session.removeAttribute(SESSION_ATTRIBUTE);
      }
    }
    else {
      WebSession session = getSession(request, true);
      Assert.state(session != null, "WebSession not found in current request");
      session.setAttribute(SESSION_ATTRIBUTE, redirectModel);
    }
  }

  @Nullable
  @Override
  protected Object getRedirectModelMutex(RequestContext request) {
    WebSession session = getSession(request, false);
    if (session != null) {
      return WebUtils.getSessionMutex(session);
    }
    return null;
  }

  @Nullable
  public WebSessionManager getSessionManager() {
    return sessionManager;
  }

  @Nullable
  private WebSession getSession(RequestContext context, boolean create) {
    WebSessionManager sessionManager = getSessionManager();
    if (sessionManager == null) {
      return RequestContextUtils.getSession(context, create);
    }
    return sessionManager.getSession(context, create);
  }

}
