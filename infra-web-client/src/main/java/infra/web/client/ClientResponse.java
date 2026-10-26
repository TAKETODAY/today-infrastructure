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

import java.nio.charset.Charset;

import infra.core.ParameterizedTypeReference;
import infra.http.client.ClientHttpResponse;

/**
 * Represents an HTTP response, as returned by {@link RestClient}.
 * Provides access to the response status and headers, and also
 * methods to consume the response body.
 *
 * <p> Extension of {@link ClientHttpResponse} that can convert the body.
 *
 * <p>Body access methods consume the same underlying stream. Unless buffering
 * is explicitly configured, repeated reads are not supported. These methods
 * do not close the response; its owner is responsible for calling {@link #close()},
 * including when reading fails.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/29 17:39
 */
public interface ClientResponse extends ClientHttpResponse {

  /**
   * Extract the response body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body, or {@code null} if no response body was available
   */
  <T> @Nullable T bodyTo(Class<T> bodyType) throws RestClientException;

  /**
   * Extract the response body as an object of the given type.
   *
   * @param bodyType the type of return value
   * @param <T> the body type
   * @return the body, or {@code null} if no response body was available
   */
  <T> @Nullable T bodyTo(ParameterizedTypeReference<T> bodyType) throws RestClientException;

  /**
   * Read the remaining response body as a byte array.
   * <p>Consumes the body without closing the stream or the response.
   *
   * @return the remaining body content, or an empty array if no content remains
   * @throws RestClientException if reading the response body fails
   */
  @Override
  byte[] getContentAsByteArray() throws RestClientException;

  /**
   * Read the remaining response body as a UTF-8 string.
   * <p>Consumes the body without closing the stream or the response.
   *
   * @return the remaining body content, or an empty string if no content remains
   * @throws RestClientException if reading the response body fails
   */
  @Override
  String getContentAsString() throws RestClientException;

  /**
   * Read the remaining response body as a string using the given charset,
   * regardless of the charset declared in the response headers.
   * <p>Consumes the body without closing the stream or the response.
   *
   * @param charset the charset to use to turn the body content to a String,
   * or {@code null} to use UTF-8
   * @return the remaining body content, or an empty string if no content remains
   * @throws RestClientException if reading the response body fails
   */
  @Override
  String getContentAsString(@Nullable Charset charset) throws RestClientException;

}
