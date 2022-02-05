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

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;

/**
 * @author TODAY <br>
 * 2018-12-02 19:54
 */
public class ModelAndView implements Model {

  @Nullable
  private Object view;

  private final RequestContext dataModel;

  public ModelAndView() {
    this((Object) null);
  }

  public ModelAndView(@Nullable Object view) {
    this(view, RequestContextHolder.currentContext());
  }

  public ModelAndView(RequestContext dataModel) {
    this(null, dataModel);
  }

  public ModelAndView(@Nullable Object view, RequestContext dataModel) {
    setView(view);
    this.dataModel = dataModel;
  }

  public ModelAndView(Object view, String name, Object value) {
    this(view);
    setAttribute(name, value);
  }

  public String getContentType() {
    return dataModel.getContentType();
  }

  public ModelAndView setContentType(String contentType) {
    dataModel.setContentType(contentType);
    return this;
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
  public void setAttribute(String name, Object value) {
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

  @Override
  public void clear() {
    dataModel.clear();
  }

  @Override
  public String[] getAttributeNames() {
    return dataModel.getAttributeNames();
  }

}
