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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.RequestBodyAdvice;

/**
 * A base class for resolving method argument values by reading from the body of
 * a request with {@link HttpMessageConverter HttpMessageConverters}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 19:40
 */
public abstract class AbstractMessageConverterParameterResolver implements ParameterResolvingStrategy {
  private static final Logger log = LoggerFactory.getLogger(AbstractMessageConverterParameterResolver.class);

  private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(
          HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH
  );

  private static final Object NO_VALUE = new Object();

  protected final List<HttpMessageConverter<?>> messageConverters;

  private final RequestResponseBodyAdviceChain advice;

  /**
   * Basic constructor with converters only.
   */
  public AbstractMessageConverterParameterResolver(List<HttpMessageConverter<?>> converters) {
    this(converters, null);
  }

  /**
   * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
   */
  public AbstractMessageConverterParameterResolver(
          List<HttpMessageConverter<?>> converters, @Nullable List<Object> requestResponseBodyAdvice) {

    Assert.notEmpty(converters, "'messageConverters' must not be empty");
    this.messageConverters = converters;
    this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
  }

  /**
   * Return the configured {@link RequestBodyAdvice} and
   * {@link RequestBodyAdvice} where each instance may be wrapped as a
   * {@link cn.taketoday.web.handler.method.ControllerAdviceBean ControllerAdviceBean}.
   */
  RequestResponseBodyAdviceChain getAdvice() {
    return this.advice;
  }

  /**
   * Create the method argument value of the expected parameter type by
   * reading from the given request.
   *
   * @param context the current request context
   * @param parameter the method parameter descriptor (may be {@code null})
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @return the created method argument value
   * @throws IOException if the reading from the request fails
   * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
   */
  @Nullable
  @SuppressWarnings("unchecked")
  protected <T> Object readWithMessageConverters(RequestContext context, MethodParameter parameter, Type targetType)
          throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException //
  {
    MediaType contentType;
    boolean noContentType = false;
    try {
      contentType = context.requestHeaders().getContentType();
    }
    catch (InvalidMediaTypeException ex) {
      throw new HttpMediaTypeNotSupportedException(ex.getMessage());
    }
    if (contentType == null) {
      noContentType = true;
      contentType = MediaType.APPLICATION_OCTET_STREAM;
    }

    Class<?> contextClass = parameter.getContainingClass();
    Class<T> targetClass = targetType instanceof Class ? (Class<T>) targetType : null;
    if (targetClass == null) {
      ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
      targetClass = (Class<T>) resolvableType.resolve();
    }

    Object body = NO_VALUE;

    EmptyBodyCheckingHttpInputMessage message = null;
    try {
      message = new EmptyBodyCheckingHttpInputMessage(context);

      RequestResponseBodyAdviceChain adviceChain = getAdvice();
      for (HttpMessageConverter<?> converter : this.messageConverters) {
        Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
        GenericHttpMessageConverter<?> genericConverter = converter instanceof GenericHttpMessageConverter
                                                          ? (GenericHttpMessageConverter<?>) converter : null;
        if (genericConverter != null ? genericConverter.canRead(targetType, contextClass, contentType)
                                     : targetClass != null && converter.canRead(targetClass, contentType)) {

          if (message.hasBody()) {
            HttpInputMessage msgToUse =
                    adviceChain.beforeBodyRead(message, parameter, targetType, converterType);
            body = genericConverter != null
                   ? genericConverter.read(targetType, contextClass, msgToUse)
                   : ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse);
            body = adviceChain.afterBodyRead(body, msgToUse, parameter, targetType, converterType);
          }
          else {
            body = adviceChain.handleEmptyBody(null, message, parameter, targetType, converterType);
          }
          break;
        }
      }
    }
    catch (IOException ex) {
      throw new HttpMessageNotReadableException("I/O error while reading input message", ex, context);
    }
    finally {
      if (message != null && message.hasBody()) {
        closeStreamIfNecessary(message.getBody());
      }
    }

    if (body == NO_VALUE) {
      HttpMethod httpMethod = context.getMethod();
      if (httpMethod == null
              || !SUPPORTED_METHODS.contains(httpMethod)
              || (noContentType && !message.hasBody())) {
        return null;
      }
      throw new HttpMediaTypeNotSupportedException(contentType,
              getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
    }

    MediaType selectedContentType = contentType;
    Object theBody = body;

    LogFormatUtils.traceDebug(log, traceOn -> {
      String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
      return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
    });

    return body;
  }

  /**
   * Return the media types supported by all provided message converters sorted
   * by specificity via {@link MimeTypeUtils#sortBySpecificity(List)}.
   */
  protected List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
    LinkedHashSet<MediaType> mediaTypeSet = new LinkedHashSet<>();
    for (HttpMessageConverter<?> converter : this.messageConverters) {
      mediaTypeSet.addAll(converter.getSupportedMediaTypes(clazz));
    }
    ArrayList<MediaType> result = new ArrayList<>(mediaTypeSet);
    MimeTypeUtils.sortBySpecificity(result);
    return result;
  }

  /**
   * Adapt the given argument against the method parameter, if necessary.
   *
   * @param arg the resolved argument
   * @param parameter the method parameter descriptor
   * @return the adapted argument, or the original resolved argument as-is
   */
  @Nullable
  protected Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
    if (parameter.getParameterType() == Optional.class) {
      if (arg == null || (arg instanceof Collection && ((Collection<?>) arg).isEmpty())
              || (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
        return Optional.empty();
      }
      else {
        return Optional.of(arg);
      }
    }
    return arg;
  }

  /**
   * Allow for closing the body stream if necessary,
   * e.g. for part streams in a multipart request.
   */
  void closeStreamIfNecessary(InputStream body) {
    // No-op by default: A standard HttpInputMessage exposes the HTTP request stream
    // (ServletRequest#getInputStream), with its lifecycle managed by the container.
  }

  private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

    private final HttpHeaders headers;

    @Nullable
    private final InputStream body;

    public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
      this.headers = inputMessage.getHeaders();
      InputStream inputStream = inputMessage.getBody();
      if (inputStream.markSupported()) {
        inputStream.mark(1);
        this.body = inputStream.read() != -1 ? inputStream : null;
        inputStream.reset();
      }
      else {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
        int b = pushbackInputStream.read();
        if (b == -1) {
          this.body = null;
        }
        else {
          this.body = pushbackInputStream;
          pushbackInputStream.unread(b);
        }
      }
    }

    @Override
    public HttpHeaders getHeaders() {
      return this.headers;
    }

    @Override
    public InputStream getBody() {
      return this.body != null ? this.body : StreamUtils.emptyInput();
    }

    public boolean hasBody() {
      return this.body != null;
    }
  }

}
