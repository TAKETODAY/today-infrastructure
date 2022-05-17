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

package cn.taketoday.web.bind.support;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.session.SessionManager;
import cn.taketoday.web.session.WebSession;

/**
 * Default implementation of the {@link SessionAttributeStore} interface,
 * storing the attributes in the RequestContext session (i.e. HttpSession).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setAttributeNamePrefix
 * @see cn.taketoday.web.session.WebSession#setAttribute
 * @see WebSession#getAttribute
 * @see WebSession#removeAttribute
 * @since 4.0 2022/4/8 23:20
 */
public class DefaultSessionAttributeStore implements SessionAttributeStore {

  private String attributeNamePrefix = "";

  @Nullable
  private SessionManager sessionManager;

  /**
   * Specify a prefix to use for the attribute names in the backend session.
   * <p>Default is to use no prefix, storing the session attributes with the
   * same name as in the model.
   */
  public void setAttributeNamePrefix(@Nullable String attributeNamePrefix) {
    this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
  }

  @Override
  public void storeAttribute(RequestContext request, String attributeName, Object attributeValue) {
    Assert.notNull(request, "RequestContext must not be null");
    Assert.notNull(attributeName, "Attribute name must not be null");
    Assert.notNull(attributeValue, "Attribute value must not be null");
    String storeAttributeName = getAttributeNameInSession(request, attributeName);

    getSession(request).setAttribute(storeAttributeName, attributeValue);
  }

  @Override
  @Nullable
  public Object retrieveAttribute(RequestContext request, String attributeName) {
    Assert.notNull(request, "RequestContext must not be null");
    Assert.notNull(attributeName, "Attribute name must not be null");
    String storeAttributeName = getAttributeNameInSession(request, attributeName);
    return getSession(request).getAttribute(storeAttributeName);
  }

  @Override
  public void cleanupAttribute(RequestContext request, String attributeName) {
    Assert.notNull(request, "RequestContext must not be null");
    Assert.notNull(attributeName, "Attribute name must not be null");
    String storeAttributeName = getAttributeNameInSession(request, attributeName);
    getSession(request).removeAttribute(storeAttributeName);
  }

  /**
   * Calculate the attribute name in the backend session.
   * <p>The default implementation simply prepends the configured
   * {@link #setAttributeNamePrefix "attributeNamePrefix"}, if any.
   *
   * @param request the current request
   * @param attributeName the name of the attribute
   * @return the attribute name in the backend session
   */
  protected String getAttributeNameInSession(RequestContext request, String attributeName) {
    return this.attributeNamePrefix + attributeName;
  }

  private WebSession getSession(RequestContext request) {
    WebSession session = null;
    if (sessionManager != null) {
      session = sessionManager.getSession(request);
    }
    if (session == null) {
      session = RequestContextUtils.getSession(request);
    }
    Assert.state(session != null, "No session");
    return session;
  }

  public void setSessionManager(@Nullable SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

}
