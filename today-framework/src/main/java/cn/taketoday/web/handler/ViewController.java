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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.handler;

import java.lang.reflect.Method;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.ResolvableParameterFactory;

/**
 * Views request mapping
 *
 * @author TODAY <br>
 * 2018-06-25 19:58:07
 */
public class ViewController extends OrderedSupport {

  /** 资源路径 */
  private Object resource;
  /** The resource's content type @since 2.3.3 */
  private String contentType = null;

  /** The request status @since 2.3.7 */
  private Integer status;

  private final ActionMappingAnnotationHandler handlerMethod;

  public ViewController() {
    this(null, null, null);
  }

  public ViewController(int order) {
    super(order);
    this.handlerMethod = null;
  }

  public ViewController(Object bean, Method method, ResolvableParameterFactory parameterFactory) { // FIXME
    this.handlerMethod = (method == null) ? null : ActionMappingAnnotationHandler.from(bean, method, parameterFactory);
  }

  public boolean hasAction() {
    return getHandler() != null;
  }

  public Integer getStatus() {
    return status;
  }

  public String getContentType() {
    return contentType;
  }

  public Object getResource() {
    return resource;
  }

  public ViewController setStatus(Integer status) {
    this.status = status;
    return this;
  }

  public ViewController setResource(Object resource) {
    this.resource = resource;
    return this;
  }

  public ViewController setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("status", status)
            .append("resource", resource)
            .append("contentType", contentType)
            .toString();
  }

  public ActionMappingAnnotationHandler getHandler() {
    return handlerMethod;
  }
}
