/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.ui;

import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;

/**
 * @author TODAY <br>
 * 2018-12-02 19:54
 */
public class ModelAndView implements Model {

  private Object view;
  private final RequestContext dataModel;

  public ModelAndView() {
    this(null);
  }

  public ModelAndView(Object view) {
    this(view, RequestContextHolder.currentContext());
  }

  public ModelAndView(RequestContext dataModel) {
    this(null, dataModel);
  }

  public ModelAndView(Object view, RequestContext dataModel) {
    this.setView(view);
    this.dataModel = dataModel;
  }

  public ModelAndView(Object view, String name, Object value) {
    this(view);
    attribute(name, value);
  }

  public String getContentType() {
    return dataModel.contentType();
  }

  public ModelAndView setContentType(String contentType) {
    dataModel.contentType(contentType);
    return this;
  }

  /**
   * Set view
   *
   * @param view
   *         View object
   *
   * @return Current {@link ModelAndView}
   */
  public ModelAndView setView(Object view) {
    this.view = view;
    return this;
  }

  public final boolean hasView() {
    return view != null;
  }

  public Object getView() {
    return view;
  }

  @Override
  public Model attributes(Map<String, Object> attributes) {
    return dataModel.attributes(attributes);
  }

  @Override
  public Enumeration<String> attributes() {
    return dataModel.attributes();
  }

  @Override
  public Object attribute(String name) {
    return dataModel.attribute(name);
  }

  @Override
  public <T> T attribute(String name, Class<T> targetClass) {
    return dataModel.attribute(name, targetClass);
  }

  @Override
  public Model attribute(String name, Object value) {
    return dataModel.attribute(name, value);
  }

  @Override
  public Model removeAttribute(String name) {
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

}
