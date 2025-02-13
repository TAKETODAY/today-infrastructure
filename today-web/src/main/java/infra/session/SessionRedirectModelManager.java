/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.session;

import java.util.List;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.web.AbstractRedirectModelManager;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.util.WebUtils;

/**
 * Store {@link RedirectModel} in {@link WebSession}
 *
 * @author TODAY 2021/4/2 21:55
 * @since 3.0
 */
public class SessionRedirectModelManager extends AbstractRedirectModelManager implements RedirectModelManager {

  private static final String SESSION_ATTRIBUTE = SessionRedirectModelManager.class.getName() + ".RedirectModel";

  @Nullable
  private SessionManager sessionManager;

  public SessionRedirectModelManager() { }

  public SessionRedirectModelManager(@Nullable SessionManager sessionManager) {
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
  protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {
    if (CollectionUtils.isEmpty(redirectModels)) {
      WebSession session = getSession(request, false);
      if (session != null) {
        session.removeAttribute(SESSION_ATTRIBUTE);
      }
    }
    else {
      WebSession session = getSession(request, true);
      Assert.state(session != null, "WebSession not found in current request");
      session.setAttribute(SESSION_ATTRIBUTE, redirectModels);
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
  public SessionManager getSessionManager() {
    return sessionManager;
  }

  @Nullable
  private WebSession getSession(RequestContext context, boolean create) {
    SessionManager sessionManager = getSessionManager();
    if (sessionManager == null) {
      return RequestContextUtils.getSession(context, create);
    }
    return sessionManager.getSession(context, create);
  }

}
