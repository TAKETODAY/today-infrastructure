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

package cn.taketoday.web.registry;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
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

    for (Map.Entry<String, Object> entry : getHandlers().entrySet()) {
      super.logMapping(entry.getKey(), entry.getValue());
    }

    List<PatternHandler> patternHandlers = getPatternHandlers();
    if (CollectionUtils.isNotEmpty(patternHandlers)) {
      for (PatternHandler patternHandler : patternHandlers) {
        super.logMapping(patternHandler.getPattern(), patternHandler.getHandler());
      }
    }
  }

  @Override
  protected void logMapping(String handlerKey, Object handler) {
    // no-op
  }

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
                                      @Nullable MergedAnnotation<ActionMapping> controllerMapping,
                                      MergedAnnotation<ActionMapping> handlerMethodMapping) {
    AnnotationAttributes mapping = new AnnotationAttributes(ActionMapping.class);
    mergeMappingAttributes(mapping, handlerMethodMapping, controllerMapping);
    for (String path : mapping.getStringArray("value")) { // url on method
      String pathPattern = getRequestPathPattern(path);
      // transform
      handler = transformHandlerMethod(pathPattern, handler);
      AnnotationMappingInfo mappingInfo = new AnnotationMappingInfo(mapping, handler);
      registerMappingInfo(pathPattern, mappingInfo);
    }
    mapping.clear(); // for next mapping
  }

  @Override
  protected Object transformHandler(String handlerKey, Object handler) {
    if (handler instanceof AnnotationMappingInfo mapping) {
      HandlerMethod handlerMethod = mapping.getHandler();
      Object transformed = super.transformHandler(handlerKey, handlerMethod);
      if (transformed != handlerMethod) {
        mapping = new AnnotationMappingInfo(mapping, (HandlerMethod) transformed);
      }
      return mapping;
    }
    return super.transformHandler(handlerKey, handler);
  }

  /*private for testing */
  void mergeMappingAttributes(AnnotationAttributes mapping,
                              MergedAnnotation<ActionMapping> actionMapping,
                              @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    AnnotationAttributes actionAttr = actionMapping.asAnnotationAttributes();
    if (controllerMapping == null || actionMapping.getBoolean("exclude")) {
      mapping.putAll(actionAttr);
      return;
    }

    AnnotationAttributes controllerAttr = controllerMapping.asAnnotationAttributes();
    doMergeMapping(mapping, actionAttr, controllerAttr, MergedAnnotation.VALUE, String[].class, true);
    doMergeMapping(mapping, actionAttr, controllerAttr, "params", String[].class, false);
    doMergeMapping(mapping, actionAttr, controllerAttr, "produces", String[].class, false);
    doMergeMapping(mapping, actionAttr, controllerAttr, "consumes", String[].class, false);
    doMergeMapping(mapping, actionAttr, controllerAttr, "method", HttpMethod[].class, false);
  }

  private <T> void doMergeMapping(AnnotationAttributes mapping,
                                  AnnotationAttributes actionMapping,
                                  AnnotationAttributes controllerMapping,
                                  String key, Class<T> requiredType, boolean append) {

    T attribute = controllerMapping.getRequiredAttribute(key, requiredType);
    int length = Array.getLength(attribute);
    if (length == 0) {
      mapping.put(key, actionMapping.getRequiredAttribute(key, requiredType));
    }
    else {
      LinkedHashSet<Object> values = new LinkedHashSet<>();
      T actionAttribute = actionMapping.getRequiredAttribute(key, requiredType);
      int actionLength = Array.getLength(actionAttribute);
      if (append) {
        for (int i = 0; i < length; i++) {
          Object classElement = Array.get(attribute, i);
          String parentPath = StringUtils.formatURL(classElement.toString());
          boolean appendParentPathEmpty = !parentPath.isEmpty();
          for (int j = 0; j < actionLength; j++) {
            Object element = Array.get(actionAttribute, j);
            if (appendParentPathEmpty) {
              values.add(parentPath.concat(StringUtils.formatURL(element.toString())));
            }
            else {
              values.add(StringUtils.formatURL(element.toString()));
            }
          }
        }
      }
      else {
//      Collections.addAll(values, actionAttribute);
        for (int i = 0; i < length; i++) {
          Object element = Array.get(attribute, i);
          values.add(element);
        }
        for (int i = 0; i < actionLength; i++) {
          Object element = Array.get(actionAttribute, i);
          values.add(element);
        }
      }
      Object array = Array.newInstance(requiredType.getComponentType(), values.size());
      mapping.put(key, values.toArray((Object[]) array));
    }
  }

  @Override
  public void registerHandler(String handlerKey, Object handler) {
    if (handler instanceof AnnotationMappingInfo) {
      registerMappingInfo(handlerKey, (AnnotationMappingInfo) handler);
    }
    else {
      super.registerHandler(handlerKey, handler);
    }
  }

  @SuppressWarnings("unchecked")
  private void registerMappingInfo(String requestPath, AnnotationMappingInfo handler) {
    Object existMappingInfo = getExistMappingInfo(requestPath);
    if (existMappingInfo instanceof List) {
      // List<ActionMappingInfo>
      List<AnnotationMappingInfo> mappingInfo = (List<AnnotationMappingInfo>) existMappingInfo;
      mappingInfo.add(handler);
      sort(mappingInfo);
    }
    else if (existMappingInfo instanceof AnnotationMappingInfo) {
      // ActionMappingInfo
      ArrayList<AnnotationMappingInfo> mappingInfo = new ArrayList<>();
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
   * @param requestPath handler key
   * @return a exits Handler
   */
  private Object getExistMappingInfo(String requestPath) {
    Object existMappingInfo = getHandlers().get(requestPath);
    if (existMappingInfo == null && getPathMatcher().isPattern(requestPath)) {
      List<PatternHandler> patternHandlers = getPatternHandlers();
      if (CollectionUtils.isNotEmpty(patternHandlers)) {
        for (PatternHandler patternHandler : patternHandlers) {
          if (Objects.equals(patternHandler.getPattern(), requestPath)) {
            return patternHandler.getHandler();
          }
        }
      }
    }
    return existMappingInfo;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object lookupHandler(String requestPath, RequestContext context) {
    Object handler = super.lookupHandler(requestPath, context);
    if (handler != null) {
      if (handler instanceof AnnotationMappingInfo mappingInfo) {  // single MappingInfo
        if (testMapping(mappingInfo, context)) {
          return mappingInfo.getHandler();
        }
        // cannot pass the condition
        return null;
      }
      else if (handler instanceof List) { // list of MappingInfo
        for (AnnotationMappingInfo mappingInfo : ((List<AnnotationMappingInfo>) handler)) {
          if (testMapping(mappingInfo, context)) {
            return mappingInfo.getHandler();
          }
        }
        // cannot pass the condition
        return null;
      }
    }
    return handler;
  }

  protected boolean testMapping(AnnotationMappingInfo mappingInfo, RequestContext context) {
    // test request method
    HttpMethod[] supportedMethods = mappingInfo.method();
    if (supportedMethods != null) {
      String requestMethod = context.getMethod();
      boolean matched = false;
      for (HttpMethod testMethod : supportedMethods) {
        if (testMethod.matches(requestMethod)) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }

    // test contentType
    MediaType[] consumes = mappingInfo.consumes();
    if (consumes != null) {
      MimeType contentType = MimeType.valueOf(context.getContentType());
      for (MediaType consume : consumes) {
        if (!consume.includes(contentType)) {
          return false;
        }
      }
    }
    // test params
    RequestParameter[] params = mappingInfo.params();
    if (params != null) {
      for (RequestParameter param : params) {
        if (!param.matches(context)) {
          return false;
        }
      }
    }

    // test produces (Accept header)
    MediaType[] produces = mappingInfo.produces();
    if (produces != null) {
      List<MediaType> acceptedMediaTypes = context.requestHeaders().getAccept();
      for (MediaType produce : produces) {
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
    for (Map.Entry<String, String> entry : accept.getParameters().entrySet()) {
      String s1 = entry.getValue();
      String s2 = acceptedMediaType.getParameter(entry.getKey());
      if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
        return false;
      }
    }
    return true;
  }
}
