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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.ServerSentEvent;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SseEventIterator} and SSE parsing.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class SseEventIteratorTests {

  private static final ServerSentEvent.Builder<String> EVENT = ServerSentEvent.builder();

  @Test
  void parseSimpleDataEvent() {
    SseEventIterator it = iterator("data: hello world\n\n");

    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.data()).isEqualTo("hello world");
    assertThat(event.event()).isNull();
    assertThat(event.id()).isNull();

    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultiLineData() {
    SseEventIterator it = iterator("data: line1\ndata: line2\ndata: line3\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("line1\nline2\nline3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithIdAndType() {
    SseEventIterator it = iterator("id: 123\nevent: update\ndata: {\"key\":\"value\"}\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.id()).isEqualTo("123");
    assertThat(event.event()).isEqualTo("update");
    assertThat(event.data()).isEqualTo("{\"key\":\"value\"}");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseRetry() {
    SseEventIterator it = iterator("retry: 5000\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().retry()).isEqualTo(Duration.ofMillis(5000));
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseComment() {
    SseEventIterator it = iterator(": this is a comment\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEqualTo("this is a comment");
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultipleEvents() {
    SseEventIterator it = iterator("""
            event: foo
            data: first

            event: bar
            data: second

            """);

    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().event()).isEqualTo("foo");

    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().event()).isEqualTo("bar");

    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEmptyDataEvent() {
    SseEventIterator it = iterator("data: \n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithoutData() {
    SseEventIterator it = iterator("event: ping\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.event()).isEqualTo("ping");
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseIncompleteEventAtEnd() {
    SseEventIterator it = iterator("data: incomplete");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("incomplete");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void lastEventId() {
    SseEventIterator it = iterator("id: 1\ndata: a\n\nid: 2\ndata: b\n\nid: 3\ndata: c\n\n");
    assertThat(it.next().id()).isEqualTo("1");
    assertThat(it.lastEventId()).isEqualTo("1");
    assertThat(it.next().id()).isEqualTo("2");
    assertThat(it.lastEventId()).isEqualTo("2");
    assertThat(it.next().id()).isEqualTo("3");
    assertThat(it.lastEventId()).isEqualTo("3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void nextThrowsWhenEmpty() {
    SseEventIterator it = iterator("");
    assertThat(it.hasNext()).isFalse();
    assertThatThrownBy(it::next).isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void fieldWithoutColon() {
    SseEventIterator it = iterator("data\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void fieldWithSpaceAfterColon() {
    SseEventIterator it = iterator("data: hello\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("hello");
  }

  @Test
  void callbackConsumerInvokedForEachEvent() throws IOException {
    ClientHttpResponse response = mockResponse("event: foo\ndata: bar\n\nevent: baz\ndata: qux\n\n");
    List<ServerSentEvent<String>> captured = new ArrayList<>();

    // simulate the default method on ResponseSpec
    try (SseEventIterator it = new SseEventIterator(response)) {
      while (it.hasNext()) {
        captured.add(it.next());
      }
    }

    assertThat(captured).hasSize(2);
    assertThat(captured.get(0).event()).isEqualTo("foo");
    assertThat(captured.get(1).event()).isEqualTo("baz");
  }

  @Test
  void serverSentEvent_equalsAndHashCode() {
    ServerSentEvent<String> a = EVENT.id("1").event("e").data("d").build();
    ServerSentEvent<String> b = EVENT.id("1").event("e").data("d").build();
    ServerSentEvent<String> c = EVENT.id("2").build();

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(c);
  }

  @Test
  void serverSentEvent_format() {
    ServerSentEvent<String> event = EVENT.id("1").event("update").data("hello").build();
    String formatted = event.format();
    assertThat(formatted).contains("id:1", "event:update", "data:");
  }

  // --- helpers ---

  private static SseEventIterator iterator(String sseContent) {
    try {
      return new SseEventIterator(mockResponse(sseContent));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static ClientHttpResponse mockResponse(String sseContent) throws IOException {
    InputStream body = new ByteArrayInputStream(sseContent.getBytes(StandardCharsets.UTF_8));
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(headers);
    when(response.getBody()).thenReturn(body);
    return response;
  }
}
