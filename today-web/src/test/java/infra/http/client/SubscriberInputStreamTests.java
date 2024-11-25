/*
 * Copyright 2017 - 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/11/23 10:27
 */
class SubscriberInputStreamTests {

  private static final byte[] FOO = "foo".getBytes(UTF_8);

  private static final byte[] BAR = "bar".getBytes(UTF_8);

  private static final byte[] BAZ = "baz".getBytes(UTF_8);

  private final Executor executor = Executors.newSingleThreadExecutor();

  private final OutputStreamPublisher.ByteMapper<byte[]> byteMapper =
          new OutputStreamPublisher.ByteMapper<>() {

            @Override
            public byte[] map(int b) {
              return new byte[] { (byte) b };
            }

            @Override
            public byte[] map(byte[] b, int off, int len) {
              byte[] result = new byte[len];
              System.arraycopy(b, off, result, 0, len);
              return result;
            }
          };

  @Test
  void basic() throws IOException {
    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.flush();
              out.write(BAR);
              out.flush();
              out.write(BAZ);
              out.flush();
            },
            this.byteMapper, this.executor, null);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);

      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(BAR);

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(BAZ);

      assertThat(is.read(chunk)).isEqualTo(-1);
    }
  }

  @Test
  void chunkSize() throws Exception {
    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.write(BAR);
              out.write(BAZ);
            },
            this.byteMapper, this.executor, 2);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);

      StringBuilder sb = new StringBuilder();
      byte[] chunk = new byte[3];

      sb.append((char) is.read());
      assertThat(sb).matches("f");

      assertThat(is.read(chunk)).isEqualTo(3);
      sb.append(new String(chunk, UTF_8));
      assertThat(sb).matches("foob");

      assertThat(is.read(chunk)).isEqualTo(3);
      sb.append(new String(chunk, UTF_8));
      assertThat(sb).matches("foobarb");

      assertThat(is.read(chunk)).isEqualTo(2);
      sb.append(new String(chunk, 0, 2, UTF_8));
      assertThat(sb).matches("foobarbaz");

      assertThat(is.read()).isEqualTo(-1);
    }
  }

  @Test
  void cancel() throws Exception {
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              assertThatIOException()
                      .isThrownBy(() -> {
                        out.write(FOO);
                        out.flush();
                        out.write(BAR);
                        out.flush();
                        latch1.countDown();
                        out.write(BAZ);
                        out.flush();
                      })
                      .withMessage("Subscription has been terminated");
              latch2.countDown();
            }, this.byteMapper, this.executor, null);

    List<byte[]> discarded = new ArrayList<>();

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, discarded::add, 1)) {
      publisher.subscribe(is);
      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      latch1.await();
    }

    latch2.await();
    assertThat(discarded).containsExactly("bar".getBytes(UTF_8));
  }

  @Test
  void closed() throws InterruptedException, IOException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
              writer.write("foo");
              writer.close();
              assertThatIOException().isThrownBy(() -> writer.write("bar")).withMessage("Stream closed");
              latch.countDown();
            },
            this.byteMapper, this.executor, null);

    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> { }, 1)) {
      publisher.subscribe(is);
      byte[] chunk = new byte[3];

      assertThat(is.read(chunk)).isEqualTo(3);
      assertThat(chunk).containsExactly(FOO);

      assertThat(is.read(chunk)).isEqualTo(-1);
    }

    latch.await();
  }

  @Test
  void mapperThrowsException() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
            out -> {
              out.write(FOO);
              out.flush();
              assertThatIOException()
                      .isThrownBy(() -> {
                        out.write(BAR);
                        out.flush();
                      })
                      .withMessage("Subscription has been terminated");
              latch.countDown();
            },
            this.byteMapper, this.executor, null);

    Throwable savedEx = null;

    StringBuilder sb = new StringBuilder();
    try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(
            s -> { throw new NullPointerException("boom"); }, s -> { }, 1)) {

      publisher.subscribe(is);
      sb.append(new String(new byte[] { (byte) is.read() }, UTF_8));
    }
    catch (Throwable ex) {
      savedEx = ex;
    }

    latch.await();

    assertThat(sb.toString()).isEqualTo("");
    assertThat(savedEx).hasMessage("boom");
  }

}