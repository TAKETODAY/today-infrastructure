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

package cn.taketoday.web.handler.mvc;

import java.util.function.Supplier;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.View;

/**
 * Trivial controller that always returns a pre-configured view and optionally
 * sets the response status code. The view and status can be configured using
 * the provided configuration properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:48
 */
public class ParameterizableViewController extends AbstractController {

  @Nullable
  private Object returnValue;

  @Nullable
  private HttpStatusCode statusCode;

  private boolean statusOnly;

  @Nullable
  private String contentType;

  public ParameterizableViewController() {
    super(false);
    setSupportedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name());
  }

  /**
   * Set a view name for the ModelAndView to return, to be resolved by the
   * DispatcherServlet via a ViewResolver. Will override any pre-existing
   * view name or View.
   */
  public void setViewName(@Nullable String viewName) {
    this.returnValue = viewName;
  }

  /**
   * Return the name of the view to delegate to, or {@code null} if using a
   * View instance.
   */
  @Nullable
  public String getViewName() {
    if (this.returnValue instanceof String viewName) {
      if (getStatusCode() != null && getStatusCode().is3xxRedirection()) {
        return viewName.startsWith("redirect:") ? viewName : "redirect:" + viewName;
      }
      else {
        return viewName;
      }
    }
    return null;
  }

  /**
   * Set a View object for the ModelAndView to return.
   * Will override any pre-existing view name or View.
   */
  public void setView(View view) {
    this.returnValue = view;
  }

  /**
   * Return the View object, or {@code null} if we are using a view name
   * to be resolved by the DispatcherHandler via a ViewResolver.
   */
  @Nullable
  public View getView() {
    return (this.returnValue instanceof View ? (View) this.returnValue : null);
  }

  /**
   * Configure the HTTP status code that this controller should set on the
   * response.
   * <p>When a "redirect:" prefixed view name is configured, there is no need
   * to set this property since RedirectView will do that. However this property
   * may still be used to override the 3xx status code of {@code RedirectView}.
   * For full control over redirecting provide a {@code RedirectView} instance.
   * <p>If the status code is 204 and no view is configured, the request is
   * fully handled within the controller.
   */
  public void setStatusCode(@Nullable HttpStatusCode statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Return the configured HTTP status code or {@code null}.
   */
  @Nullable
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  /**
   * The property can be used to indicate the request is considered fully
   * handled within the controller and that no view should be used for rendering.
   * Useful in combination with {@link #setStatusCode}.
   * <p>By default this is set to {@code false}.
   */
  public void setStatusOnly(boolean statusOnly) {
    this.statusOnly = statusOnly;
  }

  /**
   * Whether the request is fully handled within the controller.
   */
  public boolean isStatusOnly() {
    return this.statusOnly;
  }

  /**
   * Set a result object to return.
   * Will override any pre-existing view name or View.
   */
  public void setReturnValue(@Nullable Object returnValue) {
    this.returnValue = returnValue;
  }

  /**
   * Return the result object, or {@code null} if we are not set.
   */
  @Nullable
  public Object getReturnValue() {
    return returnValue;
  }

  /**
   * Set content type
   */
  public void setContentType(@Nullable String contentType) {
    this.contentType = contentType;
  }

  @Nullable
  public String getContentType() {
    return contentType;
  }

  /**
   * Return a ModelAndView object with the specified view name.
   * <p>The content of the {@link RequestContext#getInputRedirectModel()}
   * "input" RedirectModel} is also added to the model.
   *
   * @see #getViewName()
   */
  @Override
  protected Object handleRequestInternal(RequestContext request) throws Throwable {
    String contentType = getContentType();
    if (contentType != null) {
      request.setContentType(contentType);
    }

    String viewName = getViewName();

    HttpStatusCode statusCode = getStatusCode();
    if (statusCode != null) {
      if (statusCode.is3xxRedirection()) {
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, statusCode);
      }
      else {
        request.setStatus(statusCode);
        if (statusCode.equals(HttpStatus.NO_CONTENT) && viewName == null) {
          return NONE_RETURN_VALUE;
        }
      }
    }

    if (isStatusOnly()) {
      return NONE_RETURN_VALUE;
    }

    Object result = getReturnValue();
    if (viewName == null && !(result instanceof View) && result != null) {
      if (result instanceof Supplier<?> supplier) {
        return supplier.get();
      }
      if (result instanceof HttpRequestHandler handler) {
        return handler.handleRequest(request);
      }
      return result;
    }

    ModelAndView modelAndView = new ModelAndView();
    RedirectModel redirectModel = request.getInputRedirectModel();
    if (redirectModel != null) {
      modelAndView.addAllObjects(redirectModel.asMap());
    }

    if (viewName != null) {
      modelAndView.setViewName(viewName);
    }
    else {
      modelAndView.setView(getView());
    }
    return modelAndView;
  }

  @Override
  public String toString() {
    return "ParameterizableViewController [" + formatStatusAndView() + "]";
  }

  private String formatStatusAndView() {
    StringBuilder sb = new StringBuilder();
    if (this.statusCode != null) {
      sb.append("status=").append(this.statusCode);
    }
    if (this.returnValue != null) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      String viewName = getViewName();
      sb.append("view=").append(viewName != null ? "\"" + viewName + "\"" : this.returnValue);
    }
    return sb.toString();
  }

}
