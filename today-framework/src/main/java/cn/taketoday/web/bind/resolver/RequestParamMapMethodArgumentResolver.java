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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.config.WebMvcProperties;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
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
 * @see MultipartRequest#getMultiFileMap()
 * @see MultipartRequest#getFileMap()
 * @since 4.0 2022/4/28 15:26
 */
public class RequestParamMapMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    RequestParam requestParam = resolvable.getParameterAnnotation(RequestParam.class);
    return requestParam != null
            && Map.class.isAssignableFrom(resolvable.getParameterType())
            && !StringUtils.hasText(requestParam.name());
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    ResolvableType resolvableType = resolvable.getResolvableType();

    if (MultiValueMap.class.isAssignableFrom(resolvable.getParameterType())) {
      // MultiValueMap
      Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
      if (valueType == MultipartFile.class) {
        MultipartRequest multipartRequest = context.getMultipartRequest();
        return (multipartRequest != null ? multipartRequest.getMultiFileMap() : new LinkedMultiValueMap<>(0));
      }
      else if (ServletDetector.runningInServlet(context) && valueType == Part.class) {
        if (context.isMultipart()) {
          HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
          Collection<Part> parts = servletRequest.getParts();
          LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
          for (Part part : parts) {
            result.add(part.getName(), part);
          }
          return result;
        }
        return new LinkedMultiValueMap<>(0);
      }
      else {
        Map<String, String[]> parameterMap = context.getParameters();
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
          for (String value : values) {
            result.add(key, value);
          }
        });
        return result;
      }
    }

    else {
      // Regular Map
      Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
      if (valueType == MultipartFile.class) {
        MultipartRequest multipartRequest = context.getMultipartRequest();
        return multipartRequest.getFileMap();
      }
      else if (valueType == Part.class) {
        if (context.isMultipart()) {
          HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
          Collection<Part> parts = servletRequest.getParts();
          LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
          for (Part part : parts) {
            if (!result.containsKey(part.getName())) {
              result.put(part.getName(), part);
            }
          }
          return result;
        }
        return new LinkedHashMap<>(0);
      }
      else {
        Map<String, String[]> parameterMap = context.getParameters();
        Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
        parameterMap.forEach((key, values) -> {
          if (values.length > 0) {
            result.put(key, values[0]);
          }
        });
        return result;
      }
    }
  }

}
