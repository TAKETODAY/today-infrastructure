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

package cn.taketoday.web.client;

import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Common base class for exceptions that contain actual HTTP response data.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RestClientResponseException extends RestClientException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final HttpStatusCode statusCode;

  private final String statusText;

  private final byte[] responseBody;

  @Nullable
  private final HttpHeaders responseHeaders;

  @Nullable
  private transient final Charset responseCharset;

  @Nullable
  private transient Function<ResolvableType, ?> bodyConvertFunction;

  /**
   * Construct a new instance of with the given response data.
   *
   * @param statusCode the raw status code value
   * @param statusText the status text
   * @param headers the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public RestClientResponseException(@Nullable String message, int statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    this(message, HttpStatusCode.valueOf(statusCode), statusText, headers, responseBody, responseCharset);
  }

  /**
   * Construct a new instance of with the given response data.
   *
   * @param statusCode the raw status code value
   * @param statusText the status text
   * @param headers the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public RestClientResponseException(@Nullable String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    super(message);
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.responseHeaders = headers;
    this.responseBody = (responseBody != null ? responseBody : Constant.EMPTY_BYTES);
    this.responseCharset = responseCharset;
  }

  /**
   * Return the HTTP status code.
   */
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return the raw HTTP status code value.
   */
  public int getRawStatusCode() {
    return this.statusCode.value();
  }

  /**
   * Return the HTTP status text.
   */
  public String getStatusText() {
    return this.statusText;
  }

  /**
   * Return the HTTP response headers.
   */
  @Nullable
  public HttpHeaders getResponseHeaders() {
    return this.responseHeaders;
  }

  /**
   * Return the response body as a byte array.
   */
  public byte[] getResponseBodyAsByteArray() {
    return this.responseBody;
  }

  /**
   * Return the response body converted to String. The charset used is that
   * of the response "Content-Type" or otherwise {@code "UTF-8"}.
   */
  public String getResponseBodyAsString() {
    return getResponseBodyAsString(DEFAULT_CHARSET);
  }

  /**
   * Return the response body converted to String. The charset used is that
   * of the response "Content-Type" or otherwise the one given.
   *
   * @param fallbackCharset the charset to use on if the response doesn't specify.
   */
  public String getResponseBodyAsString(Charset fallbackCharset) {
    Charset charsetToUse = responseCharset;
    if (charsetToUse == null) {
      charsetToUse = fallbackCharset;
    }
    return new String(responseBody, charsetToUse);
  }

  /**
   * Convert the error response content to the specified type.
   *
   * @param targetType the type to convert to
   * @param <E> the expected target type
   * @return the converted object, or {@code null} if there is no content
   */
  @Nullable
  public <E> E getResponseBodyAs(Class<E> targetType) {
    return getResponseBodyAs(ResolvableType.forClass(targetType));
  }

  /**
   * Variant of {@link #getResponseBodyAs(Class)} with
   * {@link ParameterizedTypeReference}.
   */
  @Nullable
  public <E> E getResponseBodyAs(ParameterizedTypeReference<E> targetType) {
    return getResponseBodyAs(ResolvableType.forType(targetType.getType()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private <E> E getResponseBodyAs(ResolvableType targetType) {
    Assert.state(this.bodyConvertFunction != null, "Function to convert body not set");
    return (E) this.bodyConvertFunction.apply(targetType);
  }

  /**
   * Provide a function to use to decode the response error content
   * via {@link #getResponseBodyAs(Class)}.
   *
   * @param bodyConvertFunction the function to use
   */
  public void setBodyConvertFunction(Function<ResolvableType, ?> bodyConvertFunction) {
    this.bodyConvertFunction = bodyConvertFunction;
  }

}
