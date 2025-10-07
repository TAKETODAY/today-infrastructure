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

package infra.web.view;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.session.Session;
import infra.session.SessionManager;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;

/**
 * Adapter base class for template-based view technologies such as FreeMarker,
 * with the ability to use request and session attributes in their model and
 * the option to expose helper objects for Framework's FreeMarker macro library.
 *
 * <p>JSP/JSTL and other view technologies automatically have access to the
 * HttpRequest object and thereby the request/session attributes
 * for the current user. Furthermore, they are able to create and cache
 * helper objects as request attributes themselves.
 *
 * @author Juergen Hoeller
 * @author Darren Davison
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractTemplateViewResolver
 * @see infra.web.view.freemarker.FreeMarkerView
 * @since 4.0
 */
public abstract class AbstractTemplateView extends AbstractUrlBasedView {

  private boolean exposeRequestAttributes = false;

  private boolean allowRequestOverride = false;

  private boolean exposeSessionAttributes = false;

  private boolean allowSessionOverride = false;

  /**
   * Set whether all request attributes should be added to the
   * model prior to merging with the template. Default is "false".
   * <p>Note that some templates may make request attributes visible
   * on their own, e.g. FreeMarker, without exposure in the MVC model.
   */
  public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
    this.exposeRequestAttributes = exposeRequestAttributes;
  }

  /**
   * Set whether Request attributes are allowed to override (hide)
   * controller generated model attributes of the same name. Default is "false"
   * which causes an exception to be thrown if request attributes of the same
   * name as model attributes are found.
   */
  public void setAllowRequestOverride(boolean allowRequestOverride) {
    this.allowRequestOverride = allowRequestOverride;
  }

  /**
   * Set whether all HttpSession attributes should be added to the
   * model prior to merging with the template. Default is "false".
   */
  public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
    this.exposeSessionAttributes = exposeSessionAttributes;
  }

  /**
   * Set whether HttpSession attributes are allowed to override (hide)
   * controller generated model attributes of the same name. Default is "false",
   * which causes an exception to be thrown if session attributes of the same
   * name as model attributes are found.
   */
  public void setAllowSessionOverride(boolean allowSessionOverride) {
    this.allowSessionOverride = allowSessionOverride;
  }

  @Override
  protected final void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
    if (exposeRequestAttributes) {
      Map<String, Object> exposed = null;
      for (String attribute : request.attributeNames()) {
        if (model.containsKey(attribute) && !allowRequestOverride) {
          throw new ViewRenderingException("Cannot expose request attribute '%s' because of an existing model object of the same name"
                  .formatted(attribute));
        }
        Object attributeValue = request.getAttribute(attribute);
        if (logger.isDebugEnabled()) {
          exposed = exposed != null ? exposed : new LinkedHashMap<>();
          exposed.put(attribute, attributeValue);
        }
        model.put(attribute, attributeValue);
      }
      if (exposed != null && logger.isTraceEnabled()) {
        logger.trace("Exposed request attributes to model: {}", exposed);
      }
    }

    if (exposeSessionAttributes) {
      exposeSessionAttributes(model, request);
    }

    applyContentType(request);

    if (logger.isDebugEnabled()) {
      logger.debug("Rendering [{}]", getUrl());
    }

    renderMergedTemplateModel(model, request);
  }

  private void exposeSessionAttributes(Map<String, Object> model, RequestContext context) {
    SessionManager sessionManager = RequestContextUtils.getSessionManager(context);
    if (sessionManager != null) {
      Session session = sessionManager.getSession(context, false);
      if (session != null) {
        Map<String, Object> exposed = null;
        String[] attributeNames = session.getAttributeNames();
        for (String attribute : attributeNames) {
          if (model.containsKey(attribute) && !allowSessionOverride) {
            throw new ViewRenderingException("Cannot expose session attribute '%s' because of an existing model object of the same name"
                    .formatted(attribute));
          }
          Object attributeValue = session.getAttribute(attribute);
          if (logger.isDebugEnabled()) {
            exposed = exposed != null ? exposed : new LinkedHashMap<>();
            exposed.put(attribute, attributeValue);
          }
          model.put(attribute, attributeValue);
        }
        if (logger.isTraceEnabled() && exposed != null) {
          logger.trace("Exposed session attributes to model: {}", exposed);
        }
      }
    }
  }

  /**
   * Apply this view's content type as specified in the "contentType"
   * bean property to the given response.
   * <p>Only applies the view's contentType if no content type has been
   * set on the response before. This allows handlers to override the
   * default content type beforehand.
   *
   * @param response current HTTP response
   * @see #setContentType
   */
  protected void applyContentType(RequestContext response) {
    if (response.getResponseContentType() == null) {
      response.setContentType(getContentType());
    }
  }

  /**
   * Subclasses must implement this method to actually render the view.
   *
   * @param model combined output Map, with request attributes and
   * session attributes merged into it if required
   * @param context current HTTP request
   * @throws Exception if rendering failed
   */
  protected abstract void renderMergedTemplateModel(Map<String, Object> model, RequestContext context) throws Exception;

}
