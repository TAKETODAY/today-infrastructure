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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.OutputStreamSource;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Resolves servlet backed request-related method arguments. Supports values of the
 * following types:
 * <ul>
 * <li>{@link RequestContext}</li>
 * <li>{@link ServletRequest}
 * <li>{@link HttpSession}
 * <li>{@link jakarta.servlet.http.PushBuilder} (as of Spring 5.0 on Servlet 4.0)
 * <li>{@link Principal} but only if not annotated in order to allow custom
 * resolvers to resolve it, and the falling back on
 * {@link PrincipalMethodArgumentResolver}.
 * <li>{@link InputStream}
 * <li>{@link Reader}
 * <li>{@link HttpMethod} (as of Spring 4.0)
 * <li>{@link Locale}
 * <li>{@link TimeZone} (as of Spring 4.0)
 * <li>{@link java.time.ZoneId} (as of Spring 4.0 and Java 8)
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
              "Current request is not of type [" + paramType.getName() + "]: " + request);
    }
    if (InputStream.class.isAssignableFrom(paramType)) {
      InputStream inputStream = request.getInputStream();
      if (inputStream != null && !paramType.isInstance(inputStream)) {
        throw new IllegalStateException(
                "Request input stream is not of type [" + paramType.getName() + "]: " + inputStream);
      }
      return inputStream;
    }
    else if (OutputStream.class.isAssignableFrom(paramType)) {
      OutputStream outputStream = request.getOutputStream();
      if (outputStream != null && !paramType.isInstance(outputStream)) {
        throw new IllegalStateException(
                "Response output stream is not of type [" + paramType.getName() + "]: " + outputStream);
      }
      return outputStream;
    }
    else if (Reader.class.isAssignableFrom(paramType)) {
      Reader reader = request.getReader();
      if (reader != null && !paramType.isInstance(reader)) {
        throw new IllegalStateException(
                "Request body reader is not of type [" + paramType.getName() + "]: " + reader);
      }
      return reader;
    }
    else if (Writer.class.isAssignableFrom(paramType)) {
      PrintWriter writer = request.getWriter();
      if (writer != null && !paramType.isInstance(writer)) {
        throw new IllegalStateException(
                "Request body writer is not of type [" + paramType.getName() + "]: " + writer);
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
