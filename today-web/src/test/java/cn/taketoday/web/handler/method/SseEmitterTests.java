/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.web.handler.method.SseEmitter.event;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 15:50
 */
class SseEmitterTests {

  private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

  private SseEmitter emitter;

  private TestHandler handler;

  @BeforeEach
  public void setup() throws IOException {
    this.handler = new TestHandler();
    this.emitter = new SseEmitter();
    this.emitter.initialize(this.handler);
  }

  @Test
  public void send() throws Exception {
    this.emitter.send("foo");
    this.handler.assertSentObjectCount(3);
    this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo");
    this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  @Test
  public void sendWithMediaType() throws Exception {
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.handler.assertSentObjectCount(3);
    this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo", MediaType.TEXT_PLAIN);
    this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  @Test
  public void sendEventEmpty() throws Exception {
    this.emitter.send(event());
    this.handler.assertSentObjectCount(0);
    this.handler.assertWriteCount(0);
  }

  @Test
  public void sendEventWithDataLine() throws Exception {
    this.emitter.send(event().data("foo"));
    this.handler.assertSentObjectCount(3);
    this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo");
    this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  @Test
  public void sendEventWithTwoDataLines() throws Exception {
    this.emitter.send(event().data("foo").data("bar"));
    this.handler.assertSentObjectCount(5);
    this.handler.assertObject(0, "data:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo");
    this.handler.assertObject(2, "\ndata:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(3, "bar");
    this.handler.assertObject(4, "\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  @Test
  public void sendEventFull() throws Exception {
    this.emitter.send(event().comment("blah").name("test").reconnectTime(5000L).id("1").data("foo"));
    this.handler.assertSentObjectCount(3);
    this.handler.assertObject(0, ":blah\nevent:test\nretry:5000\nid:1\ndata:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo");
    this.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  @Test
  public void sendEventFullWithTwoDataLinesInTheMiddle() throws Exception {
    this.emitter.send(event().comment("blah").data("foo").data("bar").name("test").reconnectTime(5000L).id("1"));
    this.handler.assertSentObjectCount(5);
    this.handler.assertObject(0, ":blah\ndata:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(1, "foo");
    this.handler.assertObject(2, "\ndata:", TEXT_PLAIN_UTF8);
    this.handler.assertObject(3, "bar");
    this.handler.assertObject(4, "\nevent:test\nretry:5000\nid:1\n\n", TEXT_PLAIN_UTF8);
    this.handler.assertWriteCount(1);
  }

  private static class TestHandler implements ResponseBodyEmitter.Handler {

    private final List<Object> objects = new ArrayList<>();

    private final List<MediaType> mediaTypes = new ArrayList<>();

    private int writeCount;

    public void assertSentObjectCount(int size) {
      assertThat(this.objects).hasSize(size);
    }

    public void assertObject(int index, Object object) {
      assertObject(index, object, null);
    }

    public void assertObject(int index, Object object, MediaType mediaType) {
      assertThat(index).isLessThanOrEqualTo(this.objects.size());
      assertThat(this.objects.get(index)).isEqualTo(object);
      assertThat(this.mediaTypes.get(index)).isEqualTo(mediaType);
    }

    public void assertWriteCount(int writeCount) {
      assertThat(this.writeCount).isEqualTo(writeCount);
    }

    @Override
    public void send(Object data, @Nullable MediaType mediaType) throws IOException {
      this.objects.add(data);
      this.mediaTypes.add(mediaType);
      this.writeCount++;
    }

    @Override
    public void send(Collection<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
      for (ResponseBodyEmitter.DataWithMediaType item : items) {
        this.objects.add(item.data);
        this.mediaTypes.add(item.mediaType);
      }
      this.writeCount++;
    }

    @Override
    public void complete() {
    }

    @Override
    public void completeWithError(Throwable failure) {
    }

    @Override
    public void onTimeout(Runnable callback) {
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
    }

    @Override
    public void onCompletion(Runnable callback) {
    }
  }

}
