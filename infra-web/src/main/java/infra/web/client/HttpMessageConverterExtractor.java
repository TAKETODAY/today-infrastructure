/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import infra.core.ResolvableType;
import infra.http.MediaType;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.SmartHttpMessageConverter;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Response extractor that uses the given {@linkplain HttpMessageConverter entity converters}
 * to convert the response into a type {@code T}.
 *
 * @param <T> the data type
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see RestTemplate
 * @since 4.0
 */
public class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {

  private final Logger logger;

  private final Type responseType;

  @Nullable
  private final Class<T> responseClass;

  private final List<HttpMessageConverter<?>> messageConverters;

  // @since 5.0
  private final boolean detectEmptyMessageBody;

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
    this(responseType, messageConverters, true);
  }

  /**
   * Creates a new instance of the {@code HttpMessageConverterExtractor} with the given response
   * type and message converters. The given converters must support the response type.
   *
   * @since 5.0
   */
  public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, boolean detectEmptyMessageBody) {
    this(responseType, messageConverters, LoggerFactory.getLogger(HttpMessageConverterExtractor.class), detectEmptyMessageBody);
  }

  public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, Logger logger) {
    this(responseType, messageConverters, logger, true);
  }

  /**
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, Logger logger, boolean detectEmptyMessageBody) {
    Assert.notNull(responseType, "'responseType' is required");
    Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
    Assert.noNullElements(messageConverters, "'messageConverters' must not contain null elements");
    this.logger = logger;
    this.responseType = responseType;
    this.messageConverters = messageConverters;
    this.detectEmptyMessageBody = detectEmptyMessageBody;
    this.responseClass = (responseType instanceof Class ? (Class<T>) responseType : null);
  }

  @Nullable
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public T extractData(ClientHttpResponse response) throws IOException {
    if (detectEmptyMessageBody) {
      var responseWrapper = new IntrospectingClientHttpResponse(response);
      if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
        return null;
      }
      response = responseWrapper;
    }

    MediaType contentType = getContentType(response);
    try {
      ResolvableType resolvableType = null;
      for (HttpMessageConverter<?> converter : messageConverters) {
        if (converter instanceof GenericHttpMessageConverter<?> generic) {
          if (generic.canRead(responseType, null, contentType)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Reading to [{}]", ResolvableType.forType(responseType));
            }
            return (T) generic.read(responseType, null, response);
          }
        }
        else if (converter instanceof SmartHttpMessageConverter smart) {
          if (resolvableType == null) {
            resolvableType = ResolvableType.forType(responseType);
          }
          if (smart.canRead(resolvableType, contentType)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Reading to [{}]", resolvableType);
            }
            return (T) smart.read(resolvableType, response, null);
          }
        }
        else if (responseClass != null && converter.canRead(responseClass, contentType)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Reading to [{}] as \"{}\"", responseClass.getName(), contentType);
          }
          return (T) converter.read((Class) responseClass, response);
        }
      }
    }
    catch (IOException | HttpMessageNotReadableException ex) {
      throw new RestClientException("Error while extracting response for type [%s] and content type [%s]"
              .formatted(responseType, contentType), ex);
    }

    throw new UnknownContentTypeException(responseType, contentType, response.getRawStatusCode(),
            response.getStatusText(), response.getHeaders(), RestClientUtils.getBody(response));
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

}
