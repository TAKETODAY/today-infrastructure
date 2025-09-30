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

package infra.web.server.error;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import infra.context.ApplicationContext;
import infra.core.Ordered;
import infra.core.io.Resource;
import infra.http.HttpStatus;
import infra.http.HttpStatus.Series;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.ui.template.TemplateAvailabilityProviders;
import infra.util.FileCopyUtils;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;
import infra.web.view.View;

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

  private final String[] staticLocations;

  private final TemplateAvailabilityProviders templateAvailabilityProviders;

  private int order = Ordered.LOWEST_PRECEDENCE;

  /**
   * Create a new {@link DefaultErrorViewResolver} instance.
   *
   * @param applicationContext the source application context
   * @param staticLocations staticLocations properties
   */
  public DefaultErrorViewResolver(ApplicationContext applicationContext, String[] staticLocations) {
    this(applicationContext, staticLocations, new TemplateAvailabilityProviders(applicationContext));
  }

  public DefaultErrorViewResolver(ApplicationContext applicationContext, String[] staticLocations, TemplateAvailabilityProviders providers) {
    Assert.notNull(staticLocations, "staticLocations is required");
    Assert.notNull(applicationContext, "ApplicationContext is required");
    this.applicationContext = applicationContext;
    this.staticLocations = staticLocations;
    this.templateAvailabilityProviders = providers;
  }

  @Nullable
  @Override
  public ModelAndView resolveErrorView(RequestContext request, HttpStatusCode status, Map<String, Object> model) {
    ModelAndView view = resolve(String.valueOf(status.value()), model);
    if (view == null) {
      HttpStatus.Series series = HttpStatus.Series.resolve(status.value());
      if (SERIES_VIEWS.containsKey(series)) {
        view = resolve(SERIES_VIEWS.get(series), model);
      }
    }
    return view;
  }

  @Nullable
  private ModelAndView resolve(String viewName, Map<String, Object> model) {
    String errorViewName = "error/" + viewName;
    var provider = templateAvailabilityProviders.getProvider(errorViewName, applicationContext);
    if (provider != null) {
      return new ModelAndView(errorViewName, model);
    }
    return resolveResource(errorViewName, model);
  }

  @Nullable
  private ModelAndView resolveResource(String viewName, Map<String, Object> model) {
    for (String location : staticLocations) {
      try {
        Resource resource = applicationContext.getResource(location);
        resource = resource.createRelative(viewName + ".html");
        if (resource.exists()) {
          return new ModelAndView(new HtmlResourceView(resource), model);
        }
      }
      catch (Exception ignored) {
      }
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
    public void render(@Nullable Map<String, ?> model, RequestContext request) throws Exception {
      request.setContentType(getContentType());
      FileCopyUtils.copy(resource.getInputStream(), request.getOutputStream());
    }

  }

}
