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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;

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
public class ModelAndView implements Model {

  @Nullable
  private Object view;

  private final Model dataModel;

  /** Optional HTTP status for the response. */
  @Nullable
  private HttpStatus status;

  /** Indicates whether or not this instance has been cleared with a call to {@link #clear()}. */
  private boolean cleared = false;

  public ModelAndView() {
    this((Object) null);
  }

  public ModelAndView(@Nullable Object view) {
    this(view, new ModelAttributes());
  }

  public ModelAndView(Model dataModel) {
    this(null, dataModel);
  }

  public ModelAndView(@Nullable Object view, Model dataModel) {
    setView(view);
    this.dataModel = dataModel;
  }

  public ModelAndView(Object view, String name, Object value) {
    this(view);
    setAttribute(name, value);
  }

  /**
   * Set view
   *
   * @param view View object
   * @return Current {@link ModelAndView}
   */
  public ModelAndView setView(@Nullable Object view) {
    this.view = view;
    return this;
  }

  /**
   * Set a view name for this ModelAndView, to be resolved by the
   * ViewResolver. Will override any pre-existing view name or View.
   *
   * @since 4.0
   */
  public ModelAndView setViewName(@Nullable String viewName) {
    this.view = viewName;
    return this;
  }

  /**
   * Return the view name to be resolved by the DispatcherServlet
   * via a ViewResolver, or {@code null} if we are using a View object.
   *
   * @since 4.0
   */
  @Nullable
  public String getViewName() {
    return this.view instanceof String ? (String) this.view : null;
  }

  /**
   * Return whether we use a view reference, i.e. {@code true}
   * if the view has been specified via a name to be resolved by the
   * ReturnValueHandler via a ViewResolver.
   *
   * @since 4.0
   */
  public boolean isReference() {
    return this.view instanceof String;
  }

  public final boolean hasView() {
    return view != null;
  }

  @Nullable
  public Object getView() {
    return view;
  }

  @Override
  public void setAttributes(Map<String, Object> attributes) {
    dataModel.setAttributes(attributes);
  }

  @Override
  public Object getAttribute(String name) {
    return dataModel.getAttribute(name);
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    dataModel.setAttribute(name, value);
  }

  @Override
  public Object removeAttribute(String name) {
    return dataModel.removeAttribute(name);
  }

  @Override
  public Map<String, Object> asMap() {
    return dataModel.asMap();
  }

  /**
   * Clear the state of this ModelAndView object.
   * The object will be empty afterwards.
   * <p>Can be used to suppress rendering of a given ModelAndView object
   * in the {@code postHandle} method of a HandlerInterceptor.
   *
   * @see #isEmpty()
   */
  @Override
  public void clear() {
    dataModel.clear();
    this.view = null;
    this.cleared = true;
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
    // FIXME isEmpty 语义问题
    return this.cleared && isEmpty();
  }

  @Override
  public String[] getAttributeNames() {
    return dataModel.getAttributeNames();
  }

  @Override
  public Iterator<String> attributeNames() {
    return dataModel.attributeNames();
  }

  @Override
  public boolean isEmpty() {
    return dataModel.isEmpty();
  }

  @Override
  public Model addAttribute(@Nullable Object attributeValue) {
    return dataModel.addAttribute(attributeValue);
  }

  @Override
  public Model addAllAttributes(@Nullable Map<String, ?> attributes) {
    return dataModel.addAllAttributes(attributes);
  }

  @Override
  public Model addAllAttributes(@Nullable Collection<?> attributeValues) {
    return dataModel.addAllAttributes(attributeValues);
  }

  @Override
  public Model mergeAttributes(@Nullable Map<String, ?> attributes) {
    return dataModel.mergeAttributes(attributes);
  }

  /**
   * Return the model map. Never returns {@code null}.
   * To be called by application code for modifying the model.
   *
   * @since 4.0
   */
  public Model getModel() {
    return dataModel;
  }

  /**
   * Set the HTTP status to use for the response.
   * <p>The response status is set just prior to View rendering.
   *
   * @since 4.0
   */
  public void setStatus(@Nullable HttpStatus status) {
    this.status = status;
  }

  /**
   * Return the configured HTTP status for the response, if any.
   *
   * @since 4.0
   */
  @Nullable
  public HttpStatus getStatus() {
    return this.status;
  }

}
