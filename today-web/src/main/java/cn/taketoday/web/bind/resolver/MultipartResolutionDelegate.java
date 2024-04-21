/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * A common delegate for {@code ParameterResolvingStrategy} implementations
 * which need to resolve {@link MultipartFile} arguments.
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
            || isMultipartArray(paramType);
  }

  @Nullable
  public static Object resolveMultipartArgument(String name, MethodParameter parameter, RequestContext request) {
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

}
