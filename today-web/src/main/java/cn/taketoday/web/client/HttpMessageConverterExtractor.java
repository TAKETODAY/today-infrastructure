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

package cn.taketoday.web.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.FileCopyUtils;

/**
 * Response extractor that uses the given {@linkplain HttpMessageConverter entity converters}
 * to convert the response into a type {@code T}.
 *
 * @param <T> the data type
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @see RestTemplate
 * @since 4.0
 */
public class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {
  private final Logger logger;
  private final Type responseType;

  @Nullable
  private final Class<T> responseClass;
  private final List<HttpMessageConverter<?>> messageConverters;

  /**
   * Create a new instance of the {@code HttpMessageConverterExtractor} with the given response
   * type and message converters. The given converters must support the response type.
   */
  public HttpMessageConverterExtractor(Class<T> responseType, List<HttpMessageConverter<?>> messageConverters) {
    this((Type) responseType, messageConverters);
  }

  /**
   * Creates a new instance of the {@code HttpMessageConverterExtractor} with the given response
   * type and message converters. The given converters must support the response type.
   */
  public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters) {
    this(responseType, messageConverters, LoggerFactory.getLogger(HttpMessageConverterExtractor.class));
  }

  @SuppressWarnings("unchecked")
  public HttpMessageConverterExtractor(
          Type responseType, List<HttpMessageConverter<?>> messageConverters, Logger logger) {
    Assert.notNull(responseType, "'responseType' is required");
    Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
    Assert.noNullElements(messageConverters, "'messageConverters' must not contain null elements");
    this.logger = logger;
    this.responseType = responseType;
    this.responseClass = (responseType instanceof Class ? (Class<T>) responseType : null);
    this.messageConverters = messageConverters;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public T extractData(ClientHttpResponse response) throws IOException {
    IntrospectingClientHttpResponse responseWrapper = new IntrospectingClientHttpResponse(response);
    if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
      return null;
    }

    MediaType contentType = getContentType(responseWrapper);
    try {
      for (HttpMessageConverter<?> messageConverter : messageConverters) {
        if (messageConverter instanceof GenericHttpMessageConverter<?> genericConverter) {
          if (genericConverter.canRead(responseType, null, contentType)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Reading to [{}]", ResolvableType.forType(responseType));
            }
            return (T) genericConverter.read(responseType, null, responseWrapper);
          }
        }
        if (responseClass != null) {
          if (messageConverter.canRead(responseClass, contentType)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Reading to [{}] as \"{}\"", responseClass.getName(), contentType);
            }
            return (T) messageConverter.read((Class) responseClass, responseWrapper);
          }
        }
      }
    }
    catch (IOException | HttpMessageNotReadableException ex) {
      throw new RestClientException(
              "Error while extracting response for type ["
                      + responseType + "] and content type [" + contentType + "]", ex);
    }

    throw new UnknownContentTypeException(
            responseType, contentType,
            responseWrapper.getRawStatusCode(), responseWrapper.getStatusText(),
            responseWrapper.getHeaders(), getResponseBody(responseWrapper));
  }

  /**
   * Determine the Content-Type of the response based on the "Content-Type"
   * header or otherwise default to {@link MediaType#APPLICATION_OCTET_STREAM}.
   *
   * @param response the response
   * @return the MediaType, or "application/octet-stream"
   */
  protected MediaType getContentType(ClientHttpResponse response) {
    MediaType contentType = response.getHeaders().getContentType();
    if (contentType == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("No content-type, using 'application/octet-stream'");
      }
      contentType = MediaType.APPLICATION_OCTET_STREAM;
    }
    return contentType;
  }

  private static byte[] getResponseBody(ClientHttpResponse response) {
    try {
      return FileCopyUtils.copyToByteArray(response.getBody());
    }
    catch (IOException ex) {
      // ignore
    }
    return Constant.EMPTY_BYTES;
  }
}
