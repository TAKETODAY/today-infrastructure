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

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/8/23 14:35
 */
class JdkClientHttpRequestTests {

  private final HttpClient client = mock(HttpClient.class);

  @AutoClose("shutdownNow")
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Test
  @SuppressWarnings("unchecked")
  void futureCancelledAfterTimeout() {
    CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();
    when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(future);

    assertThatThrownBy(() -> createRequest(Duration.ofMillis(10)).executeInternal(HttpHeaders.forWritable(), null))
            .isExactlyInstanceOf(HttpTimeoutException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void futureCancelled() {
    CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();
    future.cancel(true);
    when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(future);

    assertThatThrownBy(() -> createRequest(null).executeInternal(HttpHeaders.forWritable(), null))
            .isExactlyInstanceOf(IOException.class);
  }

  private JdkClientHttpRequest createRequest(Duration timeout) {
    return new JdkClientHttpRequest(client, URI.create("https://abc.com"), HttpMethod.GET, executor, timeout, false);
  }

}