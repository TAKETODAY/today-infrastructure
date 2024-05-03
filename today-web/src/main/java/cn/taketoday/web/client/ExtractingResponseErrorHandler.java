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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Implementation of {@link ResponseErrorHandler} that uses {@link HttpMessageConverter
 * HttpMessageConverters} to convert HTTP error responses to {@link RestClientException
 * RestClientExceptions}.
 *
 * <p>To use this error handler, you must specify a
 * {@linkplain #setStatusMapping(Map) status mapping} and/or a
 * {@linkplain #setSeriesMapping(Map) series mapping}. If either of these mappings has a match
 * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
 * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
 * {@code true}, and {@link #handleError(ClientHttpResponse)} will attempt to use the
 * {@linkplain #setMessageConverters(List) configured message converters} to convert the response
 * into the mapped subclass of {@link RestClientException}. Note that the
 * {@linkplain #setStatusMapping(Map) status mapping} takes precedence over
 * {@linkplain #setSeriesMapping(Map) series mapping}.
 *
 * <p>If there is no match, this error handler will default to the behavior of
 * {@link DefaultResponseErrorHandler}. Note that you can override this default behavior
 * by specifying a {@linkplain #setSeriesMapping(Map) series mapping} from
 * {@code HttpStatus.Series#CLIENT_ERROR} and/or {@code HttpStatus.Series#SERVER_ERROR}
 * to {@code null}.
 *
 * @author Simon Galperin
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
 * @since 4.0
 */
public class ExtractingResponseErrorHandler extends DefaultResponseErrorHandler {

  private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

  private final Map<HttpStatusCode, Class<? extends RestClientException>> statusMapping = new LinkedHashMap<>();

  private final Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping = new LinkedHashMap<>();

  /**
   * Create a new, empty {@code ExtractingResponseErrorHandler}.
   * <p>Note that {@link #setMessageConverters(List)} must be called when using this constructor.
   */
  public ExtractingResponseErrorHandler() {

  }

  /**
   * Create a new {@code ExtractingResponseErrorHandler} with the given
   * {@link HttpMessageConverter} instances.
   *
   * @param messageConverters the message converters to use
   */
  public ExtractingResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  /**
   * Set the message converters to use by this extractor.
   */
  @Override
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  /**
   * Set the mapping from HTTP status code to {@code RestClientException} subclass.
   * If this mapping has a match
   * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
   * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
   * {@code true} and {@link #handleError(ClientHttpResponse)} will attempt to use the
   * {@linkplain #setMessageConverters(List) configured message converters} to convert the
   * response into the mapped subclass of {@link RestClientException}.
   */
  public void setStatusMapping(Map<HttpStatusCode, Class<? extends RestClientException>> statusMapping) {
    if (CollectionUtils.isNotEmpty(statusMapping)) {
      this.statusMapping.putAll(statusMapping);
    }
  }

  /**
   * Set the mapping from HTTP status series to {@code RestClientException} subclass.
   * If this mapping has a match
   * for the {@linkplain ClientHttpResponse#getStatusCode() status code} of a given
   * {@code ClientHttpResponse}, {@link #hasError(ClientHttpResponse)} will return
   * {@code true} and {@link #handleError(ClientHttpResponse)} will attempt to use the
   * {@linkplain #setMessageConverters(List) configured message converters} to convert the
   * response into the mapped subclass of {@link RestClientException}.
   */
  public void setSeriesMapping(Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping) {
    if (CollectionUtils.isNotEmpty(seriesMapping)) {
      this.seriesMapping.putAll(seriesMapping);
    }
  }

  @Override
  protected boolean hasError(HttpStatusCode statusCode) {
    if (this.statusMapping.containsKey(statusCode)) {
      return this.statusMapping.get(statusCode) != null;
    }
    HttpStatus.Series series = HttpStatus.Series.resolve(statusCode.value());
    if (this.seriesMapping.containsKey(series)) {
      return this.seriesMapping.get(series) != null;
    }
    else {
      return super.hasError(statusCode);
    }
  }

  @Override
  public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
    handleError(response, response.getStatusCode(), url, method);
  }

  @Override
  protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode, @Nullable URI url, @Nullable HttpMethod method) throws IOException {
    if (this.statusMapping.containsKey(statusCode)) {
      extract(this.statusMapping.get(statusCode), response);
    }
    HttpStatus.Series series = HttpStatus.Series.resolve(statusCode.value());
    if (this.seriesMapping.containsKey(series)) {
      extract(this.seriesMapping.get(series), response);
    }
    else {
      super.handleError(response, statusCode, url, method);
    }
  }

  private void extract(@Nullable Class<? extends RestClientException> exceptionClass, ClientHttpResponse response) throws IOException {
    if (exceptionClass == null) {
      return;
    }

    var extractor = new HttpMessageConverterExtractor<>(exceptionClass, this.messageConverters);
    RestClientException exception = extractor.extractData(response);
    if (exception != null) {
      throw exception;
    }
  }

}
