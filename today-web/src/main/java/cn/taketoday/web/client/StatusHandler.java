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

package cn.taketoday.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.ClientHttpResponseDecorator;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Used by {@link DefaultRestClient} and {@link DefaultRestClientBuilder}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class StatusHandler {

  private final ResponsePredicate predicate;

  private final RestClient.ResponseSpec.ErrorHandler errorHandler;

  private StatusHandler(ResponsePredicate predicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {
    this.predicate = predicate;
    this.errorHandler = errorHandler;
  }

  public static StatusHandler of(Predicate<HttpStatusCode> predicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {
    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");

    return new StatusHandler(response -> predicate.test(response.getStatusCode()), errorHandler);
  }

  public static StatusHandler fromErrorHandler(ResponseErrorHandler errorHandler) {
    Assert.notNull(errorHandler, "ResponseErrorHandler is required");

    return new StatusHandler(errorHandler::hasError, (request, response) ->
            errorHandler.handleError(request.getURI(), request.getMethod(), response));
  }

  public static StatusHandler defaultHandler(List<HttpMessageConverter<?>> messageConverters) {
    return new StatusHandler(response -> response.getStatusCode().isError(), (request, response) -> {
      HttpStatusCode statusCode = response.getStatusCode();
      String statusText = response.getStatusText();
      HttpHeaders headers = response.getHeaders();
      byte[] body = RestClientUtils.getBody(response);
      Charset charset = RestClientUtils.getCharset(response);
      String message = getErrorMessage(statusCode.value(), statusText, body, charset);
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
    });
  }

  private static Function<ResolvableType, ?> initBodyConvertFunction(
          ClientHttpResponse response, byte[] body, List<HttpMessageConverter<?>> messageConverters) {
    Assert.state(CollectionUtils.isNotEmpty(messageConverters), "Expected message converters");
    return resolvableType -> {
      try {
        HttpMessageConverterExtractor<?> extractor =
                new HttpMessageConverterExtractor<>(resolvableType.getType(), messageConverters);

        return extractor.extractData(new ClientHttpResponseDecorator(response) {
          @Override
          public InputStream getBody() {
            return new ByteArrayInputStream(body);
          }
        });
      }
      catch (IOException ex) {
        throw new RestClientException("Error while extracting response for type [%s]".formatted(resolvableType), ex);
      }
    };
  }

  private static String getErrorMessage(int rawStatusCode, String statusText,
          @Nullable byte[] responseBody, @Nullable Charset charset) {

    String preface = "%d %s: ".formatted(rawStatusCode, statusText);

    if (ObjectUtils.isEmpty(responseBody)) {
      return preface + "[no body]";
    }

    charset = (charset != null ? charset : StandardCharsets.UTF_8);

    String bodyText = new String(responseBody, charset);
    bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

    return preface + bodyText;
  }

  public boolean test(ClientHttpResponse response) throws IOException {
    return this.predicate.test(response);
  }

  public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
    this.errorHandler.handle(request, response);
  }

  @FunctionalInterface
  private interface ResponsePredicate {

    boolean test(ClientHttpResponse response) throws IOException;
  }

}
