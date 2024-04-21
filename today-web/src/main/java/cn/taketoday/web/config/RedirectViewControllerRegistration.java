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

package cn.taketoday.web.config;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.mvc.ParameterizableViewController;
import cn.taketoday.web.view.RedirectView;

/**
 * Assist with the registration of a single redirect view controller.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:44
 */
public class RedirectViewControllerRegistration {

  private final String urlPath;

  private final RedirectView redirectView;

  private final ParameterizableViewController controller = new ParameterizableViewController();

  public RedirectViewControllerRegistration(String urlPath, String redirectUrl) {
    Assert.notNull(urlPath, "'urlPath' is required.");
    Assert.notNull(redirectUrl, "'redirectUrl' is required.");
    this.urlPath = urlPath;
    this.redirectView = new RedirectView(redirectUrl);
    this.controller.setView(this.redirectView);
  }

  /**
   * Set the specific redirect 3xx status code to use.
   * <p>If not set, {@link cn.taketoday.web.view.RedirectView}
   * will select {@code HttpStatus.MOVED_TEMPORARILY (302)} by default.
   */
  public RedirectViewControllerRegistration setStatusCode(HttpStatusCode statusCode) {
    Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
    this.redirectView.setStatusCode(statusCode);
    return this;
  }

  /**
   * Whether to propagate the query parameters of the current request through
   * to the target redirect URL.
   * <p>Default is {@code false}.
   */
  public RedirectViewControllerRegistration setKeepQueryParams(boolean propagate) {
    this.redirectView.setPropagateQueryParams(propagate);
    return this;
  }

  protected void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.controller.setApplicationContext(applicationContext);
    this.redirectView.setApplicationContext(applicationContext);
  }

  protected String getUrlPath() {
    return this.urlPath;
  }

  protected ParameterizableViewController getViewController() {
    return this.controller;
  }

}
