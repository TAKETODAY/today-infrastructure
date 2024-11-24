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

package infra.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpResponse;
import infra.http.client.ClientHttpResponseDecorator;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.LogFormatUtils;
import infra.util.ObjectUtils;

/**
 * Default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the
 * {@link ClientHttpResponse}. Any code in the 4xx or 5xx series is considered
 * to be an error. This behavior can be changed by overriding
 * {@link #hasError(HttpStatusCode)}. Unknown status codes will be ignored by
 * {@link #hasError(ClientHttpResponse)}.
 *
 * <p>See {@link #handleError(HttpRequest, ClientHttpResponse)} for more details on specific
 * exception types.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplate#setErrorHandler
 * @since 4.0
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  /**
   * For internal use from the RestTemplate, to pass the message converters
   * to use to decode error content.
   */
  void setMessageConverters(List<HttpMessageConverter<?>> converters) {
    this.messageConverters = Collections.unmodifiableList(converters);
  }

  /**
   * Delegates to {@link #hasError(HttpStatusCode)} with the response status code.
   *
   * @see ClientHttpResponse#getStatusCode()
   * @see #hasError(HttpStatusCode)
   */
  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    HttpStatusCode statusCode = response.getStatusCode();
    return hasError(statusCode);
  }

  /**
   * Template method called from {@link #hasError(ClientHttpResponse)}.
   * <p>The default implementation checks {@link HttpStatusCode#isError()}.
   * Can be overridden in subclasses.
   *
   * @param statusCode the HTTP status code
   * @return {@code true} if the response indicates an error; {@code false} otherwise
   * @see HttpStatusCode#isError()
   */
  protected boolean hasError(HttpStatusCode statusCode) {
    return statusCode.isError();
  }

  /**
   * Handle the error in the given response with the given resolved status code
   * and extra information providing access to the request URL and HTTP method.
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
   * @see #handleError(HttpRequest, ClientHttpResponse, HttpStatusCode)
   */
  @Override
  public void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
    handleError(request, response, response.getStatusCode());
  }

  /**
   * Return error message with details from the response body. For example:
   * <pre>
   * 404 Not Found on GET request for "https://example.com": [{'id': 123, 'message': 'my message'}]
   * </pre>
   */
  private String getErrorMessage(int rawStatusCode, String statusText,
          @Nullable byte[] responseBody, @Nullable Charset charset, HttpRequest request) {

    StringBuilder msg = new StringBuilder(rawStatusCode + " " + statusText);

    msg.append(" on ").append(request.getMethod()).append(" request");
    msg.append(" for \"");
    String urlString = request.getURI().toString();
    int idx = urlString.indexOf('?');
    if (idx != -1) {
      msg.append(urlString, 0, idx);
    }
    else {
      msg.append(urlString);
    }

    msg.append("\": ");
    if (ObjectUtils.isEmpty(responseBody)) {
      msg.append("[no body]");
    }
    else {
      charset = (charset != null ? charset : StandardCharsets.UTF_8);
      String bodyText = new String(responseBody, charset);
      bodyText = LogFormatUtils.formatValue(bodyText, -1, true);
      msg.append(bodyText);
    }
    return msg.toString();
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
   * @since 5.0
   */
  protected void handleError(HttpRequest request, ClientHttpResponse response, HttpStatusCode statusCode) throws IOException {
    String statusText = response.getStatusText();
    HttpHeaders headers = response.getHeaders();
    byte[] body = getResponseBody(response);
    Charset charset = getCharset(response);
    String message = getErrorMessage(statusCode.value(), statusText, body, charset, request);

    RestClientResponseException ex;
    if (statusCode.is4xxClientError()) {
      ex = HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
    }
    else if (statusCode.is5xxServerError()) {
      ex = HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
    }
    else {
      ex = new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
    }

    if (CollectionUtils.isNotEmpty(messageConverters)) {
      ex.setBodyConvertFunction(initBodyConvertFunction(response, body, messageConverters));
    }

    throw ex;
  }

  /**
   * Return a function for decoding the error content. This can be passed to
   * {@link RestClientResponseException#setBodyConvertFunction(Function)}.
   */
  @SuppressWarnings("NullAway")
  protected Function<ResolvableType, ?> initBodyConvertFunction(ClientHttpResponse response, byte[] body, List<HttpMessageConverter<?>> messageConverters) {
    return resolvable -> {
      try {
        var extractor = new HttpMessageConverterExtractor<>(resolvable.getType(), messageConverters);
        return extractor.extractData(new ClientHttpResponseDecorator(response) {

          @Override
          public InputStream getBody() {
            return new ByteArrayInputStream(body);
          }

        });
      }
      catch (IOException ex) {
        throw new RestClientException("Error while extracting response for type [%s]".formatted(resolvable), ex);
      }
    };
  }

  /**
   * Read the body of the given response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the response body as a byte array,
   * or an empty byte array if the body could not be read
   */
  protected byte[] getResponseBody(ClientHttpResponse response) {
    return RestClientUtils.getBody(response);
  }

  /**
   * Determine the charset of the response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the associated charset, or {@code null} if none
   */
  @Nullable
  protected Charset getCharset(ClientHttpResponse response) {
    return RestClientUtils.getCharset(response);
  }

}
