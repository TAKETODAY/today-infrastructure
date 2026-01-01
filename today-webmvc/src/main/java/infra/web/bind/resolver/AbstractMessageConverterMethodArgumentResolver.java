/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import infra.core.Conventions;
import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.HttpOutputMessage;
import infra.http.HttpRequest;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.SmartHttpMessageConverter;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.LogFormatUtils;
import infra.util.MimeTypeUtils;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.validation.annotation.Validated;
import infra.validation.annotation.ValidationAnnotationUtils;
import infra.web.BindingContext;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.RequestContext;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.WebDataBinder;
import infra.web.handler.method.ControllerAdviceBean;
import infra.web.handler.method.RequestBodyAdvice;

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

  private static final EnumSet<HttpMethod> SUPPORTED_METHODS = EnumSet.of(
          HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH
  );

  private static final Object NO_VALUE = new Object();

  protected final List<HttpMessageConverter<?>> messageConverters;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The configured {@link RequestBodyAdvice} and
   * {@link RequestBodyAdvice} where each instance may be wrapped as a
   * {@link ControllerAdviceBean ControllerAdviceBean}.
   */
  protected final RequestResponseBodyAdviceChain advice;

  /**
   * Basic constructor with converters only.
   */
  public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
    this(converters, null);
  }

  /**
   * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
   */
  public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters, @Nullable List<Object> requestResponseBodyAdvice) {
    Assert.notEmpty(converters, "'messageConverters' must not be empty");
    this.messageConverters = converters;
    this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
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
    Class<T> targetClass = targetType instanceof Class ? (Class<T>) targetType : null;
    ResolvableType resolvableType = null;
    if (targetClass == null) {
      resolvableType = ResolvableType.forMethodParameter(parameter);
      targetClass = (Class<T>) resolvableType.resolve();
    }

    MediaType contentType;
    boolean noContentType = false;
    try {
      contentType = inputMessage.getContentType();
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
      ResolvableType targetResolvableType = null;
      message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
      for (HttpMessageConverter converter : messageConverters) {
        if (converter instanceof GenericHttpMessageConverter generic) {
          if (generic.canRead(targetType, contextClass, contentType)) {
            if (message.hasBody()) {
              // beforeBodyRead
              var msgToUse = advice.beforeBodyRead(message, parameter, targetType, converter);
              // read
              body = generic.read(targetType, contextClass, msgToUse);
              // afterBodyRead
              body = advice.afterBodyRead(body, msgToUse, parameter, targetType, converter);
            }
            else {
              body = advice.handleEmptyBody(null, message, parameter, targetType, converter);
            }
            break;
          }
        }
        else if (converter instanceof SmartHttpMessageConverter<?> smart) {
          if (targetResolvableType == null) {
            targetResolvableType = getNestedTypeIfNeeded(resolvableType == null ? ResolvableType.forMethodParameter(parameter) : resolvableType);
          }
          if (smart.canRead(targetResolvableType, contentType)) {
            if (message.hasBody()) {
              var advice = this.advice;
              // beforeBodyRead
              var msgToUse = advice.beforeBodyRead(message, parameter, targetType, converter);
              // read
              body = smart.read(targetResolvableType, msgToUse, advice.determineReadHints(parameter, targetType, smart));
              // afterBodyRead
              body = advice.afterBodyRead(body, msgToUse, parameter, targetType, converter);
            }
            else {
              body = advice.handleEmptyBody(null, message, parameter, targetType, converter);
            }
            break;
          }
        }
        else if (targetClass != null && converter.canRead(targetClass, contentType)) {
          if (message.hasBody()) {
            // beforeBodyRead
            var msgToUse = advice.beforeBodyRead(message, parameter, targetType, converter);
            // read
            body = converter.read(targetClass, msgToUse);
            // afterBodyRead
            body = advice.afterBodyRead(body, msgToUse, parameter, targetType, converter);
          }
          else {
            body = advice.handleEmptyBody(null, message, parameter, targetType, converter);
          }
          break;
        }
      }
      if (body == NO_VALUE && noContentType && !message.hasBody()) {
        body = advice.handleEmptyBody(
                null, message, parameter, targetType, new NoContentTypeHttpMessageConverter());
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

    if (logger.isDebugEnabled()) {
      Object theBody = body;
      MediaType selectedContentType = contentType;
      LogFormatUtils.traceDebug(logger, traceOn -> "Read \"%s\" to [%s]"
              .formatted(selectedContentType, LogFormatUtils.formatValue(theBody, !traceOn)));
    }

    return body;
  }

  /**
   * Return the generic type of the {@code returnType} (or of the nested type
   * if it is an {@link HttpEntity} or/and an {@link Optional}).
   */
  protected ResolvableType getNestedTypeIfNeeded(ResolvableType type) {
    ResolvableType genericType = type;
    if (Optional.class.isAssignableFrom(genericType.toClass())) {
      genericType = genericType.getNested(2);
    }
    if (HttpEntity.class.isAssignableFrom(genericType.toClass())) {
      genericType = genericType.getNested(2);
    }
    return genericType;
  }

  protected void validateIfApplicable(RequestContext context, MethodParameter parameter, @Nullable Object arg) throws Throwable {
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
   * Infra {@link Validated},
   * and custom annotations whose name starts with "Valid".
   *
   * @param binder the DataBinder to be used
   * @param parameter the method parameter descriptor
   * @see #isBindExceptionRequired
   */
  protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
    for (Annotation ann : parameter.getParameterAnnotations()) {
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
   * Allow for closing the body stream if necessary,
   * e.g. for part streams in a multipart request.
   */
  void closeStreamIfNecessary(InputStream body) {
    // No-op by default: A standard HttpInputMessage exposes the HTTP request stream
    // (Request#getInputStream), with its lifecycle managed by the container.
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

  /**
   * Placeholder HttpMessageConverter type to pass to RequestBodyAdvice if there
   * is no content-type and no content. In that case, we may not find a converter,
   * but RequestBodyAdvice have a chance to provide it via handleEmptyBody.
   */
  private static final class NoContentTypeHttpMessageConverter implements HttpMessageConverter<String> {

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
      return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
      return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
      return Collections.emptyList();
    }

    @Override
    public String read(Class<? extends String> clazz, HttpInputMessage inputMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void write(String s, @Nullable MediaType contentType, HttpOutputMessage outputMessage) {
      throw new UnsupportedOperationException();
    }
  }

}
