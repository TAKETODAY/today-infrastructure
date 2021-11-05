/*
 * Copyright 2002-2020 the original author or authors.
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

package cn.taketoday.web.client;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.Assert;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.http.client.ClientHttpResponse;
import cn.taketoday.web.http.converter.GenericHttpMessageConverter;
import cn.taketoday.web.http.converter.HttpMessageConverter;
import cn.taketoday.web.http.converter.HttpMessageNotReadableException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

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

  private final Type responseType;

  @Nullable
  private final Class<T> responseClass;

  private final List<HttpMessageConverter<?>> messageConverters;

  private final Log logger;

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
    this(responseType, messageConverters, LogFactory.getLog(HttpMessageConverterExtractor.class));
  }

  @SuppressWarnings("unchecked")
  HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, Log logger) {
    Assert.notNull(responseType, "'responseType' must not be null");
    Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
    Assert.noNullElements(messageConverters, "'messageConverters' must not contain null elements");
    this.responseType = responseType;
    this.responseClass = (responseType instanceof Class ? (Class<T>) responseType : null);
    this.messageConverters = messageConverters;
    this.logger = logger;
  }


  @Override
  @SuppressWarnings({ "unchecked", "rawtypes", "resource" })
  public T extractData(ClientHttpResponse response) throws IOException {
    MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
    if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
      return null;
    }
    MediaType contentType = getContentType(responseWrapper);

    try {
      for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
        if (messageConverter instanceof GenericHttpMessageConverter) {
          GenericHttpMessageConverter<?> genericMessageConverter =
                  (GenericHttpMessageConverter<?>) messageConverter;
          if (genericMessageConverter.canRead(this.responseType, null, contentType)) {
            if (logger.isDebugEnabled()) {
              ResolvableType resolvableType = ResolvableType.forType(this.responseType);
              logger.debug("Reading to [" + resolvableType + "]");
            }
            return (T) genericMessageConverter.read(this.responseType, null, responseWrapper);
          }
        }
        if (this.responseClass != null) {
          if (messageConverter.canRead(this.responseClass, contentType)) {
            if (logger.isDebugEnabled()) {
              String className = this.responseClass.getName();
              logger.debug("Reading to [" + className + "] as \"" + contentType + "\"");
            }
            return (T) messageConverter.read((Class) this.responseClass, responseWrapper);
          }
        }
      }
    }
    catch (IOException | HttpMessageNotReadableException ex) {
      throw new RestClientException("Error while extracting response for type [" +
              this.responseType + "] and content type [" + contentType + "]", ex);
    }

    throw new UnknownContentTypeException(this.responseType, contentType,
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
    return new byte[0];
  }
}
