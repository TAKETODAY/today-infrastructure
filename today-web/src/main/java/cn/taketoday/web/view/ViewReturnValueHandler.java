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

package cn.taketoday.web.view;

import java.util.LinkedHashMap;
import java.util.Locale;

import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;

/**
 * view-name or {@link View} and {@link ViewRef} ReturnValueHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see View
 * @see RedirectModel
 * @see RedirectModelManager
 * @see HandlerMethod
 * @since 4.0 2022/2/9 20:34
 */
public class ViewReturnValueHandler implements SmartReturnValueHandler {

  private final ViewResolver viewResolver;

  @Nullable
  private LocaleResolver localeResolver;

  private boolean putAllOutputRedirectModel = true;

  public ViewReturnValueHandler(ViewResolver viewResolver) {
    this(viewResolver, null);
  }

  public ViewReturnValueHandler(ViewResolver viewResolver, @Nullable LocaleResolver localeResolver) {
    Assert.notNull(viewResolver, "viewResolver is required");
    this.viewResolver = viewResolver;
    this.localeResolver = localeResolver;
  }

  @Override
  public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      if (handlerMethod.isReturn(String.class)) {
        return !handlerMethod.isResponseBody();
      }
      return handlerMethod.isReturnTypeAssignableTo(ViewRef.class)
              || handlerMethod.isReturnTypeAssignableTo(View.class);
    }
    else {
      return supportsReturnValue(returnValue);
    }
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof String
            || returnValue instanceof View
            || returnValue instanceof ViewRef;
  }

  /**
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws ViewRenderingException Could not resolve view with given name
   */
  @Override
  public void handleReturnValue(RequestContext context,
          @Nullable Object handler, @Nullable Object returnValue) throws ViewRenderingException {
    if (returnValue instanceof String viewName) {
      renderView(context, viewName);
    }
    else if (returnValue instanceof ViewRef viewRef) {
      renderView(context, viewRef);
    }
    else if (returnValue instanceof View view) {
      renderView(context, view);
    }
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewName View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, String viewName) {
    renderView(context, ViewRef.of(viewName));
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewRef ViewRef to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, ViewRef viewRef) {
    Locale locale = viewRef.getLocale();
    String viewName = viewRef.getViewName();
    if (locale == null) {
      locale = getLocale(context);
    }

    View view = resolveViewName(locale, viewName);
    if (view == null) {
      HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        Object handler = matchingMetadata.getHandler();
        throw new ViewRenderingException(
                "Could not resolve view with name '" + viewName + "' in handler '" + handler + "'");
      }
      else {
        throw new ViewRenderingException(
                "Could not resolve view with name '" + viewName + "'");
      }
    }
    else {
      renderView(context, view);
    }
  }

  /**
   * Retrieve the current locale from the given request, using the
   * LocaleResolver bound to the request by the Ioc
   * (if available), falling back to the request's accept-header Locale.
   * <p>This method serves as a straightforward alternative to the standard
   * Servlet {@link jakarta.servlet.http.HttpServletRequest#getLocale()} method,
   * falling back to the latter if no more specific locale has been found.
   * <p>Consider using {@link LocaleContextHolder#getLocale()}
   * which will normally be populated with the same Locale.
   *
   * @param request current HTTP request
   * @return the current locale for the given request, either from the
   * LocaleResolver or from the plain request itself
   */
  private Locale getLocale(RequestContext request) {
    LocaleResolver localeResolver = this.localeResolver;
    if (localeResolver == null) {
      localeResolver = RequestContextUtils.getLocaleResolver(request);
    }

    if (localeResolver != null) {
      return localeResolver.resolveLocale(request);
    }
    return request.getLocale();
  }

  @Nullable
  private View resolveViewName(Locale locale, String viewName) {
    try {
      return viewResolver.resolveViewName(viewName, locale);
    }
    catch (Exception e) {
      throw new ViewRenderingException("Could not resolve view with name '" + viewName + "'", e);
    }
  }

  /**
   * rendering a {@link View}
   *
   * @param context current HTTP request context
   * @param view View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, View view) {
    LinkedHashMap<String, Object> model = new LinkedHashMap<>();

    if (putAllOutputRedirectModel) {
      // put all output RedirectModel
      RedirectModel output = RequestContextUtils.getOutputRedirectModel(context);
      if (output != null) {
        model.putAll(output.asMap());
      }
    }

    BindingContext binding = context.getBinding();
    if (binding != null && binding.hasModel()) {
      // injected arguments Model
      model.putAll(binding.getModel());
    }
    try {
      // do rendering
      view.render(model, context);
    }
    catch (Exception e) {
      throw new ViewRenderingException("View '" + view + "' render failed", e);
    }
  }

  /**
   * set {@link #putAllOutputRedirectModel} to determine if all 'output'
   * RedirectModel should be put into model
   *
   * @param putAllOutputRedirectModel If true, all 'output' RedirectModel
   * will be put to current view
   * @see RequestContextUtils#getOutputRedirectModel(RequestContext)
   */
  public void setPutAllOutputRedirectModel(boolean putAllOutputRedirectModel) {
    this.putAllOutputRedirectModel = putAllOutputRedirectModel;
  }

  /**
   * set {@link #localeResolver} to determine the Locale in which
   * to resolve the view. ViewResolvers that support internationalization
   * should respect this.
   * <p>
   * If {@link #localeResolver} is null use {@link RequestContextUtils#getLocale(RequestContext)}
   * to find Locale
   *
   * @param localeResolver to determine the Locale in which to resolve the view
   */
  public void setLocaleResolver(@Nullable LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  /**
   * Returns ViewResolver
   *
   * @return ViewResolver
   */
  public ViewResolver getViewResolver() {
    return viewResolver;
  }

}
