/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MediaType;

/**
 * Default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the
 * {@link ClientHttpResponse}. Any code in the 4xx or 5xx series is considered
 * to be an error. This behavior can be changed by overriding
 * {@link #hasError(HttpStatus)}. Unknown status codes will be ignored by
 * {@link #hasError(ClientHttpResponse)}.
 *
 * <p>See {@link #handleError(ClientHttpResponse)} for more details on specific
 * exception types.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see RestTemplate#setErrorHandler
 * @since 4.0
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

  /**
   * Delegates to {@link #hasError(HttpStatus)} (for a standard status enum value) or
   * {@link #hasError(int)} (for an unknown status code) with the response status code.
   *
   * @see ClientHttpResponse#getRawStatusCode()
   * @see #hasError(HttpStatus)
   * @see #hasError(int)
   */
  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    int rawStatusCode = response.getRawStatusCode();
    HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);
    return (statusCode != null ? hasError(statusCode) : hasError(rawStatusCode));
  }

  /**
   * Template method called from {@link #hasError(ClientHttpResponse)}.
   * <p>The default implementation checks {@link HttpStatus#isError()}.
   * Can be overridden in subclasses.
   *
   * @param statusCode the HTTP status code as enum value
   * @return {@code true} if the response indicates an error; {@code false} otherwise
   * @see HttpStatus#isError()
   */
  protected boolean hasError(HttpStatus statusCode) {
    return statusCode.isError();
  }

  /**
   * Template method called from {@link #hasError(ClientHttpResponse)}.
   * <p>The default implementation checks if the given status code is
   * {@link cn.taketoday.http.HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR} or
   * {@link cn.taketoday.http.HttpStatus.Series#SERVER_ERROR SERVER_ERROR}.
   * Can be overridden in subclasses.
   *
   * @param unknownStatusCode the HTTP status code as raw value
   * @return {@code true} if the response indicates an error; {@code false} otherwise
   * @see cn.taketoday.http.HttpStatus.Series#CLIENT_ERROR
   * @see cn.taketoday.http.HttpStatus.Series#SERVER_ERROR
   */
  protected boolean hasError(int unknownStatusCode) {
    HttpStatus.Series series = HttpStatus.Series.resolve(unknownStatusCode);
    return (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
  }

  /**
   * Handle the error in the given response with the given resolved status code.
   * <p>The default implementation throws:
   * <ul>
   * <li>{@link HttpClientErrorException} if the status code is in the 4xx
   * series, or one of its sub-classes such as
   * {@link HttpClientErrorException.BadRequest} and others.
   * <li>{@link HttpServerErrorException} if the status code is in the 5xx
   * series, or one of its sub-classes such as
   * {@link HttpServerErrorException.InternalServerError} and others.
   * <li>{@link UnknownHttpStatusCodeException} for error status codes not in the
   * {@link HttpStatus} enum range.
   * </ul>
   *
   * @throws UnknownHttpStatusCodeException in case of an unresolvable status code
   * @see #handleError(ClientHttpResponse, HttpStatus)
   */
  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
    if (statusCode == null) {
      byte[] body = getResponseBody(response);
      String message = getErrorMessage(
              response.getRawStatusCode(), response.getStatusText(), body, getCharset(response));
      throw new UnknownHttpStatusCodeException(
              message,
              response.getRawStatusCode(), response.getStatusText(),
              response.getHeaders(), body, getCharset(response));
    }
    handleError(response, statusCode);
  }

  /**
   * Return error message with details from the response body. For example:
   * <pre>
   * 404 Not Found: [{'id': 123, 'message': 'my message'}]
   * </pre>
   */
  private String getErrorMessage(
          int rawStatusCode, String statusText,
          @Nullable byte[] responseBody, @Nullable Charset charset) {

    String preface = rawStatusCode + " " + statusText + ": ";
    if (responseBody == null || responseBody.length == 0) {
      return preface + "[no body]";
    }

    charset = (charset != null ? charset : StandardCharsets.UTF_8);

    String bodyText = new String(responseBody, charset);
    bodyText = LogFormatUtils.formatValue(bodyText, -1, true);
    return preface + bodyText;
  }

  /**
   * Handle the error based on the resolved status code.
   *
   * <p>The default implementation delegates to
   * {@link HttpClientErrorException#create} for errors in the 4xx range, to
   * {@link HttpServerErrorException#create} for errors in the 5xx range,
   * or otherwise raises {@link UnknownHttpStatusCodeException}.
   *
   * @see HttpClientErrorException#create
   * @see HttpServerErrorException#create
   */
  protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
    String statusText = response.getStatusText();
    HttpHeaders headers = response.getHeaders();
    byte[] body = getResponseBody(response);
    Charset charset = getCharset(response);
    String message = getErrorMessage(statusCode.value(), statusText, body, charset);

    switch (statusCode.series()) {
      case CLIENT_ERROR -> throw HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
      case SERVER_ERROR -> throw HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
      default -> throw new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
    }
  }

  /**
   * Read the body of the given response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the response body as a byte array,
   * or an empty byte array if the body could not be read
   */
  protected byte[] getResponseBody(ClientHttpResponse response) {
    try {
      return FileCopyUtils.copyToByteArray(response.getBody());
    }
    catch (IOException ex) {
      // ignore
    }
    return new byte[0];
  }

  /**
   * Determine the charset of the response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the associated charset, or {@code null} if none
   */
  @Nullable
  protected Charset getCharset(ClientHttpResponse response) {
    HttpHeaders headers = response.getHeaders();
    MediaType contentType = headers.getContentType();
    return contentType != null ? contentType.getCharset() : null;
  }

}
