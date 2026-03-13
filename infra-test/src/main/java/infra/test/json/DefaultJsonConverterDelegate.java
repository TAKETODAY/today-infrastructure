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

package infra.test.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.MediaType;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.SmartHttpMessageConverter;
import infra.lang.Assert;
import infra.mock.http.MockHttpInputMessage;
import infra.mock.http.MockHttpOutputMessage;
import infra.util.function.SingletonSupplier;

/**
 * Default {@link JsonConverterDelegate} based on {@link HttpMessageConverter}s.
 *
 * @author Stephane Nicoll
 * @author Rossen Stoyanchev
 * @since 5.0
 */
final class DefaultJsonConverterDelegate implements JsonConverterDelegate {

  private static final MediaType JSON = MediaType.APPLICATION_JSON;

  private final List<HttpMessageConverter<?>> messageConverters;

  DefaultJsonConverterDelegate(Iterable<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = StreamSupport.stream(messageConverters.spliterator(), false).toList();
    Assert.notEmpty(this.messageConverters, "At least one message converter needs to be specified");
  }

  @Override
  public <T> T read(String content, ResolvableType targetType) throws IOException {
    HttpInputMessage message = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
    message.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    return read(message, MediaType.APPLICATION_JSON, targetType);
  }

  /**
   * Convert the given {@link HttpInputMessage} whose content must match the
   * given {@link MediaType} to the requested {@code targetType}.
   *
   * @param message an input message
   * @param mediaType the media type of the input
   * @param targetType the target type
   * @param <T> the converted object type
   * @return a value of the given {@code targetType}
   */
  @SuppressWarnings("unchecked")
  <T> T read(HttpInputMessage message, MediaType mediaType, ResolvableType targetType)
          throws IOException, HttpMessageNotReadableException {

    Class<?> contextClass = targetType.getRawClass();
    SingletonSupplier<Type> javaType = SingletonSupplier.of(targetType::getType);
    for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
      if (messageConverter instanceof GenericHttpMessageConverter<?> genericMessageConverter) {
        Type type = javaType.obtain();
        if (genericMessageConverter.canRead(type, contextClass, mediaType)) {
          return (T) genericMessageConverter.read(type, contextClass, message);
        }
      }
      else if (messageConverter instanceof SmartHttpMessageConverter<?> smartMessageConverter) {
        if (smartMessageConverter.canRead(targetType, mediaType)) {
          return (T) smartMessageConverter.read(targetType, message, null);
        }
      }
      else {
        Class<?> targetClass = (contextClass != null ? contextClass : Object.class);
        if (messageConverter.canRead(targetClass, mediaType)) {
          HttpMessageConverter<T> simpleMessageConverter = (HttpMessageConverter<T>) messageConverter;
          Class<? extends T> clazz = (Class<? extends T>) targetClass;
          return simpleMessageConverter.read(clazz, message);
        }
      }
    }
    throw new IllegalStateException("No converter found to read [%s] to [%s]".formatted(mediaType, targetType));
  }

  /**
   * Convert the given raw value to the given {@code targetType} by writing
   * it first to JSON and reading it back.
   *
   * @param value the value to convert
   * @param targetType the target type
   * @param <T> the converted object type
   * @return a value of the given {@code targetType}
   */
  @Override
  public <T> T map(Object value, ResolvableType targetType) throws IOException {
    MockHttpOutputMessage outputMessage = writeToJson(value, ResolvableType.forInstance(value));
    return read(fromHttpOutputMessage(outputMessage), JSON, targetType);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private MockHttpOutputMessage writeToJson(Object value, ResolvableType valueType) throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    Class<?> valueClass = value.getClass();
    SingletonSupplier<Type> javaType = SingletonSupplier.of(valueType::getType);
    for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
      if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
        Type type = javaType.obtain();
        if (genericMessageConverter.canWrite(type, valueClass, JSON)) {
          genericMessageConverter.write(value, type, JSON, outputMessage);
          return outputMessage;
        }
      }
      else if (messageConverter instanceof SmartHttpMessageConverter smartMessageConverter) {
        if (smartMessageConverter.canWrite(valueType, valueClass, JSON)) {
          smartMessageConverter.write(value, valueType, JSON, outputMessage, null);
          return outputMessage;
        }
      }
      else if (messageConverter.canWrite(valueClass, JSON)) {
        ((HttpMessageConverter<Object>) messageConverter).write(value, JSON, outputMessage);
        return outputMessage;
      }
    }
    throw new IllegalStateException("No converter found to convert [%s] to JSON".formatted(valueType));
  }

  private static HttpInputMessage fromHttpOutputMessage(MockHttpOutputMessage message) {
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(message.getBodyAsBytes());
    inputMessage.getHeaders().setAll(message.getHeaders());
    return inputMessage;
  }

}
