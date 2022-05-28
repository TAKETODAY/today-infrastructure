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

package cn.taketoday.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.support.SessionStatus;
import cn.taketoday.web.bind.support.SimpleSessionStatus;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.view.BindingAwareModelMap;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.ModelMap;
import cn.taketoday.web.view.RedirectModel;

/**
 * Context to assist with binding request data onto Objects and provide access
 * to a shared {@link Model} with controller-specific attributes.
 *
 * <p>Provides methods to create a {@link RequestContextDataBinder} for a specific
 * target, command Object to apply data binding and validation to, or without a
 * target Object for simple type conversion from request values.
 *
 * <p>Container for the default model for the request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:40
 */
public class BindingContext {

  @Nullable
  private Object view;

  private final ModelMap model = new BindingAwareModelMap();

  @Nullable
  private RedirectModel redirectModel;

  private final Set<String> noBinding = new HashSet<>(4);

  private final Set<String> bindingDisabled = new HashSet<>(4);

  private final SessionStatus sessionStatus = new SimpleSessionStatus();

  @Nullable
  private final WebBindingInitializer initializer;

  protected ModelAndView modelAndView;

  /**
   * Create a new {@code BindingContext}.
   */
  public BindingContext() {
    this(null);
  }

  /**
   * Create a new {@code BindingContext} with the given initializer.
   *
   * @param initializer the binding initializer to apply (may be {@code null})
   */
  public BindingContext(@Nullable WebBindingInitializer initializer) {
    this.initializer = initializer;
  }

  /**
   * Create a {@link RequestContextDataBinder} without a target object for type
   * conversion of request values to simple types.
   *
   * @param exchange the current exchange
   * @param name the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public RequestContextDataBinder createBinder(RequestContext exchange, String name) throws Throwable {
    return createBinder(exchange, null, name);
  }

  /**
   * Create a {@link RequestContextDataBinder} to apply data binding and
   * validation with on the target, command object.
   *
   * @param exchange the current exchange
   * @param target the object to create a data binder for
   * @param name the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public RequestContextDataBinder createBinder(
          RequestContext exchange, @Nullable Object target, String name) throws Throwable {
    RequestContextDataBinder dataBinder = new HandlerMatchingMetadataDataBinder(target, name);
    if (initializer != null) {
      initializer.initBinder(dataBinder);
    }
    initBinder(dataBinder, exchange);
    return dataBinder;
  }

  /**
   * Initialize the data binder instance for the given exchange.
   *
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public void initBinder(WebDataBinder dataBinder, RequestContext request) throws Throwable {

  }

  /**
   * Extended variant of {@link RequestContextDataBinder}, adding path variables.
   */
  private static class HandlerMatchingMetadataDataBinder extends RequestContextDataBinder {

    public HandlerMatchingMetadataDataBinder(@Nullable Object target, String objectName) {
      super(target, objectName);
    }

    @Override
    public PropertyValues getValuesToBind(RequestContext request) {
      PropertyValues valuesToBind = super.getValuesToBind(request);
      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        Map<String, String> uriVariables = matchingMetadata.getUriVariables();
        valuesToBind.add(uriVariables);
      }
      return valuesToBind;
    }
  }

  /**
   * Get a {@link ModelAndView}
   * <p>
   * If there isn't a {@link ModelAndView} in this {@link RequestContext},
   * <b>Create One</b>
   *
   * @return Returns {@link ModelAndView}
   */
  public ModelAndView getModelAndView() {
    if (modelAndView == null) {
      this.modelAndView = new ModelAndView();
    }
    return modelAndView;
  }

  /**
   * @since 3.0
   */
  public boolean hasModelAndView() {
    return modelAndView != null;
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
   * Return the default model.
   */
  public ModelMap getModel() {
    return this.model;
  }

  @Nullable
  public RedirectModel getRedirectModel() {
    return redirectModel;
  }

  public void setRedirectModel(RedirectModel redirectModel) {
    this.redirectModel = redirectModel;
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
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(String, Object)}.
   */
  public BindingContext addAttribute(String name, @Nullable Object value) {
    getModel().setAttribute(name, value);
    return this;
  }

  /**
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(Object)}.
   */
  public BindingContext addAttribute(Object value) {
    getModel().addAttribute(value);
    return this;
  }

  /**
   * Copy all attributes to the underlying model.
   * A shortcut for {@code getModel().addAllAttributes(Map)}.
   */
  public BindingContext addAllAttributes(@Nullable Map<String, ?> attributes) {
    getModel().addAllAttributes(attributes);
    return this;
  }

  /**
   * Copy attributes in the supplied {@code Map} with existing objects of
   * the same name taking precedence (i.e. not getting replaced).
   * A shortcut for {@code getModel().mergeAttributes(Map<String, ?>)}.
   */
  public BindingContext mergeAttributes(@Nullable Map<String, ?> attributes) {
    getModel().mergeAttributes(attributes);
    return this;
  }

  /**
   * Remove the given attributes from the model.
   */
  public BindingContext removeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      for (String key : attributes.keySet()) {
        getModel().removeAttribute(key);
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
    StringBuilder sb = new StringBuilder("BindingContext: ");
    if (isViewReference()) {
      sb.append("reference to view with name '")
              .append(view)
              .append('\'');
    }
    else {
      sb.append("View is [").append(view).append(']');
    }
    sb.append("; default model ");
    sb.append(getModel());

    RedirectModel redirectModel = getRedirectModel();
    if (redirectModel != null) {
      sb.append("; redirect model ");
      sb.append(redirectModel);
    }
    return sb.toString();
  }

}
