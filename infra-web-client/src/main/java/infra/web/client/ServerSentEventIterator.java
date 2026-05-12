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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.http.ServerSentEvent;
import infra.http.client.ClientHttpResponse;

/**
 * An iterator over {@link ServerSentEvent Server-Sent Events} parsed from a
 * {@code text/event-stream} response body. Implements {@link Closeable}
 * for resource cleanup.
 *
 * <pre>{@code
 * RestClient client = RestClient.create();
 * try (ServerSentEventIterator events = client.get()
 *      .uri("https://example.com/events")
 *      .accept(MediaType.TEXT_EVENT_STREAM)
 *      .retrieve()
 *      .eventStream()) {
 *
 *   while (events.hasNext()) {
 *     ServerSentEvent<String> event = events.next();
 *     System.out.println(event.event() + ": " + event.data());
 *   }
 * }
 * }</pre>
 *
 * <p>For callback-based consumption, use
 * {@link RestClient.ResponseSpec#events(Consumer)}:
 * <pre>{@code
 * client.get().uri(...).retrieve()
 *      .eventStream(event -> System.out.println(event.data()));
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class ServerSentEventIterator<T extends @Nullable Object> implements Iterator<ServerSentEvent<T>>, Closeable {

  private final ClientHttpResponse response;

  private final Function<String, T> converter;

  /**
   * Lazily initialized on first read — avoids opening the body stream if the caller never iterates.
   */
  private @Nullable BufferedReader reader;

  private boolean eof;

  private @Nullable ServerSentEvent<T> nextEvent;

  /**
   * Create a new {@code ServerSentEventIterator} from the given response.
   */
  ServerSentEventIterator(ClientHttpResponse response, Function<String, T> converter) {
    this.response = response;
    this.converter = converter;
  }

  /**
   * Returns {@code true} if more events are available.
   */
  @Override
  public boolean hasNext() {
    if (nextEvent != null) {
      return true;
    }
    if (eof) {
      return false;
    }
    try {
      nextEvent = readEvent();
    }
    catch (IOException e) {
      throw new ResourceAccessException("Failed to read SSE event: " + e.getMessage(), e);
    }
    if (nextEvent == null) {
      eof = true;
    }
    return nextEvent != null;
  }

  /**
   * Returns the next parsed SSE event.
   *
   * @throws NoSuchElementException if no more events are available
   */
  @Override
  public ServerSentEvent<T> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more SSE events available");
    }
    ServerSentEvent<T> event = nextEvent;
    nextEvent = null;
    return event;
  }

  @Override
  public void close() {
    response.close();
  }

  // --- SSE parsing ---

  /**
   * Read and parse the next {@link ServerSentEvent} from the stream.
   * <p>Blank lines with no preceding fields are consumed silently (e.g. leading
   * blank lines or extra whitespace between events). A blank line after one or
   * more parsed fields terminates the current event.
   *
   * @return the next event, or {@code null} if the stream has been exhausted
   */
  private @Nullable ServerSentEvent<T> readEvent() throws IOException {
    var reader = ensureReader();

    String id = null;
    String event = null;
    StringBuilder data = null;
    Duration retry = null;
    StringBuilder comment = null;

    String line;
    while ((line = reader.readLine()) != null) {
      if (line.isEmpty()) {
        // Blank line: if no fields collected yet, skip it (leading / inter-event blank).
        // Otherwise terminate the current event.
        if (id == null && event == null && data == null && retry == null && comment == null) {
          continue;
        }
        break;
      }
      if (line.charAt(0) == ':') {
        if (comment == null) {
          comment = new StringBuilder();
        }
        else {
          comment.append('\n');
        }
        comment.append(line.length() > 1 ? line.substring(1).trim() : "");
        continue;
      }
      int colon = line.indexOf(':');
      String field;
      String value;
      if (colon == -1) {
        field = line;
        value = "";
      }
      else if (colon == 0) {
        // Line starts with ':' — treat as comment (handled above),
        // but if it reaches here, something is off; skip it.
        continue;
      }
      else {
        field = line.substring(0, colon);
        value = line.substring(colon + 1);
        if (!value.isEmpty() && value.charAt(0) == ' ') {
          value = value.substring(1);
        }
      }
      switch (field) {
        case "id":
          id = value;
          break;
        case "event":
          event = value;
          break;
        case "data":
          if (data == null) {
            data = new StringBuilder(value);
          }
          else {
            data.append('\n').append(value);
          }
          break;
        case "retry":
          try {
            retry = Duration.ofMillis(Long.parseLong(value));
          }
          catch (NumberFormatException ignored) {
          }
          break;
        default:
          // ignore unknown fields
      }
    }

    // line==null means EOF; no collected fields means the stream truly ended.
    if (id == null && event == null && data == null && retry == null && comment == null) {
      return null;
    }

    ServerSentEvent.Builder<T> builder = ServerSentEvent.builder(convertIfNecessary(data));
    if (id != null) {
      builder = builder.id(id);
    }
    if (event != null) {
      builder = builder.event(event);
    }
    if (retry != null) {
      builder = builder.retry(retry);
    }
    if (comment != null) {
      builder = builder.comment(comment.toString());
    }
    return builder.build();
  }

  private @Nullable T convertIfNecessary(@Nullable StringBuilder string) {
    if (string != null) {
      return converter.apply(string.toString());
    }
    return null;
  }

  private BufferedReader ensureReader() throws IOException {
    if (reader == null) {
      var charset = RestClientUtils.getCharset(response);
      if (charset == null) {
        charset = StandardCharsets.UTF_8;
      }
      reader = new BufferedReader(new InputStreamReader(response.getBody(), charset));
    }
    return reader;
  }

}

