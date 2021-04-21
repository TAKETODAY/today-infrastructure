/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.registry;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.utils.MediaType;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.MappingInfo;
import cn.taketoday.web.annotation.RequestParameter;
import cn.taketoday.web.exception.MethodNotAllowedException;

/**
 * @author TODAY 2021/3/10 11:33
 * @see ActionMapping#params()
 * @see ActionMapping#consumes()
 * @see ActionMapping#produces()
 * @since 3.0
 */
public class RequestPathMappingHandlerMethodRegistry extends HandlerMethodRegistry {

  @Override
  protected String computeKey(RequestContext context) {
    return context.getRequestPath();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerHandler(String handlerKey, Object handler) {
    if (handler instanceof MappingInfo) {
      final Object existMappingInfo = getHandlers().get(handlerKey);
      if (existMappingInfo instanceof List) {
        // List<ActionMappingInfo>
        ((List<Object>) existMappingInfo).add(handler);
        return;
      }
      else if (existMappingInfo instanceof MappingInfo) {
        // ActionMappingInfo
        final LinkedList<Object> infos = new LinkedList<>();
        infos.add(handler);
        handler = infos;
      }
    }
    super.registerHandler(handlerKey, handler);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object lookupHandler(String handlerKey, RequestContext context) {
    final Object handler = super.lookupHandler(handlerKey, context);
    if (handler instanceof MappingInfo) {  // single MappingInfo
      final MappingInfo mappingInfo = (MappingInfo) handler;
      if (testMapping(mappingInfo, context)) {
        return mappingInfo.getHandler();
      }
      // cannot pass the condition
      return null;
    }
    else if (handler instanceof List) { // list of MappingInfo
      for (final MappingInfo mappingInfo : ((List<MappingInfo>) handler)) {
        if (testMapping(mappingInfo, context)) {
          return mappingInfo.getHandler();
        }
      }
      // cannot pass the condition
      return null;
    }
    return handler;
  }

  /**
   * @throws MethodNotAllowedException
   */
  protected boolean testMapping(final MappingInfo mappingInfo, final RequestContext context) {
    // test request method
    final RequestMethod[] supportedMethods = mappingInfo.method();
    if (supportedMethods != null) {
      final String requestMethod = context.getMethod();
      boolean matched = false;
      for (final RequestMethod testMethod : supportedMethods) {
        if (requestMethod.equals(testMethod.name())) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        throw new MethodNotAllowedException(requestMethod, supportedMethods);
      }
    }

    // test contentType
    final MediaType[] consumes = mappingInfo.consumes();
    if (consumes != null) {
      final MediaType contentType = context.requestHeaders().getContentType();
      for (final MediaType consume : consumes) {
        if (!consume.isCompatibleWith(contentType)) {
          return false;
        }
      }
    }
    // test params
    final RequestParameter[] params = mappingInfo.params();
    if (params != null) {
      for (final RequestParameter param : params) {
        final String name = param.getName();
        final String parameter = context.getParameter(name);
        if (parameter != null) {
          // test parameter value
          final String value = param.getValue();
          if (value != null && !Objects.equals(value, parameter)) {
            return false;
          }
        }
        else {
          return false;
        }
      }
    }

    //

    return true;
  }
}
