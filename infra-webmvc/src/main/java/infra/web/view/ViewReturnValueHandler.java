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

import infra.util.Assert;
import infra.web.BindingContext;
import infra.web.HandlerMatchingMetadata;
import infra.web.LocaleResolver;
import infra.web.ServerHttpExchange;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.SmartReturnValueHandler;
import infra.web.util.WebUtils;

/**
 * Renders view names ({@link CharSequence}), {@link ViewRef} references,
 * {@link View} instances, and {@link ModelAndView} results.
 *
 * <p>Named views are resolved through a {@link ViewResolver}. A locale supplied
 * by a {@code ViewRef} takes precedence over the request locale. Existing
 * {@code View} instances are rendered directly without name resolution.
 *
 * <p>Before rendering, the handler updates the request's {@link BindingContext},
 * if present, and merges its model into any supplied model. Binding context
 * attributes take precedence over entries with the same name.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ViewResolver
 * @see ViewRef
 * @see ModelAndView
 * @since 4.0 2022/2/9 20:34
 */
public class ViewReturnValueHandler implements SmartReturnValueHandler {

  private final ViewResolver viewResolver;

  private @Nullable LocaleResolver localeResolver;

  /**
   * Create a handler that resolves the request locale through {@link WebUtils#resolveLocale(ServerHttpExchange)}.
   *
   * @param viewResolver the resolver for named views (never {@code null})
   */
  public ViewReturnValueHandler(ViewResolver viewResolver) {
    this(viewResolver, null);
  }

  /**
   * Create a handler with the given view and locale resolvers.
   *
   * @param viewResolver the resolver for named views (never {@code null})
   * @param localeResolver the resolver for the request locale, or {@code null}
   * to use {@link WebUtils#resolveLocale(ServerHttpExchange)}
   */
  public ViewReturnValueHandler(ViewResolver viewResolver, @Nullable LocaleResolver localeResolver) {
    Assert.notNull(viewResolver, "viewResolver is required");
    this.viewResolver = viewResolver;
    this.localeResolver = localeResolver;
  }

  /**
   * Determine whether the result can be handled as a view, taking the handler
   * method's declared return type into account when available.
   *
   * <p>For a {@code null} result, a handler method is supported only if its
   * declared return type is assignable to {@link View}, {@link ViewRef}, or
   * {@link ModelAndView}. If the result matches a declared {@link CharSequence}
   * return type, it is supported only if the method is not marked for
   * response body handling. Otherwise, support is determined from the result alone.
   *
   * @param handler the request handler, which may be {@code null}
   * @param returnValue the result to check, which may be {@code null}
   * @return whether the result is supported for the given handler
   * @see #supportsReturnValue(Object)
   */
  @Override
  public boolean supportsHandler(@Nullable Object handler, @Nullable Object returnValue) {
    HandlerMethod handlerMethod = HandlerMethod.resolve(handler);
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

  /**
   * Check whether the result is a supported view name, reference, or object,
   * without consulting a handler method.
   *
   * @param returnValue the result to check, which may be {@code null}
   * @return {@code true} for a {@link CharSequence}, {@link View}, {@link ViewRef},
   * or {@link ModelAndView}; {@code false} otherwise
   */
  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof CharSequence
            || returnValue instanceof View
            || returnValue instanceof ViewRef
            || returnValue instanceof ModelAndView;
  }

  /**
   * Render the result according to its runtime type. A {@code null} result
   * leaves the response unchanged.
   *
   * @param exchange the current HTTP request and response context
   * @param handler the request handler, which is not used by this method
   * @param returnValue the view result to render, or {@code null} to skip rendering
   * @throws ViewRenderingException if the result type is unsupported or view
   * resolution, model update, or rendering fails
   */
  @Override
  public void handleReturnValue(ServerHttpExchange exchange,
          @Nullable Object handler, @Nullable Object returnValue) throws ViewRenderingException {
    if (returnValue instanceof CharSequence viewName) {
      renderView(exchange, viewName);
    }
    else if (returnValue instanceof ViewRef viewRef) {
      renderView(exchange, viewRef);
    }
    else if (returnValue instanceof View view) {
      renderView(exchange, view);
    }
    else if (returnValue instanceof ModelAndView mv) {
      renderView(exchange, mv);
    }
    else if (returnValue != null) {
      throw new ViewRenderingException("Unsupported render result [%s] as view".formatted(returnValue));
    }
  }

  // renderView

  /**
   * Apply the status, if present, and render the named or direct view using
   * the model from the given {@link ModelAndView}. The status is also exposed
   * as the {@link View#RESPONSE_STATUS_ATTRIBUTE} request attribute. If the
   * result contains no view, only the status is applied.
   *
   * @param request the current HTTP request and response context
   * @param mv the model and view to render, or {@code null} to do nothing
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   */
  public void renderView(ServerHttpExchange request, @Nullable ModelAndView mv) throws ViewRenderingException {
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
   * Convert the view name to a {@link String} and render it using the request model.
   *
   * @param context the current HTTP request and response context
   * @param viewName the logical view name
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   */
  public void renderView(ServerHttpExchange context, CharSequence viewName) {
    renderView(context, viewName.toString());
  }

  /**
   * Resolve and render a named view using the request model.
   *
   * @param context the current HTTP request and response context
   * @param viewName the logical view name
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   */
  public void renderView(ServerHttpExchange context, String viewName) {
    renderView(context, viewName, null);
  }

  /**
   * Resolve a named view using the request locale and render it with the given model.
   *
   * @param context the current HTTP request and response context
   * @param viewName the logical view name
   * @param model the mutable model to merge with the request model, or {@code null}
   * to use only the request model, if present
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   * @see #renderView(ServerHttpExchange, View, Map)
   */
  public void renderView(ServerHttpExchange context, String viewName, @Nullable Map<String, Object> model) {
    View view = resolveViewName(context, viewName, null);
    renderView(context, view, model);
  }

  /**
   * Resolve and render a view reference using the request model.
   *
   * @param context the current HTTP request and response context
   * @param viewRef the reference containing the view name and optional locale
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   * @see #renderView(ServerHttpExchange, ViewRef, Map)
   */
  public void renderView(ServerHttpExchange context, ViewRef viewRef) {
    renderView(context, viewRef, null);
  }

  /**
   * Resolve and render the referenced view with the given model. Use the
   * reference's locale if present, otherwise use the request locale.
   *
   * @param context the current HTTP request and response context
   * @param viewRef the reference containing the view name and optional locale
   * @param model the mutable model to merge with the request model, or {@code null}
   * to use only the request model, if present
   * @throws ViewRenderingException if view resolution, model update, or rendering fails
   * @see #renderView(ServerHttpExchange, View, Map)
   */
  public void renderView(ServerHttpExchange context, ViewRef viewRef, @Nullable Map<String, Object> model) {
    View view = resolveViewName(context, viewRef.getViewName(), viewRef.getLocale());
    renderView(context, view, model);
  }

  /**
   * Resolve the request locale using the configured resolver, if present,
   * or {@link WebUtils#resolveLocale(ServerHttpExchange)} otherwise.
   */
  private Locale getLocale(ServerHttpExchange request) {
    LocaleResolver localeResolver = this.localeResolver;
    if (localeResolver != null) {
      return localeResolver.resolveLocale(request);
    }
    return WebUtils.resolveLocale(request);
  }

  /**
   * Render the given view directly using the request model, if present.
   *
   * @param context the current HTTP request and response context
   * @param view the view to render
   * @throws ViewRenderingException if the model update or rendering fails
   */
  public void renderView(ServerHttpExchange context, View view) {
    renderView(context, view, null);
  }

  /**
   * Render the given view directly after updating and merging the request model.
   * If a model is supplied, request model entries are added to it in place,
   * replacing entries with the same name. If neither model is available,
   * the view receives {@code null}.
   *
   * @param context the current HTTP request and response context
   * @param view the view to render
   * @param model the mutable model to merge with the request model, or {@code null}
   * to use only the request model, if present
   * @throws ViewRenderingException if the model update or rendering fails
   */
  public void renderView(ServerHttpExchange context, View view, @Nullable Map<String, @Nullable Object> model) {
    // Model from BindingContext
    BindingContext binding = BindingContext.find(context);
    if (binding != null) {
      try {
        binding.updateModel(context);
      }
      catch (Throwable e) {
        throw new ViewRenderingException("View model update failed", e);
      }
      if (binding.hasModel()) {
        // injected arguments Model
        if (model == null) {
          model = binding.getModel().asMap();
        }
        else {
          model.putAll(binding.getModel().asMap());
        }
      }
    }

    try {
      // do rendering
      view.render(model, context);
    }
    catch (Exception e) {
      throw new ViewRenderingException("View '%s' render failed".formatted(view), e);
    }
  }

  /**
   * Set the resolver that determines the locale for named views without an
   * explicit locale. The resulting locale is passed to the view resolver.
   *
   * @param localeResolver the request locale resolver, or {@code null} to use
   * {@link WebUtils#resolveLocale(ServerHttpExchange)}
   */
  public void setLocaleResolver(@Nullable LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  /**
   * Return the explicitly configured locale resolver, or {@code null} if none is set.
   */
  public @Nullable LocaleResolver getLocaleResolver() {
    return localeResolver;
  }

  /**
   * Return the resolver used to resolve named views.
   */
  public ViewResolver getViewResolver() {
    return viewResolver;
  }

  private View resolveViewName(ServerHttpExchange context, String viewName, @Nullable Locale locale) {
    if (locale == null) {
      locale = getLocale(context);
    }

    View view = resolveViewName(locale, viewName);
    if (view == null) {
      HandlerMatchingMetadata matchingMetadata = HandlerMatchingMetadata.find(context);
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

  private @Nullable View resolveViewName(Locale locale, String viewName) {
    try {
      return viewResolver.resolveViewName(viewName, locale);
    }
    catch (Exception e) {
      throw new ViewRenderingException("Could not resolve view with name '%s'".formatted(viewName), e);
    }
  }

}
