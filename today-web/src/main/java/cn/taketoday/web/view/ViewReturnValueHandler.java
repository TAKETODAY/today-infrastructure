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

import java.util.Locale;
import java.util.Map;

import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
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
      Class<?> rawReturnType = handlerMethod.getRawReturnType();
      if (CharSequence.class.isAssignableFrom(rawReturnType)) {
        return !handlerMethod.isResponseBody();
      }

      if (returnValue == null) {
        if (ViewRef.class.isAssignableFrom(rawReturnType)
                || View.class.isAssignableFrom(rawReturnType)
                || ModelAndView.class.isAssignableFrom(rawReturnType)) {
          return true;
        }
      }
    }
    return supportsReturnValue(returnValue);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof CharSequence
            || returnValue instanceof View
            || returnValue instanceof ViewRef
            || returnValue instanceof ModelAndView;
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
    if (returnValue instanceof CharSequence viewName) {
      renderView(context, viewName);
    }
    else if (returnValue instanceof ViewRef viewRef) {
      renderView(context, viewRef);
    }
    else if (returnValue instanceof View view) {
      renderView(context, view);
    }
    else if (returnValue instanceof ModelAndView mv) {
      renderView(context, mv);
    }
  }

  // renderView

  /**
   * Resolve {@link ModelAndView} return type
   */
  public void renderView(RequestContext request, @Nullable ModelAndView mv) throws ViewRenderingException {
    if (mv != null) {
      if (mv.getStatus() != null) {
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, mv.getStatus());
        request.setStatus(mv.getStatus());
      }

      String viewName = mv.getViewName();
      if (viewName != null) {
        renderView(request, viewName, mv.getModel());
      }
      else {
        View view = mv.getView();
        if (view != null) {
          renderView(request, view, mv.getModel());
        }
      }
    }
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewName View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, CharSequence viewName) {
    renderView(context, viewName.toString());
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewName View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, String viewName) {
    renderView(context, viewName, null);
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewName View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, String viewName, @Nullable Map<String, Object> model) {
    View view = resolveViewName(context, viewName, null);
    renderView(context, view, model);
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewRef ViewRef to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, ViewRef viewRef) {
    View view = resolveViewName(context, viewRef.getViewName(), viewRef.getLocale());
    renderView(context, view);
  }

  /**
   * rendering a view from {@code viewName}
   *
   * @param context current HTTP request context
   * @param viewRef ViewRef to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, ViewRef viewRef, @Nullable Map<String, Object> model) {
    View view = resolveViewName(context, viewRef.getViewName(), viewRef.getLocale());
    renderView(context, view, model);
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

  /**
   * rendering a {@link View}
   *
   * @param context current HTTP request context
   * @param view View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, View view) {
    renderView(context, view, null);
  }

  /**
   * rendering a {@link View}
   *
   * @param context current HTTP request context
   * @param view View to render
   * @throws ViewRenderingException If view rendering failed
   */
  public void renderView(RequestContext context, View view, @Nullable Map<String, Object> model) {
    try {
      // do rendering
      view.render(model, context);
    }
    catch (Exception e) {
      throw new ViewRenderingException("View '%s' render failed".formatted(view), e);
    }
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
   * Returns LocaleResolver
   *
   * @return LocaleResolver
   */
  @Nullable
  public LocaleResolver getLocaleResolver() {
    return localeResolver;
  }

  /**
   * Returns ViewResolver
   *
   * @return ViewResolver
   */
  public ViewResolver getViewResolver() {
    return viewResolver;
  }

  private View resolveViewName(RequestContext context, String viewName, @Nullable Locale locale) {
    if (locale == null) {
      locale = getLocale(context);
    }

    View view = resolveViewName(locale, viewName);
    if (view == null) {
      HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        Object handler = matchingMetadata.getHandler();
        throw new ViewRenderingException(
                "Could not resolve view with name '%s' in handler '%s'".formatted(viewName, handler));
      }
      else {
        throw new ViewRenderingException("Could not resolve view with name '%s'".formatted(viewName));
      }
    }
    return view;
  }

  @Nullable
  private View resolveViewName(Locale locale, String viewName) {
    try {
      return viewResolver.resolveViewName(viewName, locale);
    }
    catch (Exception e) {
      throw new ViewRenderingException("Could not resolve view with name '%s'".formatted(viewName), e);
    }
  }

}
