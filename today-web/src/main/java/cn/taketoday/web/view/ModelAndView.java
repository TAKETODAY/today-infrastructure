/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.Map;

import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;

/**
 * Holder for both Model and View in the web MVC framework.
 * Note that these are entirely distinct. This class merely holds
 * both to make it possible for a controller to return both model
 * and view in a single return value.
 *
 * <p>Represents a model and view returned by a handler, to be resolved
 * by a ReturnValueHandler. The view can take the form of a String
 * view name which will need to be resolved by a ViewResolver object;
 * alternatively a View object can be specified directly. The model
 * is a Map, allowing the use of multiple objects keyed by name.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @author TODAY
 * @since 2018-12-02 19:54
 */
public class ModelAndView {

  /** View instance or view name String. */
  @Nullable
  private Object view;

  /** Model Map. */
  @Nullable
  private ModelMap model;

  /** Optional HTTP status for the response. */
  @Nullable
  private HttpStatusCode status;

  /** Indicates whether or not this instance has been cleared with a call to {@link #clear()}. */
  private boolean cleared = false;

  /**
   * Default constructor for bean-style usage: populating bean
   * properties instead of passing in constructor arguments.
   *
   * @see #setView(View)
   * @see #setViewName(String)
   */
  public ModelAndView() { }

  /**
   * Convenient constructor when there is no model data to expose.
   * Can also be used in conjunction with {@code addObject}.
   *
   * @param viewName name of the View to render, to be resolved
   * by the DispatcherServlet's ViewResolver
   * @see #addObject
   */
  public ModelAndView(String viewName) {
    this.view = viewName;
  }

  /**
   * Convenient constructor when there is no model data to expose.
   * Can also be used in conjunction with {@code addObject}.
   *
   * @param view the View object to render
   * @see #addObject
   */
  public ModelAndView(View view) {
    this.view = view;
  }

  /**
   * Create a new ModelAndView given a view name and a model.
   *
   * @param viewName name of the View to render, to be resolved
   * by the DispatcherServlet's ViewResolver
   * @param model a Map of model names (Strings) to model objects
   * (Objects). Model entries may not be {@code null}, but the
   * model Map may be {@code null} if there is no model data.
   */
  public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
    this.view = viewName;
    if (model != null) {
      getModelMap().addAllAttributes(model);
    }
  }

  /**
   * Create a new ModelAndView given a View object and a model.
   * <em>Note: the supplied model data is copied into the internal
   * storage of this class. You should not consider to modify the supplied
   * Map after supplying it to this class</em>
   *
   * @param view the View object to render
   * @param model a Map of model names (Strings) to model objects
   * (Objects). Model entries may not be {@code null}, but the
   * model Map may be {@code null} if there is no model data.
   */
  public ModelAndView(View view, @Nullable Map<String, ?> model) {
    this.view = view;
    if (model != null) {
      getModelMap().addAllAttributes(model);
    }
  }

  /**
   * Create a new ModelAndView given a view name and HTTP status.
   *
   * @param viewName name of the View to render, to be resolved
   * by the DispatcherServlet's ViewResolver
   * @param status an HTTP status code to use for the response
   * (to be set just prior to View rendering)
   */
  public ModelAndView(String viewName, HttpStatusCode status) {
    this.view = viewName;
    this.status = status;
  }

  /**
   * Create a new ModelAndView given a view name, model, and HTTP status.
   *
   * @param viewName name of the View to render, to be resolved
   * by the DispatcherServlet's ViewResolver
   * @param model a Map of model names (Strings) to model objects
   * (Objects). Model entries may not be {@code null}, but the
   * model Map may be {@code null} if there is no model data.
   * @param status an HTTP status code to use for the response
   * (to be set just prior to View rendering)
   */
  public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatusCode status) {
    this.view = viewName;
    if (model != null) {
      getModelMap().addAllAttributes(model);
    }
    this.status = status;
  }

  /**
   * Convenient constructor to take a single model object.
   *
   * @param viewName name of the View to render, to be resolved
   * by the DispatcherServlet's ViewResolver
   * @param modelName name of the single entry in the model
   * @param modelObject the single model object
   */
  public ModelAndView(String viewName, String modelName, Object modelObject) {
    this.view = viewName;
    addObject(modelName, modelObject);
  }

  /**
   * Convenient constructor to take a single model object.
   *
   * @param view the View object to render
   * @param modelName name of the single entry in the model
   * @param modelObject the single model object
   */
  public ModelAndView(View view, String modelName, Object modelObject) {
    this.view = view;
    addObject(modelName, modelObject);
  }

  /**
   * Set a view name for this ModelAndView, to be resolved by the
   * DispatcherServlet via a ViewResolver. Will override any
   * pre-existing view name or View.
   */
  public void setViewName(@Nullable String viewName) {
    this.view = viewName;
  }

  /**
   * Return the view name to be resolved by the DispatcherServlet
   * via a ViewResolver, or {@code null} if we are using a View object.
   */
  @Nullable
  public String getViewName() {
    return (this.view instanceof String ? (String) this.view : null);
  }

  /**
   * Set a View object for this ModelAndView. Will override any
   * pre-existing view name or View.
   */
  public void setView(@Nullable View view) {
    this.view = view;
  }

  /**
   * Return the View object, or {@code null} if we are using a view name
   * to be resolved by the DispatcherServlet via a ViewResolver.
   */
  @Nullable
  public View getView() {
    return view instanceof View ? (View) this.view : null;
  }

  /**
   * Indicate whether or not this {@code ModelAndView} has a view, either
   * as a view name or as a direct {@link View} instance.
   */
  public boolean hasView() {
    return view != null;
  }

  /**
   * Return whether we use a view reference, i.e. {@code true}
   * if the view has been specified via a name to be resolved by the
   * DispatcherServlet via a ViewResolver.
   */
  public boolean isReference() {
    return view instanceof String;
  }

  /**
   * Return the model map. May return {@code null}.
   * Called by DispatcherServlet for evaluation of the model.
   */
  @Nullable
  protected Map<String, Object> getModelInternal() {
    return this.model;
  }

  /**
   * Return the underlying {@code ModelMap} instance (never {@code null}).
   */
  public ModelMap getModelMap() {
    if (this.model == null) {
      this.model = new ModelMap();
    }
    return this.model;
  }

  /**
   * Return the model map. Never returns {@code null}.
   * To be called by application code for modifying the model.
   */
  public Map<String, Object> getModel() {
    return getModelMap();
  }

  /**
   * Set the HTTP status to use for the response.
   * <p>The response status is set just prior to View rendering.
   */
  public void setStatus(@Nullable HttpStatusCode status) {
    this.status = status;
  }

  /**
   * Return the configured HTTP status for the response, if any.
   */
  @Nullable
  public HttpStatusCode getStatus() {
    return this.status;
  }

  /**
   * Add an attribute to the model.
   *
   * @param attributeName name of the object to add to the model (never {@code null})
   * @param attributeValue object to add to the model (can be {@code null})
   * @see ModelMap#addAttribute(String, Object)
   * @see #getModelMap()
   */
  public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
    getModelMap().addAttribute(attributeName, attributeValue);
    return this;
  }

  /**
   * Add an attribute to the model using parameter name generation.
   *
   * @param attributeValue the object to add to the model (never {@code null})
   * @see ModelMap#addAttribute(Object)
   * @see #getModelMap()
   */
  public ModelAndView addObject(Object attributeValue) {
    getModelMap().addAttribute(attributeValue);
    return this;
  }

  /**
   * Add all attributes contained in the provided Map to the model.
   *
   * @param modelMap a Map of attributeName &rarr; attributeValue pairs
   * @see ModelMap#addAllAttributes(Map)
   * @see #getModelMap()
   */
  public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
    getModelMap().addAllAttributes(modelMap);
    return this;
  }

  /**
   * Clear the state of this ModelAndView object.
   * The object will be empty afterwards.
   * <p>Can be used to suppress rendering of a given ModelAndView object
   * in the {@code postHandle} method of a HandlerInterceptor.
   *
   * @see #isEmpty()
   * @see HandlerInterceptor#afterProcess(RequestContext, Object, Object)
   */
  public void clear() {
    this.view = null;
    this.model = null;
    this.cleared = true;
  }

  /**
   * Return whether this ModelAndView object is empty,
   * i.e. whether it does not hold any view and does not contain a model.
   */
  public boolean isEmpty() {
    return (this.view == null && CollectionUtils.isEmpty(this.model));
  }

  /**
   * Return whether this ModelAndView object is empty as a result of a call to {@link #clear}
   * i.e. whether it does not hold any view and does not contain a model.
   * <p>Returns {@code false} if any additional state was added to the instance
   * <strong>after</strong> the call to {@link #clear}.
   *
   * @see #clear()
   */
  public boolean wasCleared() {
    return (this.cleared && isEmpty());
  }

  /**
   * Return diagnostic information about this model and view.
   */
  @Override
  public String toString() {
    return "ModelAndView [view=" + formatView() + "; model=" + this.model + "]";
  }

  private String formatView() {
    return isReference() ? "\"" + this.view + "\"" : "[" + this.view + "]";
  }

}
