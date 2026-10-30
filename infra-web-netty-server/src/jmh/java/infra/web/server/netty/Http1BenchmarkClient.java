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

package infra.web.server.netty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP/1.1 benchmark client based on the JDK {@link HttpClient}. The JDK client
 * manages keep-alive connections and concurrency, which keeps the benchmark
 * driver independent from Netty's HTTP/1.x client codec.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http1BenchmarkClient {

  private final HttpClient client;

  private final HttpRequest request;

  Http1BenchmarkClient(HttpClient client, int port) {
    this.client = client;
    this.request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/bench")).build();
  }

  int get() throws Exception {
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
  }

}
