/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.error;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import cn.taketoday.annotation.config.web.WebProperties.Resources;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.template.TemplateAvailabilityProviders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatus.Series;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.View;

/**
 * Default {@link ErrorViewResolver} implementation that attempts to resolve error views
 * using well known conventions. Will search for templates and static assets under
 * {@code '/error'} using the {@link HttpStatus status code} and the
 * {@link HttpStatus#series() status series}.
 * <p>
 * For example, an {@code HTTP 404} will search (in the specific order):
 * <ul>
 * <li>{@code '/<templates>/error/404.<ext>'}</li>
 * <li>{@code '/<static>/error/404.html'}</li>
 * <li>{@code '/<templates>/error/4xx.<ext>'}</li>
 * <li>{@code '/<static>/error/4xx.html'}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultErrorViewResolver implements ErrorViewResolver, Ordered {

  private static final Map<Series, String> SERIES_VIEWS;

  static {
    Map<Series, String> views = new EnumMap<>(Series.class);
    views.put(Series.CLIENT_ERROR, "4xx");
    views.put(Series.SERVER_ERROR, "5xx");
    SERIES_VIEWS = Collections.unmodifiableMap(views);
  }

  private final ApplicationContext applicationContext;

  private final Resources resources;

  private final TemplateAvailabilityProviders templateAvailabilityProviders;

  private int order = Ordered.LOWEST_PRECEDENCE;

  /**
   * Create a new {@link DefaultErrorViewResolver} instance.
   *
   * @param applicationContext the source application context
   * @param resources resource properties
   */
  public DefaultErrorViewResolver(ApplicationContext applicationContext, Resources resources) {
    Assert.notNull(applicationContext, "ApplicationContext is required");
    Assert.notNull(resources, "Resources is required");
    this.applicationContext = applicationContext;
    this.resources = resources;
    this.templateAvailabilityProviders = new TemplateAvailabilityProviders(applicationContext);
  }

  DefaultErrorViewResolver(ApplicationContext applicationContext,
          Resources resourceProperties, TemplateAvailabilityProviders providers) {
    Assert.notNull(applicationContext, "ApplicationContext is required");
    Assert.notNull(resourceProperties, "Resources is required");
    this.applicationContext = applicationContext;
    this.resources = resourceProperties;
    this.templateAvailabilityProviders = providers;
  }

  @Override
  public Object resolveErrorView(RequestContext request, HttpStatus status, Map<String, Object> model) {
    Object view = resolve(String.valueOf(status.value()), model);
    if (view == null && SERIES_VIEWS.containsKey(status.series())) {
      view = resolve(SERIES_VIEWS.get(status.series()), model);
    }
    return view;
  }

  private ModelAndView resolve(String viewName, Map<String, Object> model) {
    String errorViewName = "error/" + viewName;
    var provider = templateAvailabilityProviders.getProvider(errorViewName, applicationContext);
    if (provider != null) {
      return new ModelAndView(errorViewName, model);
    }
    return resolveResource(errorViewName, model);
  }

  private ModelAndView resolveResource(String viewName, Map<String, Object> model) {
    for (String location : resources.getStaticLocations()) {
      try {
        Resource resource = applicationContext.getResource(location);
        resource = resource.createRelative(viewName + ".html");
        if (resource.exists()) {
          return new ModelAndView(new HtmlResourceView(resource), model);
        }
      }
      catch (Exception ignored) { }
    }
    return null;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * {@link View} backed by an HTML resource.
   */
  private static class HtmlResourceView implements View {

    private final Resource resource;

    HtmlResourceView(Resource resource) {
      this.resource = resource;
    }

    @Override
    public String getContentType() {
      return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> model, RequestContext request) throws Exception {
      request.setContentType(getContentType());
      FileCopyUtils.copy(resource.getInputStream(), request.getOutputStream());
    }

  }

}
