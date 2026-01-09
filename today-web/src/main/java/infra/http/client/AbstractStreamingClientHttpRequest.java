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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;

import infra.http.HttpHeaders;
import infra.http.HttpOutputMessage;
import infra.http.StreamingHttpOutputMessage;
import infra.lang.Assert;
import infra.util.FastByteArrayOutputStream;
import infra.util.concurrent.Future;

/**
 * Extension of {@link AbstractClientHttpRequest} that adds the ability to stream
 * request body content directly to the underlying HTTP client library through
 * the {@link StreamingHttpOutputMessage} contract.
 *
 * <p>It is necessary to call {@link #setBody} and stream the request body through
 * a callback for access to the {@code OutputStream}. The alternative to call
 * {@link #getBody()} is also supported as a fallback, but that does not stream,
 * and returns an aggregating {@code OutputStream} instead.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/1 22:47
 */
abstract class AbstractStreamingClientHttpRequest extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

  @Nullable
  private Body body;

  @Nullable
  private FastByteArrayOutputStream bodyStream;

  /**
   * Implements the {@link HttpOutputMessage} contract for request body content.
   * <p>Note that this method does not result in streaming, and the returned
   * {@code OutputStream} aggregates the full content in a byte[] before
   * sending. To use streaming, call {@link #setBody} instead.
   */
  @Override
  protected final OutputStream getBodyInternal(HttpHeaders headers) {
    Assert.state(this.body == null, "Invoke either getBody or setBody; not both");

    if (this.bodyStream == null) {
      this.bodyStream = new FastByteArrayOutputStream(1024);
    }
    return this.bodyStream;
  }

  /**
   * Implements the {@link StreamingHttpOutputMessage} contract for writing
   * request body by streaming directly to the underlying HTTP client.
   */
  @Override
  public final void setBody(Body body) {
    Assert.notNull(body, "Body is required");
    assertNotExecuted();
    Assert.state(this.bodyStream == null, "Invoke either getBody or setBody; not both");

    this.body = body;
  }

  @Override
  protected final ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
    FastByteArrayOutputStream bodyStream = this.bodyStream;
    if (this.body == null && bodyStream != null) {
      this.body = bodyStream::writeTo;
    }
    return executeInternal(headers, this.body);
  }

  @Override
  protected final Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Executor executor) {
    FastByteArrayOutputStream bodyStream = this.bodyStream;
    if (this.body == null && bodyStream != null) {
      this.body = bodyStream::writeTo;
    }
    return asyncInternal(headers, body, executor);
  }

  /**
   * Abstract method for concrete implementations to write the headers and
   * {@link StreamingHttpOutputMessage.Body} to the HTTP request.
   *
   * @param headers the HTTP headers for the request
   * @param body the HTTP body, may be {@code null} if no body was {@linkplain #setBody(Body) set}
   * @return the response object for the executed request
   */
  protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body)
          throws IOException;

  /**
   * Abstract method for concrete implementations to write the headers and
   * {@link StreamingHttpOutputMessage.Body} to the HTTP request.
   *
   * @param headers the HTTP headers for the request
   * @param body the HTTP body, may be {@code null} if no body was {@linkplain #setBody(Body) set}
   * @return the response object for the executed request
   */
  protected Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Body body, @Nullable Executor executor) {
    // todo 这样实现肯定不行
    return Future.run(() -> executeInternal(headers, body), executor);
  }

}
