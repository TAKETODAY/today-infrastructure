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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.registry.SimpleUrlHandlerMapping;

/**
 * Assists with the registration of simple automated controllers pre-configured
 * with status code and/or a view.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 22:20
 */
public class ViewControllerRegistry {

  @Nullable
  private final ApplicationContext applicationContext;

  private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

  private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

  private int order = 1;

  /**
   * Class constructor with {@link ApplicationContext}.
   */
  public ViewControllerRegistry(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Map a URL path or pattern to a view controller to render a response with
   * the configured status code and view.
   * <p>Patterns such as {@code "/admin/**"} or {@code "/articles/{articlename:\\w+}"}
   * are supported.
   *
   * <p><strong>Note:</strong> If an {@code @RequestMapping} method is mapped
   * to a URL for any HTTP method then a view controller cannot handle the
   * same URL. For this reason it is recommended to avoid splitting URL
   * handling across an annotated controller and a view controller.
   */
  public ViewControllerRegistration addViewController(String urlPathOrPattern) {
    ViewControllerRegistration registration = new ViewControllerRegistration(urlPathOrPattern);
    registration.setApplicationContext(this.applicationContext);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Map a view controller to the given URL path or pattern in order to redirect
   * to another URL.
   *
   * <p>By default the redirect URL is expected to be relative to the current
   * ServletContext, i.e. as relative to the web application root.
   */
  public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
    RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
    registration.setApplicationContext(this.applicationContext);
    this.redirectRegistrations.add(registration);
    return registration;
  }

  /**
   * Map a simple controller to the given URL path (or pattern) in order to
   * set the response status to the given code without rendering a body.
   */
  public void addStatusController(String urlPath, HttpStatusCode statusCode) {
    ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
    registration.setApplicationContext(this.applicationContext);
    registration.setStatusCode(statusCode);
    registration.getViewController().setStatusOnly(true);
    this.registrations.add(registration);
  }

  /**
   * Specify the order to use for the {@code HandlerMapping} used to map view
   * controllers relative to other handler mappings configured in Framework MVC.
   * <p>By default this is set to 1, i.e. right after annotated controllers,
   * which are ordered at 0.
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Return the {@code HandlerMapping} that contains the registered view
   * controller mappings, or {@code null} for no registrations.
   */
  @Nullable
  protected SimpleUrlHandlerMapping buildHandlerMapping() {
    if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
      return null;
    }

    LinkedHashMap<String, Object> urlMap = new LinkedHashMap<>();
    for (ViewControllerRegistration registration : this.registrations) {
      urlMap.put(registration.getUrlPath(), registration.getViewController());
    }
    for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
      urlMap.put(registration.getUrlPath(), registration.getViewController());
    }

    return new SimpleUrlHandlerMapping(urlMap, this.order);
  }

}
