/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.client.ClientHttpResponse;

/**
 * Represents a streaming HTTP response where the body is consumed incrementally
 * without buffering the entire payload. Suitable for large downloads,
 * chunked transfer encoding, line-based protocols (including SSE), or any
 * scenario where the entire response should not be held in memory.
 *
 * <p>Implements {@link Closeable} to release the underlying HTTP connection.
 *
 * <h3>Usage Examples</h3>
 *
 * <p><strong>Raw byte streaming:</strong>
 * <pre>{@code
 * try (ResponseStream stream = client.get()
 *      .uri("https://example.com/large-file")
 *      .retrieve()
 *      .bodyStream()) {
 *
 *   InputStream in = stream.inputStream();
 *   byte[] buf = new byte[8192];
 *   int n;
 *   while ((n = in.read(buf)) != -1) {
 *     // process bytes
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Line-based streaming:</strong>
 * <pre>{@code
 * try (ResponseStream stream = client.get()
 *      .uri("https://example.com/stream")
 *      .retrieve()
 *      .bodyStream()) {
 *
 *   BufferedReader reader = stream.lines();
 *   String line;
 *   while ((line = reader.readLine()) != null) {
 *     // process line
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 * @see SseEventIterator
 */
public class ResponseStream implements Closeable {

  private final ClientHttpResponse response;

  private @Nullable BufferedReader reader;

  ResponseStream(ClientHttpResponse response) {
    this.response = response;
  }

  /**
   * Return the response headers.
   */
  public HttpHeaders headers() {
    return response.getHeaders();
  }

  /**
   * Return the HTTP status code.
   */
  public HttpStatusCode statusCode() {
    return response.getStatusCode();
  }

  /**
   * Return the raw status code value.
   */
  public int rawStatusCode() {
    return response.getRawStatusCode();
  }

  /**
   * Return the response body as a binary {@link InputStream}.
   */
  public InputStream inputStream() {
    try {
      return response.getBody();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Return a {@link BufferedReader} over the response body, using the charset
   * from the {@code Content-Type} header, defaulting to {@code UTF-8}.
   */
  public BufferedReader lines() {
    if (reader == null) {
      Charset charset = resolveCharset();
      reader = new BufferedReader(new InputStreamReader(inputStream(), charset));
    }
    return reader;
  }

  private Charset resolveCharset() {
    MediaType contentType = response.getHeaders().getContentType();
    if (contentType != null) {
      Charset charset = contentType.getCharset();
      if (charset != null) {
        return charset;
      }
    }
    return StandardCharsets.UTF_8;
  }

  /**
   * Close the underlying HTTP response and release resources.
   */
  @Override
  public void close() {
    response.close();
  }
}
