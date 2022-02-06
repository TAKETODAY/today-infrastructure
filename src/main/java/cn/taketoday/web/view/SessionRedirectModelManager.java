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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionManager;

/**
 * Store {@link RedirectModel} in {@link cn.taketoday.web.session.WebSession}
 *
 * @author TODAY 2021/4/2 21:55
 * @since 3.0
 */
public class SessionRedirectModelManager implements RedirectModelManager {
  private final WebSessionManager sessionManager;

  public SessionRedirectModelManager(WebSessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Nullable
  @Override
  public RedirectModel getModel(RequestContext context) {
    WebSession session = sessionManager.getSession(context, false);
    if (session != null) {
      Object attribute = session.getAttribute(KEY_REDIRECT_MODEL);
      if (attribute instanceof RedirectModel) {
        return (RedirectModel) attribute;
      }
    }
    return null;
  }

  @Override
  public void saveRedirectModel(RequestContext context, @Nullable RedirectModel redirectModel) {
    if (redirectModel == null) {
      WebSession session = sessionManager.getSession(context, false);
      if (session != null) {
        session.removeAttribute(KEY_REDIRECT_MODEL);
      }
    }
    else {
      WebSession session = sessionManager.getSession(context);
      Assert.state(session != null, "WebSession not found in current request");
      session.setAttribute(KEY_REDIRECT_MODEL, redirectModel);
    }
  }
}
