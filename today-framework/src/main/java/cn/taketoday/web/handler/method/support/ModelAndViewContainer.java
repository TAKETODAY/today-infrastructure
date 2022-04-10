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

package cn.taketoday.web.handler.method.support;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.bind.support.SessionStatus;
import cn.taketoday.web.bind.support.SimpleSessionStatus;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelMap;
import cn.taketoday.web.view.RedirectModel;

/**
 * Records model and view related decisions made by
 * {@link ParameterResolvingStrategies ParameterResolvingStrategies} and
 * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} during the course of invocation of
 * a controller method.
 *
 * <p>The {@link #setRequestHandled} flag can be used to indicate the request
 * has been handled directly and view resolution is not required.
 *
 * <p>A default {@link Model} is automatically created at instantiation.
 * An alternate model instance may be provided via {@link #setRedirectModel}
 * for use in a redirect scenario. When {@link #setRedirectModelScenario} is set
 * to {@code true} signalling a redirect scenario, the {@link #getModel()}
 * returns the redirect model instead of the default model.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:40
 */
public class ModelAndViewContainer {

  private boolean ignoreDefaultModelOnRedirect = false;

  @Nullable
  private Object view;

  private final ModelMap defaultModel = new BindingAwareModelMap();

  @Nullable
  private RedirectModel redirectModel;

  private boolean redirectModelScenario = false;

  @Nullable
  private HttpStatusCode status;

  private final Set<String> noBinding = new HashSet<>(4);

  private final Set<String> bindingDisabled = new HashSet<>(4);

  private final SessionStatus sessionStatus = new SimpleSessionStatus();

  private boolean requestHandled = false;

  /**
   * By default the content of the "default" model is used both during
   * rendering and redirect scenarios. Alternatively controller methods
   * can declare an argument of type {@code RedirectAttributes} and use
   * it to provide attributes to prepare the redirect URL.
   * <p>Setting this flag to {@code true} guarantees the "default" model is
   * never used in a redirect scenario even if a RedirectAttributes argument
   * is not declared. Setting it to {@code false} means the "default" model
   * may be used in a redirect if the controller method doesn't declare a
   * RedirectAttributes argument.
   * <p>The default setting is {@code false}.
   */
  public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
    this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
  }

  /**
   * Set a view name to be resolved by the DispatcherServlet via a ViewResolver.
   * Will override any pre-existing view name or View.
   */
  public void setViewName(@Nullable String viewName) {
    this.view = viewName;
  }

  /**
   * Return the view name to be resolved by the DispatcherServlet via a
   * ViewResolver, or {@code null} if a View object is set.
   */
  @Nullable
  public String getViewName() {
    return view instanceof String ? (String) this.view : null;
  }

  /**
   * Set a View object to be used by the DispatcherServlet.
   * Will override any pre-existing view name or View.
   */
  public void setView(@Nullable Object view) {
    this.view = view;
  }

  /**
   * Return the View object, or {@code null} if we using a view name
   * to be resolved by the DispatcherServlet via a ViewResolver.
   */
  @Nullable
  public Object getView() {
    return this.view;
  }

  /**
   * Whether the view is a view reference specified via a name to be
   * resolved by the DispatcherServlet via a ViewResolver.
   */
  public boolean isViewReference() {
    return view instanceof String;
  }

  /**
   * Return the model to use -- either the "default" or the "redirect" model.
   * The default model is used if {@code redirectModelScenario=false} or
   * there is no redirect model (i.e. RedirectAttributes was not declared as
   * a method argument) and {@code ignoreDefaultModelOnRedirect=false}.
   */
  public ModelMap getModel() {
    if (useDefaultModel()) {
      return this.defaultModel;
    }
    else {
      if (this.redirectModel == null) {
        this.redirectModel = new RedirectModel();
      }
      return this.redirectModel;
    }
  }

  @Nullable
  public RedirectModel getRedirectModel() {
    return redirectModel;
  }

  /**
   * Whether to use the default model or the redirect model.
   */
  private boolean useDefaultModel() {
    return (!this.redirectModelScenario || (this.redirectModel == null && !this.ignoreDefaultModelOnRedirect));
  }

  /**
   * Return the "default" model created at instantiation.
   * <p>In general it is recommended to use {@link #getModel()} instead which
   * returns either the "default" model (template rendering) or the "redirect"
   * model (redirect URL preparation). Use of this method may be needed for
   * advanced cases when access to the "default" model is needed regardless,
   * e.g. to save model attributes specified via {@code @SessionAttributes}.
   *
   * @return the default model (never {@code null})
   */
  public ModelMap getDefaultModel() {
    return this.defaultModel;
  }

  /**
   * Provide a separate model instance to use in a redirect scenario.
   * <p>The provided additional model however is not used unless
   * {@link #setRedirectModelScenario} gets set to {@code true}
   * to signal an actual redirect scenario.
   */
  public void setRedirectModel(RedirectModel redirectModel) {
    this.redirectModel = redirectModel;
  }

  /**
   * Whether the controller has returned a redirect instruction, e.g. a
   * "redirect:" prefixed view name, a RedirectView instance, etc.
   */
  public void setRedirectModelScenario(boolean redirectModelScenario) {
    this.redirectModelScenario = redirectModelScenario;
  }

  /**
   * Provide an HTTP status that will be passed on to with the
   * {@code ModelAndView} used for view rendering purposes.
   */
  public void setStatus(@Nullable HttpStatusCode status) {
    this.status = status;
  }

  /**
   * Return the configured HTTP status, if any.
   */
  @Nullable
  public HttpStatusCode getStatus() {
    return this.status;
  }

  /**
   * Programmatically register an attribute for which data binding should not occur,
   * not even for a subsequent {@code @ModelAttribute} declaration.
   *
   * @param attributeName the name of the attribute
   */
  public void setBindingDisabled(String attributeName) {
    this.bindingDisabled.add(attributeName);
  }

  /**
   * Whether binding is disabled for the given model attribute.
   */
  public boolean isBindingDisabled(String name) {
    return bindingDisabled.contains(name) || this.noBinding.contains(name);
  }

  /**
   * Register whether data binding should occur for a corresponding model attribute,
   * corresponding to an {@code @ModelAttribute(binding=true/false)} declaration.
   * <p>Note: While this flag will be taken into account by {@link #isBindingDisabled},
   * a hard {@link #setBindingDisabled} declaration will always override it.
   *
   * @param attributeName the name of the attribute
   */
  public void setBinding(String attributeName, boolean enabled) {
    if (!enabled) {
      this.noBinding.add(attributeName);
    }
    else {
      this.noBinding.remove(attributeName);
    }
  }

  /**
   * Return the {@link SessionStatus} instance to use that can be used to
   * signal that session processing is complete.
   */
  public SessionStatus getSessionStatus() {
    return this.sessionStatus;
  }

  /**
   * Whether the request has been handled fully within the handler, e.g.
   * {@code @ResponseBody} method, and therefore view resolution is not
   * necessary. This flag can also be set when controller methods declare an
   * argument of type {@code ServletResponse} or {@code OutputStream}).
   * <p>The default value is {@code false}.
   */
  public void setRequestHandled(boolean requestHandled) {
    this.requestHandled = requestHandled;
  }

  /**
   * Whether the request has been handled fully within the handler.
   */
  public boolean isRequestHandled() {
    return this.requestHandled;
  }

  /**
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(String, Object)}.
   */
  public ModelAndViewContainer addAttribute(String name, @Nullable Object value) {
    getModel().addAttribute(name, value);
    return this;
  }

  /**
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(Object)}.
   */
  public ModelAndViewContainer addAttribute(Object value) {
    getModel().addAttribute(value);
    return this;
  }

  /**
   * Copy all attributes to the underlying model.
   * A shortcut for {@code getModel().addAllAttributes(Map)}.
   */
  public ModelAndViewContainer addAllAttributes(@Nullable Map<String, ?> attributes) {
    getModel().addAllAttributes(attributes);
    return this;
  }

  /**
   * Copy attributes in the supplied {@code Map} with existing objects of
   * the same name taking precedence (i.e. not getting replaced).
   * A shortcut for {@code getModel().mergeAttributes(Map<String, ?>)}.
   */
  public ModelAndViewContainer mergeAttributes(@Nullable Map<String, ?> attributes) {
    getModel().mergeAttributes(attributes);
    return this;
  }

  /**
   * Remove the given attributes from the model.
   */
  public ModelAndViewContainer removeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      for (String key : attributes.keySet()) {
        getModel().remove(key);
      }
    }
    return this;
  }

  /**
   * Whether the underlying model contains the given attribute name.
   * A shortcut for {@code getModel().containsAttribute(String)}.
   */
  public boolean containsAttribute(String name) {
    return getModel().containsAttribute(name);
  }

  /**
   * Return diagnostic information.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
    if (!isRequestHandled()) {
      if (isViewReference()) {
        sb.append("reference to view with name '").append(this.view).append('\'');
      }
      else {
        sb.append("View is [").append(this.view).append(']');
      }
      if (useDefaultModel()) {
        sb.append("; default model ");
      }
      else {
        sb.append("; redirect model ");
      }
      sb.append(getModel());
    }
    else {
      sb.append("Request handled directly");
    }
    return sb.toString();
  }

}
