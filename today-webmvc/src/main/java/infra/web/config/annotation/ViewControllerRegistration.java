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

import java.util.function.Supplier;

import infra.context.ApplicationContext;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.web.HttpRequestHandler;
import infra.web.RequestToViewNameTranslator;
import infra.web.handler.mvc.ParameterizableViewController;
import infra.web.view.DefaultRequestToViewNameTranslator;

/**
 * Assist with the registration of a single view controller.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 22:21
 */
public class ViewControllerRegistration {

  private final String urlPath;

  private final ParameterizableViewController controller = new ParameterizableViewController();

  public ViewControllerRegistration(String urlPath) {
    Assert.notNull(urlPath, "'urlPath' is required.");
    this.urlPath = urlPath;
  }

  /**
   * Set the status code to set on the response. Optional.
   * <p>If not set the response status will be 200 (OK).
   */
  public ViewControllerRegistration setStatusCode(HttpStatusCode statusCode) {
    this.controller.setStatusCode(statusCode);
    return this;
  }

  /**
   * Set the view name to return. Optional.
   * <p>If not specified, the view controller will return {@code null} as the
   * view name in which case the configured {@link RequestToViewNameTranslator}
   * will select the view name. The {@code DefaultRequestToViewNameTranslator}
   * for example translates "/foo/bar" to "foo/bar".
   *
   * @see DefaultRequestToViewNameTranslator
   */
  public ViewControllerRegistration setViewName(@Nullable String viewName) {
    this.controller.setViewName(viewName);
    return this;
  }

  /**
   * Set the content-type to return. Optional.
   */
  public ViewControllerRegistration setContentType(String contentType) {
    this.controller.setContentType(contentType);
    return this;
  }

  /**
   * Set the result
   */
  public ViewControllerRegistration setReturnValue(Object returnValue) {
    this.controller.setReturnValue(returnValue);
    return this;
  }

  public ViewControllerRegistration setReturnValue(Supplier<Object> objectSupplier) {
    this.controller.setReturnValue(objectSupplier);
    return this;
  }

  public ViewControllerRegistration setReturnValue(HttpRequestHandler handler) {
    this.controller.setReturnValue(handler);
    return this;
  }

  protected void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.controller.setApplicationContext(applicationContext);
  }

  protected String getUrlPath() {
    return this.urlPath;
  }

  protected ParameterizableViewController getViewController() {
    return this.controller;
  }

}

