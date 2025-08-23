/*
 * Copyright 2017 - 2025 the original author or authors.
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
    return new JdkClientHttpRequest(client, URI.create("https://abc.com"), HttpMethod.GET, executor, timeout);
  }

}