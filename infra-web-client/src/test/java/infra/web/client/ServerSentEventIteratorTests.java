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
import java.util.NoSuchElementException;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.ServerSentEvent;
import infra.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ServerSentEventIterator} and SSE parsing.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ServerSentEventIteratorTests {

  private static final ServerSentEvent.Builder<String> EVENT = ServerSentEvent.builder();

  @Test
  void parseSimpleDataEvent() {
    ServerSentEventIterator<String> it = iterator("data: hello world\n\n");

    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.data()).isEqualTo("hello world");
    assertThat(event.event()).isNull();
    assertThat(event.id()).isNull();

    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultiLineData() {
    ServerSentEventIterator<String> it = iterator("data: line1\ndata: line2\ndata: line3\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("line1\nline2\nline3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithIdAndType() {
    ServerSentEventIterator<String> it = iterator("id: 123\nevent: update\ndata: {\"key\":\"value\"}\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.id()).isEqualTo("123");
    assertThat(event.event()).isEqualTo("update");
    assertThat(event.data()).isEqualTo("{\"key\":\"value\"}");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseRetry() {
    ServerSentEventIterator<String> it = iterator("retry: 5000\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().retry()).isEqualTo(Duration.ofMillis(5000));
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseComment() {
    ServerSentEventIterator<String> it = iterator(": this is a comment\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEqualTo("this is a comment");
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultipleEvents() {
    ServerSentEventIterator<String> it = iterator("""
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
    ServerSentEventIterator<String> it = iterator("data: \n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithoutData() {
    ServerSentEventIterator<String> it = iterator("event: ping\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.event()).isEqualTo("ping");
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseIncompleteEventAtEnd() {
    ServerSentEventIterator<String> it = iterator("data: incomplete");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("incomplete");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void lastEventId() {
    ServerSentEventIterator<String> it = iterator("id: 1\ndata: a\n\nid: 2\ndata: b\n\nid: 3\ndata: c\n\n");
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
    ServerSentEventIterator<String> it = iterator("");
    assertThat(it.hasNext()).isFalse();
    assertThatThrownBy(it::next).isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void fieldWithoutColon() {
    ServerSentEventIterator<String> it = iterator("data\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void fieldWithSpaceAfterColon() {
    ServerSentEventIterator<String> it = iterator("data: hello\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("hello");
  }

  @Test
  void callbackConsumerInvokedForEachEvent() throws IOException {
    ClientHttpResponse response = mockResponse("event: foo\ndata: bar\n\nevent: baz\ndata: qux\n\n");
    List<ServerSentEvent<String>> captured = new ArrayList<>();

    // simulate the default method on ResponseSpec
    try (ServerSentEventIterator<String> it = new ServerSentEventIterator<>(response, Function.identity())) {
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

  // ... existing code ...

  @Test
  void parseWindowsLineEndings() {
    ServerSentEventIterator<String> it = iterator("data: hello\r\n\r\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("hello");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMixedLineEndings() {
    ServerSentEventIterator<String> it = iterator("data: line1\ndata: line2\r\ndata: line3\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("line1\nline2\nline3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseDataWithLeadingSpaceAfterColon() {
    ServerSentEventIterator<String> it = iterator("data:  hello\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo(" hello");
  }

  @Test
  void parseDataWithoutValue() {
    ServerSentEventIterator<String> it = iterator("data:\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultipleDataFieldsWithEmptyValues() {
    ServerSentEventIterator<String> it = iterator("data:\ndata:\ndata: value\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("\n\nvalue");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseCommentOnly() {
    ServerSentEventIterator<String> it = iterator(": comment only\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEqualTo("comment only");
    assertThat(event.data()).isNull();
    assertThat(event.id()).isNull();
    assertThat(event.event()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseMultipleComments() {
    ServerSentEventIterator<String> it = iterator(": comment1\n: comment2\n: comment3\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEqualTo("comment1\ncomment2\ncomment3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseCommentWithEmptyContent() {
    ServerSentEventIterator<String> it = iterator(":\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseRetryWithInvalidValue() {
    ServerSentEventIterator<String> it = iterator("retry: invalid\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.retry()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseRetryWithNegativeValue() {
    ServerSentEventIterator<String> it = iterator("retry: -1000\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.retry()).isEqualTo(Duration.ofMillis(-1000));
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseRetryWithZero() {
    ServerSentEventIterator<String> it = iterator("retry: 0\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().retry()).isEqualTo(Duration.ZERO);
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseUnknownField() {
    ServerSentEventIterator<String> it = iterator("custom: value\ndata: test\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.data()).isEqualTo("test");
    assertThat(event.event()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithAllFields() {
    ServerSentEventIterator<String> it = iterator("""
            id: 42
            event: message
            data: payload
            retry: 3000
            : a comment
            
            """);
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.id()).isEqualTo("42");
    assertThat(event.event()).isEqualTo("message");
    assertThat(event.data()).isEqualTo("payload");
    assertThat(event.retry()).isEqualTo(Duration.ofMillis(3000));
    assertThat(event.comment()).isEqualTo("a comment");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEmptyEventId() {
    ServerSentEventIterator<String> it = iterator("id:\ndata: test\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.id()).isEmpty();
    assertThat(it.lastEventId()).isEmpty();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseOnlyWhitespaceInData() {
    ServerSentEventIterator<String> it = iterator("data:   \n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("  ");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseConsecutiveEmptyLines() {
    ServerSentEventIterator<String> it = iterator("\n\n\n\n");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventFollowedByOnlyComments() {
    ServerSentEventIterator<String> it = iterator("data: event1\n\n: comment\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("event1");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.comment()).isEqualTo("comment");
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseLargeNumberOfEvents() {
    StringBuilder content = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      content.append("data: event").append(i).append("\n\n");
    }
    ServerSentEventIterator<String> it = iterator(content.toString());

    int count = 0;
    while (it.hasNext()) {
      assertThat(it.next().data()).isEqualTo("event" + count);
      count++;
    }
    assertThat(count).isEqualTo(100);
  }

  @Test
  void parseEventWithSpecialCharacters() {
    ServerSentEventIterator<String> it = iterator("data: {\"key\":\"value\",\"nested\":{\"a\":1}}\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("{\"key\":\"value\",\"nested\":{\"a\":1}}");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithUnicodeCharacters() {
    ServerSentEventIterator<String> it = iterator("data: 你好世界🌍\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("你好世界🌍");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithNewlinesInData() {
    ServerSentEventIterator<String> it = iterator("data: line1\ndata: \ndata: line3\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("line1\n\nline3");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void hasNextCanBeCalledMultipleTimes() {
    ServerSentEventIterator<String> it = iterator("data: test\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.hasNext()).isTrue();
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("test");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void nextAfterHasNextReturnsFalse() {
    ServerSentEventIterator<String> it = iterator("");
    assertThat(it.hasNext()).isFalse();
    assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void closeResourceProperly() throws IOException {
    ClientHttpResponse response = mockResponse("data: test\n\n");
    try (ServerSentEventIterator<String> it = new ServerSentEventIterator<>(response, Function.identity())) {
      assertThat(it.hasNext()).isTrue();
      assertThat(it.next().data()).isEqualTo("test");
    }
  }

  @Test
  void converterTransformsData() {
    ServerSentEventIterator<Integer> it = new ServerSentEventIterator<>(
            mockResponseUnchecked("data: 123\n\n"),
            Integer::parseInt
    );
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo(123);
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void converterWithNullData() {
    ServerSentEventIterator<String> it = iterator("event: ping\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.data()).isNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithTabCharacter() {
    ServerSentEventIterator<String> it = iterator("data: hello\tworld\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("hello\tworld");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseFieldWithMultipleColons() {
    ServerSentEventIterator<String> it = iterator("data: key:value:another\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().data()).isEqualTo("key:value:another");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseEventWithNameContainingSpaces() {
    ServerSentEventIterator<String> it = iterator("event: my custom event\ndata: test\n\n");
    assertThat(it.hasNext()).isTrue();
    ServerSentEvent<String> event = it.next();
    assertThat(event.event()).isEqualTo("my custom event");
    assertThat(event.data()).isEqualTo("test");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void parseIdWithSpaces() {
    ServerSentEventIterator<String> it = iterator("id: 123 abc\ndata: test\n\n");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().id()).isEqualTo("123 abc");
    assertThat(it.hasNext()).isFalse();
  }

  private static ClientHttpResponse mockResponseUnchecked(String sseContent) {
    try {
      return mockResponse(sseContent);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // --- helpers ---

  private static ServerSentEventIterator<String> iterator(String sseContent) {
    try {
      return new ServerSentEventIterator<>(mockResponse(sseContent), Function.identity());
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
