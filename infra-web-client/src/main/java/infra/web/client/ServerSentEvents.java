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

import java.util.function.Function;

import infra.http.ServerSentEvent;
import infra.http.client.ClientHttpResponse;
import infra.http.client.DecoratingClientHttpResponse;

/**
 * A container for {@link ServerSentEvent} instances that supports iteration.
 * <p>This class implements {@link Iterable} to allow enhanced for-loop traversal
 * of Server-Sent Events (SSE) using the underlying {@link ServerSentEventIterator}.
 *
 * @param <T> the type of data contained in the SSE events, which may be {@code null}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/10 15:00
 */
public class ServerSentEvents<T extends @Nullable Object> extends DecoratingClientHttpResponse
        implements Iterable<ServerSentEvent<T>> {

  private final ServerSentEventIterator<T> iterator;

  ServerSentEvents(ClientHttpResponse response, Function<String, T> converter) {
    super(response);
    this.iterator = new ServerSentEventIterator<>(response, converter);
  }

  /**
   * Returns an iterator over the Server-Sent Events.
   *
   * @return the {@link ServerSentEventIterator} for traversing the SSE events
   */
  @Override
  public ServerSentEventIterator<T> iterator() {
    return iterator;
  }

  @Override
  public void close() {
    iterator.close();
  }

}
