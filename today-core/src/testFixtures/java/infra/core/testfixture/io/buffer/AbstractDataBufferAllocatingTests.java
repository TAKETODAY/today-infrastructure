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

package infra.core.testfixture.io.buffer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.io.buffer.NettyDataBufferFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Base class for tests that read or write data buffers with an extension to check
 * that allocated buffers have been released.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public abstract class AbstractDataBufferAllocatingTests {

  private static UnpooledByteBufAllocator netty4OffHeapUnpooled;

  private static UnpooledByteBufAllocator netty4OnHeapUnpooled;

  private static PooledByteBufAllocator netty4OffHeapPooled;

  private static PooledByteBufAllocator netty4OnHeapPooled;

  @RegisterExtension
  AfterEachCallback leakDetector = context -> waitForDataBufferRelease(Duration.ofSeconds(2));

  protected DataBufferFactory bufferFactory;

  protected DataBuffer createDataBuffer(int capacity) {
    return this.bufferFactory.allocateBuffer(capacity);
  }

  protected DataBuffer stringBuffer(String value) {
    return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
  }

  protected Mono<DataBuffer> deferStringBuffer(String value) {
    return Mono.defer(() -> Mono.just(stringBuffer(value)));
  }

  protected DataBuffer byteBuffer(byte[] value) {
    DataBuffer buffer = this.bufferFactory.allocateBuffer(value.length);
    buffer.write(value);
    return buffer;
  }

  protected void release(DataBuffer... buffers) {
    Arrays.stream(buffers).forEach(DataBuffer.RELEASE_CONSUMER);
  }

  protected Consumer<DataBuffer> stringConsumer(String expected) {
    return stringConsumer(expected, UTF_8);
  }

  protected Consumer<DataBuffer> stringConsumer(String expected, Charset charset) {
    return dataBuffer -> {
      String value = dataBuffer.toString(charset);
      dataBuffer.release();
      assertThat(value).isEqualTo(expected);
    };
  }

  /**
   * Wait until allocations are at 0, or the given duration elapses.
   */
  private void waitForDataBufferRelease(Duration duration) throws InterruptedException {
    Instant start = Instant.now();
    while (true) {
      try {
        verifyAllocations();
        break;
      }
      catch (AssertionError ex) {
        if (Instant.now().isAfter(start.plus(duration))) {
          throw ex;
        }
      }
      Thread.sleep(50);
    }
  }

  private void verifyAllocations() {
    if (this.bufferFactory instanceof NettyDataBufferFactory) {
      ByteBufAllocator allocator = ((NettyDataBufferFactory) this.bufferFactory).getByteBufAllocator();
      if (allocator instanceof PooledByteBufAllocator) {
        Instant start = Instant.now();
        while (true) {
          PooledByteBufAllocatorMetric metric = ((PooledByteBufAllocator) allocator).metric();
          long total = getAllocations(metric.directArenas()) + getAllocations(metric.heapArenas());
          if (total == 0) {
            return;
          }
          if (Instant.now().isBefore(start.plus(Duration.ofSeconds(5)))) {
            try {
              Thread.sleep(50);
            }
            catch (InterruptedException ex) {
              // ignore
            }
            continue;
          }
          assertThat(total).as("ByteBuf Leak: " + total + " unreleased allocations").isEqualTo(0);
        }
      }
    }
  }

  private static long getAllocations(List<PoolArenaMetric> metrics) {
    return metrics.stream().mapToLong(PoolArenaMetric::numActiveAllocations).sum();
  }

  @BeforeAll
  @SuppressWarnings("deprecation") // PooledByteBufAllocator no longer supports tinyCacheSize.
  public static void createAllocators() {
    netty4OnHeapUnpooled = new UnpooledByteBufAllocator(false);
    netty4OffHeapUnpooled = new UnpooledByteBufAllocator(true);
    netty4OnHeapPooled = new PooledByteBufAllocator(false, 1, 1, 4096, 4, 0, 0, 0, true);
    netty4OffHeapPooled = new PooledByteBufAllocator(true, 1, 1, 4096, 4, 0, 0, 0, true);

  }

  @AfterAll
  public static void closeAllocators() {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("infra.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests#dataBufferFactories()")
  public @interface ParameterizedDataBufferAllocatingTest {
  }

  public static Stream<Arguments> dataBufferFactories() {
    return Stream.of(
            // Netty 4
            arguments(named("NettyDataBufferFactory - UnpooledByteBufAllocator - preferDirect = true",
                    new NettyDataBufferFactory(netty4OffHeapUnpooled))),
            arguments(named("NettyDataBufferFactory - UnpooledByteBufAllocator - preferDirect = false",
                    new NettyDataBufferFactory(netty4OnHeapUnpooled))),
            arguments(named("NettyDataBufferFactory - PooledByteBufAllocator - preferDirect = true",
                    new NettyDataBufferFactory(netty4OffHeapPooled))),
            arguments(named("NettyDataBufferFactory - PooledByteBufAllocator - preferDirect = false",
                    new NettyDataBufferFactory(netty4OnHeapPooled))),

            // Default
            arguments(named("DefaultDataBufferFactory - preferDirect = true",
                    new DefaultDataBufferFactory(true))),
            arguments(named("DefaultDataBufferFactory - preferDirect = false",
                    new DefaultDataBufferFactory(false)))
    );
  }

}
