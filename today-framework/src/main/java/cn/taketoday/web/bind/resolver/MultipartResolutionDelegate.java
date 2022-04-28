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

package cn.taketoday.web.bind.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:44
 */
public class MultipartResolutionDelegate {

  /**
   * Indicates an unresolvable value.
   */
  public static final Object UNRESOLVABLE = new Object();

  private static boolean isMultipartContent(HttpServletRequest request) {
    String contentType = request.getContentType();
    return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
  }

  public static boolean isMultipartArgument(MethodParameter parameter) {
    Class<?> paramType = parameter.getNestedParameterType();
    return MultipartFile.class == paramType
            || isMultipartFileCollection(parameter)
            || isMultipartFileArray(parameter)
            || (
            ServletDetector.isPresent
                    && (
                    Part.class == paramType
                            || ServletDelegate.isPartCollection(parameter)
                            || ServletDelegate.isPartArray(parameter)
            )
    );
  }

  @Nullable
  public static Object resolveMultipartArgument(
          String name, MethodParameter parameter, RequestContext request) throws Exception {

    MultipartRequest multipartRequest = request.getMultipartRequest();

    if (MultipartFile.class == parameter.getNestedParameterType()) {
      if (!request.isMultipart()) {
        return null;
      }
      return multipartRequest.getFile(name);
    }
    else if (isMultipartFileCollection(parameter)) {
      if (!request.isMultipart()) {
        return null;
      }
      List<MultipartFile> files = multipartRequest.getFiles(name);
      return !files.isEmpty() ? files : null;
    }
    else if (isMultipartFileArray(parameter)) {
      if (!request.isMultipart()) {
        return null;
      }
      List<MultipartFile> files = multipartRequest.getFiles(name);
      return !files.isEmpty() ? files.toArray(new MultipartFile[0]) : null;
    }
    else {
      if (ServletDetector.runningInServlet(request)) {
        return ServletDelegate.resolvePart(request, name, parameter);

      }
      return UNRESOLVABLE;
    }
  }

  private static boolean isMultipartFileCollection(MethodParameter methodParam) {
    return (MultipartFile.class == getCollectionParameterType(methodParam));
  }

  private static boolean isMultipartFileArray(MethodParameter methodParam) {
    return (MultipartFile.class == methodParam.getNestedParameterType().getComponentType());
  }

  @Nullable
  private static Class<?> getCollectionParameterType(MethodParameter methodParam) {
    Class<?> paramType = methodParam.getNestedParameterType();
    if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
      return ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
    }
    return null;
  }

  static class ServletDelegate {

    static Object resolvePart(RequestContext request, String name, MethodParameter parameter) throws Exception {
      HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);

      if (Part.class == parameter.getNestedParameterType()) {
        if (!request.isMultipart()) {
          return null;
        }
        return ServletUtils.getPart(servletRequest, name);
      }
      else if (isPartCollection(parameter)) {
        if (!request.isMultipart()) {
          return null;
        }
        List<Part> parts = resolvePartList(servletRequest, name);
        return !parts.isEmpty() ? parts : null;
      }
      else if (isPartArray(parameter)) {
        if (!request.isMultipart()) {
          return null;
        }
        List<Part> parts = resolvePartList(servletRequest, name);
        return !parts.isEmpty() ? parts.toArray(new Part[0]) : null;
      }

      return UNRESOLVABLE;
    }

    static List<Part> resolvePartList(HttpServletRequest request, String name) throws Exception {
      Collection<Part> parts = request.getParts();
      List<Part> result = new ArrayList<>(parts.size());
      for (Part part : parts) {
        if (part.getName().equals(name)) {
          result.add(part);
        }
      }
      return result;
    }

    static boolean isPartCollection(MethodParameter methodParam) {
      return (Part.class == getCollectionParameterType(methodParam));
    }

    static boolean isPartArray(MethodParameter methodParam) {
      return (Part.class == methodParam.getNestedParameterType().getComponentType());
    }

  }

}
