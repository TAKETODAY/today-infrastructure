/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.web.RequestContext;
import infra.web.multipart.Part;

/**
 * A common delegate for {@code ParameterResolvingStrategy} implementations
 * which need to resolve {@link Part} arguments.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:44
 */
final class MultipartResolutionDelegate {

  private MultipartResolutionDelegate() {
  }

  /**
   * Indicates an unresolvable value.
   */
  public static final Object UNRESOLVABLE = new Object();

  public static boolean isMultipartArgument(MethodParameter parameter) {
    Class<?> paramType = parameter.getNestedParameterType();
    return Part.class.isAssignableFrom(paramType)
            || isMultipartCollection(parameter, paramType)
            || isMultipartArray(paramType);
  }

  @Nullable
  public static Object resolveMultipartArgument(String name, MethodParameter parameter, RequestContext request) throws IOException {
    if (!request.isMultipart()) {
      if (isMultipartArgument(parameter)) {
        return null;
      }
      return UNRESOLVABLE;
    }

    Class<?> parameterType = parameter.getNestedParameterType();
    if (Part.class.isAssignableFrom(parameterType)) {
      return request.asMultipartRequest().getPart(name);
    }
    else if (isMultipartCollection(parameter, parameterType)) {
      return request.asMultipartRequest().getParts(name);
    }
    else if (isMultipartArray(parameterType)) {
      List<Part> parts = request.asMultipartRequest().getParts(name);
      if (parts == null) {
        return null;
      }
      return parts.toArray(new Part[parts.size()]);
    }
    return UNRESOLVABLE;
  }

  private static boolean isMultipartCollection(MethodParameter methodParam, Class<?> parameterType) {
    parameterType = getCollectionParameterType(methodParam, parameterType);
    return parameterType != null && Part.class.isAssignableFrom(parameterType);
  }

  private static boolean isMultipartArray(Class<?> parameterType) {
    Class<?> componentType = parameterType.getComponentType();
    return componentType != null && Part.class.isAssignableFrom(componentType);
  }

  @Nullable
  private static Class<?> getCollectionParameterType(MethodParameter methodParam, Class<?> paramType) {
    if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
      return ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
    }
    return null;
  }

}
