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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMatchingMetadata;
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

  private boolean putAllOutputRedirectModel = true;

  public ViewReturnValueHandler(ViewResolver viewResolver) {
    Assert.notNull(viewResolver, "viewResolver is required");
    this.viewResolver = viewResolver;
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
    Locale locale = RequestContextUtils.getLocale(context);
    renderView(context, ViewRef.of(viewName, locale));
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
      locale = RequestContextUtils.getLocale(context);
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
   * Returns ViewResolver
   *
   * @return ViewResolver
   */
  public ViewResolver getViewResolver() {
    return viewResolver;
  }

}
