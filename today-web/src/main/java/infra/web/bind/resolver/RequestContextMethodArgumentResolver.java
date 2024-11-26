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

package infra.web.bind.resolver;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import infra.core.io.InputStreamSource;
import infra.core.io.OutputStreamSource;
import infra.http.HttpMethod;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.multipart.MultipartRequest;

/**
 * Resolves servlet backed request-related method arguments. Supports values of the
 * following types:
 * <ul>
 * <li>{@link RequestContext}
 * <li>{@link MultipartRequest}
 * <li>{@link InputStream}
 * <li>{@link OutputStream}
 * <li>{@link Reader}
 * <li>{@link Writer}
 * <li>{@link HttpMethod}
 * <li>{@link Locale}
 * <li>{@link TimeZone}
 * <li>{@link InputStreamSource}
 * <li>{@link OutputStreamSource}
 * <li>{@link java.time.ZoneId}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 10:35
 */
public class RequestContextMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    Class<?> paramType = resolvable.getParameterType();
    return RequestContext.class.isAssignableFrom(paramType)
            || MultipartRequest.class.isAssignableFrom(paramType)
            || InputStream.class.isAssignableFrom(paramType)
            || OutputStream.class.isAssignableFrom(paramType)
            || Reader.class.isAssignableFrom(paramType)
            || Writer.class.isAssignableFrom(paramType)
            || HttpMethod.class == paramType
            || Locale.class == paramType
            || TimeZone.class == paramType
            || InputStreamSource.class == paramType
            || OutputStreamSource.class == paramType
            || ZoneId.class == paramType;
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext request, ResolvableMethodParameter resolvable) throws Throwable {
    Class<?> paramType = resolvable.getParameterType();
    // RequestContext
    if (RequestContext.class.isAssignableFrom(paramType)) {
      if (paramType.isInstance(request)) {
        return request;
      }
      throw new IllegalStateException(
              "Current request is not of type [%s]: %s".formatted(paramType.getName(), request));
    }
    if (MultipartRequest.class.isAssignableFrom(paramType)) {
      MultipartRequest multipartRequest = request.getMultipartRequest();
      if (paramType.isInstance(multipartRequest)) {
        return multipartRequest;
      }
      throw new IllegalStateException(
              "Current multipart request is not of type [%s]: %s".formatted(paramType.getName(), multipartRequest));
    }
    if (InputStream.class.isAssignableFrom(paramType)) {
      InputStream inputStream = request.getInputStream();
      if (inputStream != null && !paramType.isInstance(inputStream)) {
        throw new IllegalStateException(
                "Request input stream is not of type [%s]: %s".formatted(paramType.getName(), inputStream));
      }
      return inputStream;
    }
    else if (OutputStream.class.isAssignableFrom(paramType)) {
      OutputStream outputStream = request.getOutputStream();
      if (outputStream != null && !paramType.isInstance(outputStream)) {
        throw new IllegalStateException(
                "Response output stream is not of type [%s]: %s".formatted(paramType.getName(), outputStream));
      }
      return outputStream;
    }
    else if (Reader.class.isAssignableFrom(paramType)) {
      Reader reader = request.getReader();
      if (reader != null && !paramType.isInstance(reader)) {
        throw new IllegalStateException(
                "Request body reader is not of type [%s]: %s".formatted(paramType.getName(), reader));
      }
      return reader;
    }
    else if (Writer.class.isAssignableFrom(paramType)) {
      PrintWriter writer = request.getWriter();
      if (writer != null && !paramType.isInstance(writer)) {
        throw new IllegalStateException(
                "Request body writer is not of type [%s]: %s".formatted(paramType.getName(), writer));
      }
      return writer;
    }
    else if (HttpMethod.class == paramType) {
      return request.getMethod();
    }
    else if (Locale.class == paramType) {
      return RequestContextUtils.getLocale(request);
    }
    else if (TimeZone.class == paramType) {
      TimeZone timeZone = RequestContextUtils.getTimeZone(request);
      return timeZone != null ? timeZone : TimeZone.getDefault();
    }
    else if (ZoneId.class == paramType) {
      TimeZone timeZone = RequestContextUtils.getTimeZone(request);
      return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
    }
    else if (InputStreamSource.class == paramType
            || OutputStreamSource.class == paramType) {
      return request;
    }

    // Should never happen...
    throw new UnsupportedOperationException("Unknown parameter type: " + paramType.getName());
  }

}
