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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import cn.taketoday.core.Conventions;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
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
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.annotation.ValidationAnnotationUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.WebDataBinder;
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
public abstract class AbstractMessageConverterMethodArgumentResolver implements ParameterResolvingStrategy {
  private static final Logger log = LoggerFactory.getLogger(AbstractMessageConverterMethodArgumentResolver.class);

  private static final EnumSet<HttpMethod> SUPPORTED_METHODS = EnumSet.of(
          HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH
  );

  private static final Object NO_VALUE = new Object();

  protected final List<HttpMessageConverter<?>> messageConverters;

  private final RequestResponseBodyAdviceChain advice;

  /**
   * Basic constructor with converters only.
   */
  public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
    this(converters, null);
  }

  /**
   * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
   */
  public AbstractMessageConverterMethodArgumentResolver(
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
   * @param request the current request
   * @param parameter the method parameter descriptor (may be {@code null})
   * @param paramType the type of the argument value to be created
   * @return the created method argument value
   * @throws IOException if the reading from the request fails
   * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
   */
  @Nullable
  protected Object readWithMessageConverters(RequestContext request, MethodParameter parameter, Type paramType)
          throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
    return readWithMessageConverters((HttpInputMessage) request, parameter, paramType);
  }

  /**
   * Create the method argument value of the expected parameter type by reading
   * from the given HttpInputMessage.
   *
   * @param inputMessage the HTTP input message representing the current request
   * @param parameter the method parameter descriptor
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @return the created method argument value
   * @throws IOException if the reading from the request fails
   * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
   */
  @Nullable
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType)
          throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException //
  {

    Class<?> contextClass = parameter.getContainingClass();
    Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
    if (targetClass == null) {
      ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
      targetClass = (Class<T>) resolvableType.resolve();
    }

    MediaType contentType;
    boolean noContentType = false;
    try {
      contentType = inputMessage.getHeaders().getContentType();
    }
    catch (InvalidMediaTypeException ex) {
      throw new HttpMediaTypeNotSupportedException(
              ex.getMessage(), getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
    }
    if (contentType == null) {
      noContentType = true;
      contentType = MediaType.APPLICATION_OCTET_STREAM;
    }

    Object body = NO_VALUE;

    EmptyBodyCheckingHttpInputMessage message = null;
    try {
      message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
      RequestResponseBodyAdviceChain adviceChain = getAdvice();
      for (HttpMessageConverter converter : messageConverters) {
        if (converter instanceof GenericHttpMessageConverter genericConverter) {
          if (genericConverter.canRead(targetType, contextClass, contentType)) {
            if (message.hasBody()) {
              // beforeBodyRead
              var msgToUse = adviceChain.beforeBodyRead(message, parameter, targetType, converter);
              // read
              body = genericConverter.read(targetType, contextClass, msgToUse);
              // afterBodyRead
              body = adviceChain.afterBodyRead(body, msgToUse, parameter, targetType, converter);
            }
            else {
              body = adviceChain.handleEmptyBody(null, message, parameter, targetType, converter);
            }
            break;
          }
        }
        else if (targetClass != null && converter.canRead(targetClass, contentType)) {
          if (message.hasBody()) {
            // beforeBodyRead
            var msgToUse = adviceChain.beforeBodyRead(message, parameter, targetType, converter);
            // read
            body = converter.read(targetClass, msgToUse);
            // afterBodyRead
            body = adviceChain.afterBodyRead(body, msgToUse, parameter, targetType, converter);
          }
          else {
            body = adviceChain.handleEmptyBody(null, message, parameter, targetType, converter);
          }
          break;
        }
      }
    }
    catch (IOException ex) {
      throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
    }
    finally {
      if (message != null && message.hasBody()) {
        closeStreamIfNecessary(message.getBody());
      }
    }

    if (body == NO_VALUE) {
      HttpMethod httpMethod = inputMessage instanceof HttpRequest httpRequest ? httpRequest.getMethod() : null;
      if (!SUPPORTED_METHODS.contains(httpMethod) || noContentType && !message.hasBody()) {
        return null;
      }
      throw new HttpMediaTypeNotSupportedException(contentType,
              getSupportedMediaTypes(targetClass != null ? targetClass : Object.class), httpMethod);
    }

    if (log.isDebugEnabled()) {
      Object theBody = body;
      MediaType selectedContentType = contentType;
      LogFormatUtils.traceDebug(log, traceOn -> {
        String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
        return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
      });
    }

    return body;
  }

  protected void validateIfApplicable(RequestContext context, MethodParameter parameter, Object arg) throws Throwable {
    BindingContext bindingContext = context.getBinding();
    if (bindingContext != null) {
      String name = Conventions.getVariableNameForParameter(parameter);
      ResolvableType type = ResolvableType.forMethodParameter(parameter);

      WebDataBinder binder = bindingContext.createBinder(context, arg, name, type);
      if (arg != null) {
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
          throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
        }
      }

      bindingContext.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
    }
  }

  /**
   * Validate the binding target if applicable.
   * <p>The default implementation checks for {@code @jakarta.validation.Valid},
   * Infra {@link cn.taketoday.validation.annotation.Validated},
   * and custom annotations whose name starts with "Valid".
   *
   * @param binder the DataBinder to be used
   * @param parameter the method parameter descriptor
   * @see #isBindExceptionRequired
   */
  protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
    Annotation[] annotations = parameter.getParameterAnnotations();
    for (Annotation ann : annotations) {
      Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
      if (validationHints != null) {
        binder.validate(validationHints);
        break;
      }
    }
  }

  /**
   * Whether to raise a fatal bind exception on validation errors.
   *
   * @param binder the data binder used to perform data binding
   * @param parameter the method parameter descriptor
   * @return {@code true} if the next method argument is not of type {@link Errors}
   */
  protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
    int i = parameter.getParameterIndex();
    Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
    boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
    return !hasBindingResult;
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
      return this.body != null ? this.body : InputStream.nullInputStream();
    }

    public boolean hasBody() {
      return this.body != null;
    }
  }

}
