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

package cn.taketoday.web;

import java.net.URI;
import java.util.Locale;
import java.util.function.Consumer;

import cn.taketoday.context.MessageSource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;

/**
 * Representation of a complete RFC 9457 error response including status,
 * headers, and an RFC 9457 formatted {@link ProblemDetail} body. Allows any
 * exception to expose HTTP error response information.
 *
 * <p>{@link ErrorResponseException} is a default implementation of this
 * interface and a convenient base class for other exceptions to use.
 *
 * <p>{@code ErrorResponse} is supported as a return value from
 * {@code @ExceptionHandler} methods that render directly to the response, e.g.
 * by being marked {@code @ResponseBody}, or declared in an
 * {@code @RestController} or {@code RestControllerAdvice} class.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorResponseException
 * @see ResponseEntity#of(ProblemDetail)
 * @since 4.0 2022/3/2 13:34
 */
public interface ErrorResponse extends HttpStatusProvider {

  /**
   * Return the HTTP status code to use for the response.
   */
  @Override
  HttpStatusCode getStatusCode();

  /**
   * Return headers to use for the response.
   */
  default HttpHeaders getHeaders() {
    return HttpHeaders.empty();
  }

  /**
   * Return the body for the response, formatted as an RFC 9457
   * {@link ProblemDetail} whose {@link ProblemDetail#getStatus() status}
   * should match the response status.
   * <p><strong>Note:</strong> The returned {@code ProblemDetail} may be
   * updated before the response is rendered, e.g. via
   * {@link #updateAndGetBody(MessageSource, Locale)}. Therefore, implementing
   * methods should use an instance field, and should not re-create the
   * {@code ProblemDetail} on every call, nor use a static variable.
   */
  ProblemDetail getBody();

  // MessageSource codes and arguments

  /**
   * Return a code to use to resolve the problem "type" for this exception
   * through a {@link MessageSource}. The type resolved through the
   * {@code MessageSource} will be passed into {@link URI#create(String)}
   * and therefore must be an encoded URI String.
   * <p>By default this is initialized via {@link #getDefaultTypeMessageCode(Class)}.
   *
   * @since 5.0
   */
  default String getTypeMessageCode() {
    return getDefaultTypeMessageCode(getClass());
  }

  /**
   * Return a code to use to resolve the problem "title" for this exception
   * through a {@link MessageSource}.
   * <p>By default this is initialized via {@link #getDefaultTitleMessageCode(Class)}.
   *
   * @since 5.0
   */
  default String getTitleMessageCode() {
    return getDefaultTitleMessageCode(getClass());
  }

  /**
   * Return a code to use to resolve the problem "detail" for this exception
   * through a {@link MessageSource}.
   * <p>By default this is initialized via
   * {@link #getDefaultDetailMessageCode(Class, String)}.
   *
   * @since 5.0
   */
  default String getDetailMessageCode() {
    return getDefaultDetailMessageCode(getClass(), null);
  }

  /**
   * Return arguments to use along with a {@link #getDetailMessageCode()
   * message code} to resolve the problem "detail" for this exception
   * through a {@link MessageSource}. The arguments are expanded
   * into placeholders of the message value, e.g. "Invalid content type {0}".
   *
   * @since 5.0
   */
  @Nullable
  default Object[] getDetailMessageArguments() {
    return null;
  }

  /**
   * Variant of {@link #getDetailMessageArguments()} that uses the given
   * {@link MessageSource} for resolving the message argument values.
   * <p>This is useful for example to expand message codes from validation errors.
   * <p>The default implementation delegates to {@link #getDetailMessageArguments()},
   * ignoring the supplied {@code MessageSource} and {@code Locale}.
   *
   * @param messageSource the {@code MessageSource} to use for the lookup
   * @param locale the {@code Locale} to use for the lookup
   * @since 5.0
   */
  @Nullable
  default Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
    return getDetailMessageArguments();
  }

  /**
   * Use the given {@link MessageSource} to resolve the
   * {@link #getTypeMessageCode() type}, {@link #getTitleMessageCode() title},
   * and {@link #getDetailMessageCode() detail} message codes, and then use the
   * resolved values to update the corresponding fields in {@link #getBody()}.
   *
   * @param messageSource the {@code MessageSource} to use for the lookup
   * @param locale the {@code Locale} to use for the lookup
   * @since 5.0
   */
  default ProblemDetail updateAndGetBody(@Nullable MessageSource messageSource, Locale locale) {
    ProblemDetail body = getBody();
    if (messageSource != null) {
      String type = messageSource.getMessage(getTypeMessageCode(), null, null, locale);
      if (type != null) {
        body.setType(URI.create(type));
      }
      Object[] arguments = getDetailMessageArguments(messageSource, locale);
      String detail = messageSource.getMessage(getDetailMessageCode(), arguments, null, locale);
      if (detail != null) {
        body.setDetail(detail);
      }
      String title = messageSource.getMessage(getTitleMessageCode(), null, null, locale);
      if (title != null) {
        body.setTitle(title);
      }
    }
    return body;
  }

  /**
   * Build a message code for the "type" field, for the given exception type.
   *
   * @param exceptionType the exception type associated with the problem
   * @return {@code "problemDetail.type."} followed by the fully qualified
   * {@link Class#getName() class name}
   * @since 5.0
   */
  static String getDefaultTypeMessageCode(Class<?> exceptionType) {
    return "problemDetail.type." + exceptionType.getName();
  }

  /**
   * Build a message code for the "title" field, for the given exception type.
   *
   * @param exceptionType the exception type associated with the problem
   * @return {@code "problemDetail.title."} followed by the fully qualified
   * {@link Class#getName() class name}
   * @since 5.0
   */
  static String getDefaultTitleMessageCode(Class<?> exceptionType) {
    return "problemDetail.title." + exceptionType.getName();
  }

  /**
   * Build a message code for the "detail" field, for the given exception type.
   *
   * @param exceptionType the exception type associated with the problem
   * @param suffix an optional suffix, e.g. for exceptions that may have multiple
   * error message with different arguments
   * @return {@code "problemDetail."} followed by the fully qualified
   * {@link Class#getName() class name} and an optional suffix
   * @since 5.0
   */
  static String getDefaultDetailMessageCode(Class<?> exceptionType, @Nullable String suffix) {
    return "problemDetail." + exceptionType.getName() + (suffix != null ? "." + suffix : "");
  }

  /**
   * Static factory method to build an instance via
   * {@link #builder(Throwable, HttpStatusCode, String)}.
   *
   * @since 5.0
   */
  static ErrorResponse create(Throwable ex, HttpStatusCode statusCode, String detail) {
    return builder(ex, statusCode, detail).build();
  }

  /**
   * Return a builder to create an {@code ErrorResponse} instance.
   *
   * @param ex the underlying exception that lead to the error response;
   * mainly to derive default values for the
   * {@linkplain #getDetailMessageCode() detail message code} and for the
   * {@linkplain #getTitleMessageCode() title message code}.
   * @param statusCode the status code to set in the response
   * @param detail the default value for the
   * {@link ProblemDetail#setDetail(String) detail} field, unless overridden
   * by a {@link MessageSource} lookup with {@link #getDetailMessageCode()}
   * @since 5.0
   */
  static Builder builder(Throwable ex, HttpStatusCode statusCode, String detail) {
    return builder(ex, ProblemDetail.forStatusAndDetail(statusCode, detail));
  }

  /**
   * Variant of {@link #builder(Throwable, HttpStatusCode, String)} for use
   * with a custom {@link ProblemDetail} instance.
   *
   * @since 5.0
   */
  static Builder builder(Throwable ex, ProblemDetail problemDetail) {
    return new DefaultErrorResponseBuilder(ex, problemDetail);
  }

  /**
   * Builder for an {@code ErrorResponse}.
   *
   * @since 5.0
   */
  interface Builder {

    /**
     * Add the given header value(s) under the given name.
     *
     * @param headerName the header name
     * @param headerValues the header value(s)
     * @return the same builder instance
     * @see HttpHeaders#add(String, String)
     */
    Builder header(String headerName, String... headerValues);

    /**
     * Manipulate this response's headers with the given consumer. This is
     * useful to {@linkplain HttpHeaders#set(String, String) overwrite} or
     * {@linkplain HttpHeaders#remove(Object) remove} existing values, or
     * use any other {@link HttpHeaders} methods.
     *
     * @param headersConsumer a function that consumes the {@code HttpHeaders}
     * @return the same builder instance
     */
    Builder headers(Consumer<HttpHeaders> headersConsumer);

    /**
     * Set the underlying {@link ProblemDetail#setType(URI) type} field.
     *
     * @return the same builder instance
     */
    Builder type(URI type);

    /**
     * Customize the {@link MessageSource} code to use to resolve the value
     * for {@link ProblemDetail#setType(URI)}.
     * <p>By default, set from {@link ErrorResponse#getDefaultTypeMessageCode(Class)}.
     *
     * @param messageCode the message code to use
     * @return the same builder instance
     * @see ErrorResponse#getTypeMessageCode()
     */
    Builder typeMessageCode(String messageCode);

    /**
     * Set the underlying {@link ProblemDetail#setTitle(String) title} field.
     *
     * @return the same builder instance
     */
    Builder title(@Nullable String title);

    /**
     * Customize the {@link MessageSource} code to use to resolve the value
     * for {@link ProblemDetail#setTitle(String)}.
     * <p>By default, set from {@link ErrorResponse#getDefaultTitleMessageCode(Class)}.
     *
     * @param messageCode the message code to use
     * @return the same builder instance
     * @see ErrorResponse#getTitleMessageCode()
     */
    Builder titleMessageCode(String messageCode);

    /**
     * Set the underlying {@link ProblemDetail#setInstance(URI) instance} field.
     *
     * @return the same builder instance
     */
    Builder instance(@Nullable URI instance);

    /**
     * Set the underlying {@link ProblemDetail#setDetail(String) detail}.
     *
     * @return the same builder instance
     */
    Builder detail(String detail);

    /**
     * Customize the {@link MessageSource} code to use to resolve the value
     * for the {@link #detail(String)}.
     * <p>By default, set from {@link ErrorResponse#getDefaultDetailMessageCode(Class, String)}.
     *
     * @param messageCode the message code to use
     * @return the same builder instance
     * @see ErrorResponse#getDetailMessageCode()
     */
    Builder detailMessageCode(String messageCode);

    /**
     * Set the arguments to provide to the {@link MessageSource} lookup for
     * {@link #detailMessageCode(String)}.
     *
     * @param messageArguments the arguments to provide
     * @return the same builder instance
     * @see ErrorResponse#getDetailMessageArguments()
     */
    Builder detailMessageArguments(Object... messageArguments);

    /**
     * Set a "dynamic" {@link ProblemDetail#setProperty(String, Object)
     * property} on the underlying {@code ProblemDetail}.
     *
     * @return the same builder instance
     */
    Builder property(String name, @Nullable Object value);

    /**
     * Build the {@code ErrorResponse} instance.
     */
    ErrorResponse build();

    /**
     * Build the {@code ErrorResponse} instance and also resolve the "detail"
     * and "title" through the given {@link MessageSource}. Effectively a
     * shortcut for calling {@link #build()} and then
     * {@link ErrorResponse#updateAndGetBody(MessageSource, Locale)}.
     */
    default ErrorResponse build(@Nullable MessageSource messageSource, Locale locale) {
      ErrorResponse response = build();
      response.updateAndGetBody(messageSource, locale);
      return response;
    }

  }

  /**
   * Callback to perform an action before an RFC-7807 {@link ProblemDetail}
   * response is rendered.
   *
   * @author Rossen Stoyanchev
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 5.0
   */
  interface Interceptor {

    /**
     * Handle the given {@code ProblemDetail} that's going to be rendered,
     * and the {@code ErrorResponse} it originates from, if applicable.
     *
     * @param detail the {@code ProblemDetail} to be rendered
     * @param errorResponse the {@code ErrorResponse}, or {@code null} if there isn't one
     */
    void handleError(ProblemDetail detail, @Nullable ErrorResponse errorResponse);

  }

}
