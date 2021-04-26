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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.exception.MethodNotAllowedException;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.PatternHandler;

/**
 * @author TODAY 2021/3/10 11:33
 * @see ActionMapping#params()
 * @see ActionMapping#consumes()
 * @see ActionMapping#produces()
 * @since 3.0
 */
public class RequestPathMappingHandlerMethodRegistry extends HandlerMethodRegistry {

  @Override
  public void onStartup(WebApplicationContext context) {
    super.onStartup(context);

    for (final Map.Entry<String, Object> entry : getHandlers().entrySet()) {
      super.logMapping(entry.getKey(), entry.getValue());
    }

    for (final PatternHandler patternHandler : getPatternHandlers()) {
      super.logMapping(patternHandler.getPattern(), patternHandler.getHandler());
    }
  }

  @Override
  protected void logMapping(String handlerKey, Object handler) { }

  @Override
  protected void logHandlerReplaced(String handlerKey, Object oldHandler, Object newHandler) {
    if (!(oldHandler instanceof AnnotationMappingInfo) && !(oldHandler instanceof List)) {
      super.logHandlerReplaced(handlerKey, oldHandler, newHandler);
    }
  }

  @Override
  protected String computeKey(RequestContext context) {
    return context.getRequestPath();
  }

  @Override
  protected void mappingHandlerMethod(HandlerMethod handler,
                                      AnnotationAttributes controllerMapping,
                                      AnnotationAttributes[] annotationAttributes) {
    final AnnotationAttributes mapping = new AnnotationAttributes(ActionMapping.class);
    for (final AnnotationAttributes actionMapping : annotationAttributes) {
      mergeMappingAttributes(mapping, actionMapping, controllerMapping);
      for (final String path : mapping.getStringArray("value")) { // url on method
        final String pathPattern = getRequestPathPattern(path);
        // transform
        handler = transformHandlerMethod(pathPattern, handler);
        final AnnotationMappingInfo mappingInfo = new AnnotationMappingInfo(mapping, handler);
        registerHandler(pathPattern, mappingInfo);
      }
      mapping.clear(); // for next mapping
    }
  }

  @Override
  protected Object transformHandler(String handlerKey, Object handler) {
    if (handler instanceof AnnotationMappingInfo) {
      AnnotationMappingInfo mapping = (AnnotationMappingInfo) handler;
      final HandlerMethod handlerMethod = mapping.getHandler();
      final Object transformed = super.transformHandler(handlerKey, handlerMethod);
      if (transformed != handlerMethod) {
        mapping = new AnnotationMappingInfo(mapping, (HandlerMethod) transformed);
      }
      return mapping;
    }
    return super.transformHandler(handlerKey, handler);
  }

  /*private for testing */
  void mergeMappingAttributes(AnnotationAttributes mapping,
                              AnnotationAttributes actionMapping,
                              AnnotationAttributes controllerMapping) {
    if (ObjectUtils.isEmpty(controllerMapping) || actionMapping.getBoolean("exclude")) {
      mapping.putAll(actionMapping);
      return;
    }

    doMergeMapping(mapping, actionMapping, controllerMapping, Constant.VALUE, true);
    doMergeMapping(mapping, actionMapping, controllerMapping, "params");
    doMergeMapping(mapping, actionMapping, controllerMapping, "produces");
    doMergeMapping(mapping, actionMapping, controllerMapping, "consumes");
    doMergeMapping(mapping, actionMapping, controllerMapping, "method", RequestMethod[].class, false);
  }

  private void doMergeMapping(AnnotationAttributes mapping,
                              AnnotationAttributes actionMapping,
                              AnnotationAttributes controllerMapping, String key) {
    doMergeMapping(mapping, actionMapping, controllerMapping, key, String[].class, false);
  }

  private void doMergeMapping(AnnotationAttributes mapping,
                              AnnotationAttributes actionMapping,
                              AnnotationAttributes controllerMapping,
                              String key, boolean append) {
    doMergeMapping(mapping, actionMapping, controllerMapping, key, String[].class, append);
  }

  private <T> void doMergeMapping(AnnotationAttributes mapping,
                                  AnnotationAttributes actionMapping,
                                  AnnotationAttributes controllerMapping,
                                  String key, Class<T> requiredType, boolean append) {

    final T attribute = controllerMapping.getAttribute(key, requiredType);
    final int length = Array.getLength(attribute);
    if (length == 0) {
      mapping.put(key, actionMapping.getAttribute(key, requiredType));
    }
    else {
      LinkedHashSet<Object> values = new LinkedHashSet<>();
      final T actionAttribute = actionMapping.getAttribute(key, requiredType);
      final int actionLength = Array.getLength(actionAttribute);
      if (append) {
        for (int i = 0; i < length; i++) {
          final Object classElement = Array.get(attribute, i);
          final String parentPath = StringUtils.checkUrl(classElement.toString());
          final boolean appendParentPathEmpty = !parentPath.isEmpty();
          for (int j = 0; j < actionLength; j++) {
            final Object element = Array.get(actionAttribute, j);
            if (appendParentPathEmpty) {
              values.add(parentPath.concat(StringUtils.checkUrl(element.toString())));
            }
            else {
              values.add(StringUtils.checkUrl(element.toString()));
            }
          }
        }
      }
      else {
//      Collections.addAll(values, actionAttribute);
        for (int i = 0; i < length; i++) {
          final Object element = Array.get(attribute, i);
          values.add(element);
        }
        for (int i = 0; i < actionLength; i++) {
          final Object element = Array.get(actionAttribute, i);
          values.add(element);
        }
      }
      final Object array = Array.newInstance(requiredType.getComponentType(), values.size());
      mapping.put(key, values.toArray((Object[]) array));
    }
  }

  @Override
  public void registerHandler(String handlerKey, Object handler) {
    if (handler instanceof AnnotationMappingInfo) {
      registerHandler(handlerKey, (AnnotationMappingInfo) handler);
    }
    else {
      super.registerHandler(handlerKey, handler);
    }
  }

  @SuppressWarnings("unchecked")
  private void registerHandler(String requestPath, AnnotationMappingInfo handler) {
    final Object existMappingInfo = getExistMappingInfo(requestPath);
    if (existMappingInfo instanceof List) {
      // List<ActionMappingInfo>
      final List<AnnotationMappingInfo> mappingInfo = (List<AnnotationMappingInfo>) existMappingInfo;
      mappingInfo.add(handler);
      sort(mappingInfo);
    }
    else if (existMappingInfo instanceof AnnotationMappingInfo) {
      // ActionMappingInfo
      final LinkedList<AnnotationMappingInfo> mappingInfo = new LinkedList<>();
      mappingInfo.add(handler);
      mappingInfo.add((AnnotationMappingInfo) existMappingInfo);
      sort(mappingInfo);
      super.registerHandler(requestPath, mappingInfo);
    }
    else {
      super.registerHandler(requestPath, handler);
    }
  }

  /**
   * If a handler has already exits in this registry
   *
   * @param requestPath
   *         handler key
   *
   * @return a exits Handler
   */
  private Object getExistMappingInfo(String requestPath) {
    Object existMappingInfo = getHandlers().get(requestPath);
    if (existMappingInfo == null && getPathMatcher().isPattern(requestPath)) {
      final List<PatternHandler> patternHandlers = getPatternHandlers();
      if (ObjectUtils.isNotEmpty(patternHandlers)) {
        for (final PatternHandler patternHandler : patternHandlers) {
          if (Objects.equals(patternHandler.getPattern(), requestPath)) {
            return patternHandler.getHandler();
          }
        }
      }
    }
    return existMappingInfo;
  }

  private void sort(List<AnnotationMappingInfo> handlers) {
    OrderUtils.reversedSort(handlers);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object lookupHandler(String requestPath, RequestContext context) {
    final Object handler = super.lookupHandler(requestPath, context);
    if (handler != null) {
      if (handler instanceof AnnotationMappingInfo) {  // single MappingInfo
        final AnnotationMappingInfo mappingInfo = (AnnotationMappingInfo) handler;
        if (testMapping(mappingInfo, context)) {
          return mappingInfo.getHandler();
        }
        // cannot pass the condition
        return handleNoMatch(
                Collections.singletonList((AnnotationMappingInfo) handler), requestPath, context);
      }
      else if (handler instanceof List) { // list of MappingInfo
        for (final AnnotationMappingInfo mappingInfo : ((List<AnnotationMappingInfo>) handler)) {
          if (testMapping(mappingInfo, context)) {
            return mappingInfo.getHandler();
          }
        }
        // cannot pass the condition
        return handleNoMatch((List<AnnotationMappingInfo>) handler, requestPath, context);
      }
    }
    return handler;
  }

  /**
   * Invoked when no matching mapping is not found.
   *
   * @param mappings
   *         all registered mappings
   * @param requestPath
   *         mapping request path
   *
   * @throws MethodNotAllowedException
   *         If method is not allowed
   */
  protected Object handleNoMatch(
          List<AnnotationMappingInfo> mappings, String requestPath, RequestContext context) {

    boolean methodNotAllowed = true;
    final String requestMethod = context.getMethod();
    final HashSet<RequestMethod> supportedMethods = new HashSet<>();
    for (final AnnotationMappingInfo mapping : mappings) {
      Collections.addAll(supportedMethods, mapping.method());
      // test if its not a supported method
      for (final RequestMethod testMethod : mapping.method()) {
        if (requestMethod.equals(testMethod.name())) {
          methodNotAllowed = false;
          break;
        }
      }
    }

    if (methodNotAllowed) {
      throw new MethodNotAllowedException(requestMethod, supportedMethods.toArray(new RequestMethod[0]));
    }

    return null;
  }

  protected boolean testMapping(final AnnotationMappingInfo mappingInfo, final RequestContext context) {
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
        return false;
      }
    }

    // test contentType
    final MediaType[] consumes = mappingInfo.consumes();
    if (consumes != null) {
      final MediaType contentType = context.requestHeaders().getContentType();
      for (final MediaType consume : consumes) {
        if (!consume.includes(contentType)) {
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

    // test produces (Accept header)
    final MediaType[] produces = mappingInfo.produces();
    if (produces != null) {
      final List<MediaType> acceptedMediaTypes = context.requestHeaders().getAccept();
      for (final MediaType produce : produces) {
        if (!matchMediaType(produce, acceptedMediaTypes)) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean matchMediaType(MediaType accept, List<MediaType> acceptedMediaTypes) {
    for (MediaType acceptedMediaType : acceptedMediaTypes) {
      if (accept.isCompatibleWith(acceptedMediaType) && matchParameters(accept, acceptedMediaType)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchParameters(MediaType accept, MediaType acceptedMediaType) {
    for (String name : accept.getParameters().keySet()) {
      String s1 = accept.getParameter(name);
      String s2 = acceptedMediaType.getParameter(name);
      if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
        return false;
      }
    }
    return true;
  }
}
