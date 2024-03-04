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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartRequest;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Resolves {@link Map} method arguments annotated with an @{@link RequestParam}
 * where the annotation does not specify a request parameter name.
 *
 * <p>The created {@link Map} contains all request parameter name/value pairs,
 * or all multipart files for a given parameter name if specifically declared
 * with {@link MultipartFile} as the value type. If the method parameter type is
 * {@link MultiValueMap} instead, the created map contains all request parameters
 * and all their values for cases where request parameters have multiple values
 * (or multiple multipart files of the same name).
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestParamMethodArgumentResolver
 * @see HttpServletRequest#getParameterMap()
 * @see MultipartRequest#getMultipartFiles()
 * @see MultipartRequest#getFileMap()
 * @since 4.0 2022/4/28 15:26
 */
public class RequestParamMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    if (Map.class.isAssignableFrom(resolvable.getParameterType())) {
      RequestParam requestParam = resolvable.getParameterAnnotation(RequestParam.class);
      return requestParam != null
              && StringUtils.isBlank(requestParam.name());
    }
    return false;
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    ResolvableType resolvableType = resolvable.getResolvableType();

    if (MultiValueMap.class.isAssignableFrom(resolvable.getParameterType())) {
      // MultiValueMap
      Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
      if (valueType == MultipartFile.class) {
        return context.getMultipartRequest().getMultipartFiles();
      }
      else if (valueType == Multipart.class) {
        return context.getMultipartRequest().multipartData();
      }
      else if (ServletDetector.runningInServlet(context) && valueType == Part.class) {
        if (context.isMultipart()) {
          Collection<Part> parts = ServletUtils.getServletRequest(context).getParts();
          LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
          for (Part part : parts) {
            result.add(part.getName(), part);
          }
          return result;
        }
        return new LinkedMultiValueMap<>();
      }
      else {
        return context.getParameters();
      }
    }

    else {
      // Regular Map
      Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
      if (valueType == MultipartFile.class) {
        return context.getMultipartRequest().getFileMap();
      }
      else if (valueType == Multipart.class) {
        return context.getMultipartRequest().multipartData().toSingleValueMap();
      }
      else if (ServletDetector.runningInServlet(context) && valueType == Part.class) {
        if (context.isMultipart()) {
          Collection<Part> parts = ServletUtils.getServletRequest(context).getParts();
          LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
          for (Part part : parts) {
            if (!result.containsKey(part.getName())) {
              result.put(part.getName(), part);
            }
          }
          return result;
        }
        return new LinkedHashMap<>();
      }
      else {
        return context.getParameters().toSingleValueMap();
      }
    }
  }

}
