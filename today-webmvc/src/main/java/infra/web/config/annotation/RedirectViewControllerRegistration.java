/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.config.annotation;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.web.handler.mvc.ParameterizableViewController;
import infra.web.view.RedirectView;

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
   * <p>If not set, {@link infra.web.view.RedirectView}
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
