/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.config;

import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestToViewNameTranslator;
import cn.taketoday.web.handler.mvc.ParameterizableViewController;
import cn.taketoday.web.view.DefaultRequestToViewNameTranslator;

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

