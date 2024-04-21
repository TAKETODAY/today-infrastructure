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

package cn.taketoday.web.view;

/**
 * Abstract base class for template view resolvers, in particular for FreeMarker views.
 *
 * <p>Provides a convenient way to specify {@link AbstractTemplateView}'s exposure
 * flags for request attributes, session attributes, and Framework's macro helpers.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractTemplateView
 * @see cn.taketoday.web.view.freemarker.FreeMarkerViewResolver
 * @since 4.0
 */
public abstract class AbstractTemplateViewResolver extends UrlBasedViewResolver {

  private boolean exposeRequestAttributes = false;

  private boolean allowRequestOverride = false;

  private boolean exposeSessionAttributes = false;

  private boolean allowSessionOverride = false;

  @Override
  protected Class<?> requiredViewClass() {
    return AbstractTemplateView.class;
  }

  /**
   * Set whether all request attributes should be added to the
   * model prior to merging with the template. Default is "false".
   *
   * @see AbstractTemplateView#setExposeRequestAttributes
   */
  public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
    this.exposeRequestAttributes = exposeRequestAttributes;
  }

  /**
   * Set whether HttpServletRequest attributes are allowed to override (hide)
   * controller generated model attributes of the same name. Default is "false",
   * which causes an exception to be thrown if request attributes of the same
   * name as model attributes are found.
   *
   * @see AbstractTemplateView#setAllowRequestOverride
   */
  public void setAllowRequestOverride(boolean allowRequestOverride) {
    this.allowRequestOverride = allowRequestOverride;
  }

  /**
   * Set whether all HttpSession attributes should be added to the
   * model prior to merging with the template. Default is "false".
   *
   * @see AbstractTemplateView#setExposeSessionAttributes
   */
  public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
    this.exposeSessionAttributes = exposeSessionAttributes;
  }

  /**
   * Set whether HttpSession attributes are allowed to override (hide)
   * controller generated model attributes of the same name. Default is "false",
   * which causes an exception to be thrown if session attributes of the same
   * name as model attributes are found.
   *
   * @see AbstractTemplateView#setAllowSessionOverride
   */
  public void setAllowSessionOverride(boolean allowSessionOverride) {
    this.allowSessionOverride = allowSessionOverride;
  }

  @Override
  protected AbstractUrlBasedView buildView(String viewName) throws Exception {
    AbstractTemplateView view = (AbstractTemplateView) super.buildView(viewName);
    view.setExposeRequestAttributes(exposeRequestAttributes);
    view.setAllowRequestOverride(allowRequestOverride);
    view.setExposeSessionAttributes(exposeSessionAttributes);
    view.setAllowSessionOverride(allowSessionOverride);
    return view;
  }

}
