/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * A common delegate for {@code ParameterResolvingStrategy} implementations
 * which need to resolve {@link MultipartFile} and {@link Part} arguments.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:44
 */
final class MultipartResolutionDelegate {

  /**
   * Indicates an unresolvable value.
   */
  public static final Object UNRESOLVABLE = new Object();

  public static boolean isMultipartArgument(MethodParameter parameter) {
    Class<?> paramType = parameter.getNestedParameterType();
    return Multipart.class.isAssignableFrom(paramType)
            || isMultipartCollection(parameter, paramType)
            || isMultipartArray(paramType)
            || (
            ServletDetector.isPresent
                    && (
                    ServletDelegate.isPart(paramType)
                            || ServletDelegate.isPartArray(paramType)
                            || ServletDelegate.isPartCollection(parameter, paramType)
            )
    );
  }

  @Nullable
  public static Object resolveMultipartArgument(String name, MethodParameter parameter, RequestContext request) throws Exception {
    if (!request.isMultipart()) {
      if (isMultipartArgument(parameter)) {
        return null;
      }
      return UNRESOLVABLE;
    }

    Class<?> parameterType = parameter.getNestedParameterType();
    if (Multipart.class.isAssignableFrom(parameterType)) {
      return CollectionUtils.firstElement(request.getMultipartRequest().multipartData(name));
    }
    else if (isMultipartCollection(parameter, parameterType)) {
      return request.getMultipartRequest().multipartData(name);
    }
    else if (isMultipartArray(parameterType)) {
      List<Multipart> parts = request.getMultipartRequest().multipartData(name);
      if (parts == null) {
        return null;
      }
      if (parameterType.getComponentType() == MultipartFile.class) {
        return parts.toArray(new MultipartFile[parts.size()]);
      }
      return parts.toArray(new Multipart[parts.size()]);
    }
    else {
      if (ServletDetector.runningInServlet(request)) {
        return ServletDelegate.resolvePart(request, name, parameter, parameterType);
      }
    }
    return UNRESOLVABLE;
  }

  private static boolean isMultipartCollection(MethodParameter methodParam, Class<?> parameterType) {
    parameterType = getCollectionParameterType(methodParam, parameterType);
    return parameterType != null && Multipart.class.isAssignableFrom(parameterType);
  }

  private static boolean isMultipartArray(Class<?> parameterType) {
    Class<?> componentType = parameterType.getComponentType();
    return componentType != null && Multipart.class.isAssignableFrom(componentType);
  }

  @Nullable
  private static Class<?> getCollectionParameterType(MethodParameter methodParam, Class<?> paramType) {
    if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
      return ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
    }
    return null;
  }

  static class ServletDelegate {

    static Object resolvePart(RequestContext request,
            String name, MethodParameter parameter, Class<?> paramType) throws Exception {

      HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
      if (Part.class == paramType) {
        return ServletUtils.getPart(servletRequest, name);
      }
      else if (isPartCollection(parameter, paramType)) {
        List<Part> parts = resolvePartList(servletRequest, name);
        return !parts.isEmpty() ? parts : null;
      }
      else if (isPartArray(paramType)) {
        List<Part> parts = resolvePartList(servletRequest, name);
        return !parts.isEmpty() ? parts.toArray(new Part[0]) : null;
      }
      return UNRESOLVABLE;
    }

    static List<Part> resolvePartList(HttpServletRequest request, String name) throws Exception {
      Collection<Part> parts = request.getParts();
      ArrayList<Part> result = new ArrayList<>(parts.size());
      for (Part part : parts) {
        if (part.getName().equals(name)) {
          result.add(part);
        }
      }
      return result;
    }

    static boolean isPart(Class<?> paramType) {
      return Part.class == paramType;
    }

    static boolean isPartCollection(MethodParameter methodParam, Class<?> paramType) {
      return Part.class == getCollectionParameterType(methodParam, paramType);
    }

    static boolean isPartArray(Class<?> paramType) {
      return Part.class == paramType.getComponentType();
    }

  }

}
