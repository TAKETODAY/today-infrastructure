/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

import infra.core.i18n.LocaleContextHolder;
import infra.lang.Assert;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMatchingMetadata;
import infra.web.LocaleResolver;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.SmartReturnValueHandler;

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
  public boolean supportsHandler(@Nullable Object handler, @Nullable Object returnValue) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      Class<?> rawReturnType = handlerMethod.getRawReturnType();
      if (returnValue == null) {
        return ViewRef.class.isAssignableFrom(rawReturnType)
                || View.class.isAssignableFrom(rawReturnType)
                || ModelAndView.class.isAssignableFrom(rawReturnType);
      }

      // check return value
      if (rawReturnType.isInstance(returnValue)
              && CharSequence.class.isAssignableFrom(rawReturnType)) {
        return !handlerMethod.isResponseBody();
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
    else if (returnValue != null) {
      throw new ViewRenderingException("Unsupported render result [%s] as view".formatted(returnValue));
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
    renderView(context, viewRef, null);
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
   * {@link RequestContext#getLocale()} method,
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
