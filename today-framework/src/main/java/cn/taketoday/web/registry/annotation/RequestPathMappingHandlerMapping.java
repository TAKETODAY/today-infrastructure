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

package cn.taketoday.web.registry.annotation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.util.pattern.PathPattern;

/**
 * @author TODAY 2021/3/10 11:33
 * @see ActionMapping#params()
 * @see ActionMapping#consumes()
 * @see ActionMapping#produces()
 * @since 3.0
 */
public class RequestPathMappingHandlerMapping extends HandlerMethodMapping {

  @Override
  protected void mappingHandlerMethod(
          ActionMappingAnnotationHandler handler,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping,
          MergedAnnotation<ActionMapping> handlerMethodMapping) {
    AnnotationAttributes mapping = new AnnotationAttributes(ActionMapping.class);
    mergeMappingAttributes(mapping, handlerMethodMapping, controllerMapping);
    for (String path : mapping.getStringArray("path")) { // url on method
      PathPattern pathPattern = getRequestPathPattern(path);
      // transform
      handler = transformHandler(pathPattern, handler);
      AnnotationMappingInfo mappingInfo = new AnnotationMappingInfo(pathPattern, mapping, handler);
      registerMappingInfo(pathPattern, mappingInfo);
    }
    mapping.clear(); // for next mapping
  }

//  @Override
//  protected Object transformHandler(String handlerKey, Object handler) {
//    if (handler instanceof AnnotationMappingInfo mapping) {
//      ActionMappingAnnotationHandler handlerMethod = mapping.getHandler();
//      ActionMappingAnnotationHandler transformed = super.transformHandler(mapping.getPathPattern(), handlerMethod);
//      if (transformed != handlerMethod) {
//        mapping = new AnnotationMappingInfo(mapping, transformed);
//      }
//      return mapping;
//    }
//    return super.transformHandler(handlerKey, handler);
//  }

  /*private for testing */
  void mergeMappingAttributes(AnnotationAttributes mapping,
          MergedAnnotation<ActionMapping> actionMapping,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    AnnotationAttributes actionAttr = actionMapping.asAnnotationAttributes();
    if (controllerMapping == null || !actionMapping.getBoolean("combine")) {
      mapping.putAll(actionAttr);
      return;
    }

    AnnotationAttributes controllerAttr = controllerMapping.asAnnotationAttributes();
    doMergeMapping(mapping, actionAttr, controllerAttr, "path", String[].class, true);
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
  protected void doPutPathPattern(PathPattern pathPattern, Object resolvedHandler) {
    if (resolvedHandler instanceof AnnotationMappingInfo handler) {
      registerMappingInfo(pathPattern, handler);
    }
    else {
      super.doPutPathPattern(pathPattern, resolvedHandler);
    }
  }

  @SuppressWarnings("unchecked")
  private void registerMappingInfo(PathPattern pathPattern, AnnotationMappingInfo handler) {
    Object existMappingInfo = getExistMappingInfo(pathPattern);
    if (existMappingInfo instanceof List mappingInfo) {
      // List<ActionMappingInfo>
      mappingInfo.add(handler);
      OrderComparator.sort(mappingInfo);
    }
    else if (existMappingInfo instanceof AnnotationMappingInfo) {
      // ActionMappingInfo
      ArrayList<AnnotationMappingInfo> mappingInfo = new ArrayList<>();
      mappingInfo.add(handler);
      mappingInfo.add((AnnotationMappingInfo) existMappingInfo);
      OrderComparator.sort(mappingInfo);
      super.doPutPathPattern(pathPattern, mappingInfo);
    }
    else {
      super.doPutPathPattern(pathPattern, handler);
    }
  }

  /**
   * If a handler has already exits in this registry
   *
   * @param pathPattern pathPattern
   * @return a exits Handler
   */
  private Object getExistMappingInfo(PathPattern pathPattern) {
    Object existMappingInfo = getHandlerMap().get(pathPattern.getPatternString());
    if (existMappingInfo == null && pathPattern.hasPatternSyntax()) {
      Map<PathPattern, Object> patternHandlers = getPathPatternHandlerMap();
      if (CollectionUtils.isNotEmpty(patternHandlers)) {
        for (Map.Entry<PathPattern, Object> entry : patternHandlers.entrySet()) {
          if (Objects.equals(entry.getKey(), pathPattern)) {
            return entry.getValue();
          }
        }
      }
    }
    return existMappingInfo;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  protected Object lookupHandler(RequestContext request) {
    Object handler = super.lookupHandler(request);
    if (handler != null) {
      if (handler instanceof AnnotationMappingInfo mappingInfo) {  // single MappingInfo
        if (testMapping(mappingInfo, request)) {
          return mappingInfo.getHandler();
        }
        // cannot pass the condition
        return null;
      }
      else if (handler instanceof List) { // list of MappingInfo
        for (AnnotationMappingInfo mappingInfo : ((List<AnnotationMappingInfo>) handler)) {
          if (testMapping(mappingInfo, request)) {
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
      String requestMethod = context.getMethodValue();
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
    HandlerMatchingMetadata matchingMetadata;

    // test produces (Accept header)
    MediaType[] produces = mappingInfo.produces();
    if (produces != null) {
      List<MediaType> acceptedMediaTypes = context.requestHeaders().getAccept();
      for (MediaType produce : produces) {
        if (!matchMediaType(produce, acceptedMediaTypes)) {
          return false;
        }
      }
      matchingMetadata = new HandlerMatchingMetadata(
              mappingInfo.getHandler(),
              context.getRequestPath(), context.getLookupPath(), mappingInfo.getPathPattern(), getPatternParser());
      matchingMetadata.setProducibleMediaTypes(produces.clone());
    }
    else {
      matchingMetadata = new HandlerMatchingMetadata(
              mappingInfo.getHandler(),
              context.getRequestPath(), context.getLookupPath(), mappingInfo.getPathPattern(), getPatternParser());
    }
    context.setMatchingMetadata(matchingMetadata);
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
