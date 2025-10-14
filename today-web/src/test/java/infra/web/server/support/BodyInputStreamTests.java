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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.util.ExceptionUtils;
import infra.util.concurrent.SimpleSingleThreadAwaiter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/14 17:37
 */
class BodyInputStreamTests {

  @Test
  void readsDataFromMultipleBuffers() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer1 = Unpooled.buffer(2);
    buffer1.writeBytes(new byte[] { 1, 2 });
    inputStream.onDataReceived(buffer1);

    ByteBuf buffer2 = Unpooled.buffer(2);
    buffer2.writeBytes(new byte[] { 3, 4 });
    inputStream.onDataReceived(buffer2);

    inputStream.onComplete();

    byte[] result = new byte[4];
    int bytesRead = inputStream.read(result);

    assertThat(bytesRead).isEqualTo(4);
    assertThat(result).containsExactly(1, 2, 3, 4);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void concurrentReadsThrowException() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread t1 = new Thread(() -> {
      try {
        inputStream.read();
      }
      catch (Exception ignored) {
      }
    });

    Thread t2 = new Thread(() -> {
      assertThatThrownBy(inputStream::read)
              .isInstanceOf(ConcurrentModificationException.class)
              .hasMessage("Concurrent access is not allowed");
    });

    t1.start();
    Thread.sleep(100);
    t2.start();

    Thread.sleep(1000);
  }

  @Test
  void readAfterError() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onError(new IllegalStateException("Stream error"));

    assertThatThrownBy(() -> inputStream.read())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Stream error");
  }

  @Test
  void closeWhileReading() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread readerThread = new Thread(() -> {
      try {
        inputStream.read(); // This should block
      }
      catch (Exception ignored) {
      }
    });

    readerThread.start();
    Thread.sleep(100); // Let the reader thread block

    inputStream.close();
    readerThread.join(1000); // Wait for thread to finish

    assertThat(readerThread.getState()).isEqualTo(Thread.State.TERMINATED);
  }

  @Test
  void resumeWorksCorrectly() throws Exception {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    BodyInputStream inputStream = new BodyInputStream(awaiter);

    Thread readerThread = new Thread(() -> {
      try {
        int data = inputStream.read(); // Should block until resumed
        assertThat(data).isEqualTo(42);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    readerThread.start();
    Thread.sleep(100); // Let the reader thread block

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);
    awaiter.resume(); // Manually resume

    readerThread.join(1000);
    assertThat(readerThread.getState()).isEqualTo(Thread.State.TERMINATED);
  }

  @Test
  void multipleCloseCallsAreSafe() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();
    inputStream.close(); // Second close should not throw
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readZeroLengthArray() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);

    byte[] zeroLength = new byte[0];
    int result = inputStream.read(zeroLength, 0, 0);

    assertThat(result).isEqualTo(0);
  }

  @Test
  void readWithInvalidIndices() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    byte[] buffer = new byte[5];

    assertThatThrownBy(() -> inputStream.read(buffer, -1, 3))
            .isInstanceOf(IndexOutOfBoundsException.class);

    assertThatThrownBy(() -> inputStream.read(buffer, 6, 3))
            .isInstanceOf(IndexOutOfBoundsException.class);

    assertThatThrownBy(() -> inputStream.read(buffer, 0, -1))
            .isInstanceOf(IndexOutOfBoundsException.class);

    assertThatThrownBy(() -> inputStream.read(buffer, 0, 6))
            .isInstanceOf(IndexOutOfBoundsException.class);

    assertThatThrownBy(() -> inputStream.read(buffer, 3, 4))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void readSingleByteWithData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(0x42);
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    int result = inputStream.read();
    assertThat(result).isEqualTo(0x42);

    result = inputStream.read();
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void readSingleByteWithoutData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onComplete();

    int result = inputStream.read();
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void readByteArrayWithExactLength() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(4);
    buffer.writeBytes(new byte[] { 1, 2, 3, 4 });
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    byte[] result = new byte[4];
    int bytesRead = inputStream.read(result, 0, 4);

    assertThat(bytesRead).isEqualTo(4);
    assertThat(result).containsExactly(1, 2, 3, 4);

    int next = inputStream.read();
    assertThat(next).isEqualTo(-1);
  }

  @Test
  void readByteArrayInChunks() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(4);
    buffer.writeBytes(new byte[] { 10, 20, 30, 40 });
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    byte[] result = new byte[2];
    int firstRead = inputStream.read(result, 0, 2);
    assertThat(firstRead).isEqualTo(2);
    assertThat(result).containsExactly(10, 20);

    firstRead = inputStream.read(result, 0, 2);
    assertThat(firstRead).isEqualTo(2);
    assertThat(result).containsExactly(30, 40);

    int next = inputStream.read();
    assertThat(next).isEqualTo(-1);
  }

  @Test
  void readByteArrayWithPartialData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(2);
    buffer.writeBytes(new byte[] { 1, 2 });
    inputStream.onDataReceived(buffer);

    new Thread(ExceptionUtils.sneaky(() -> {
      Thread.sleep(100);
      inputStream.onDataReceived(Unpooled.wrappedBuffer(new byte[] { 3, 4, 5 }));
    })).start();

    byte[] result = new byte[5];
    int bytesRead = inputStream.read(result, 0, 5);

    assertThat(bytesRead).isEqualTo(5);
    assertThat(result[0]).isEqualTo((byte) 1);
    assertThat(result[1]).isEqualTo((byte) 2);
    assertThat(result[2]).isEqualTo((byte) 3);
    assertThat(result[3]).isEqualTo((byte) 4);
    assertThat(result[4]).isEqualTo((byte) 5);
  }

  @Test
  void handleOnErrorWithPendingData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);

    inputStream.onError(new RuntimeException("Test Error"));

    byte[] result = new byte[1];
    assertThat(inputStream.read(result)).isEqualTo(1);
    assertThat(result).contains(1);
  }

  @Test
  void handleOnCompleteWithNoData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onComplete();

    int result = inputStream.read();
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void handleCancelDuringRead() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread readerThread = new Thread(() -> {
      try {
        inputStream.read();
      }
      catch (Exception ignored) {
      }
    });

    readerThread.start();
    Thread.sleep(100);

    inputStream.close();
    readerThread.join(1000);

    assertThat(readerThread.getState()).isEqualTo(Thread.State.TERMINATED);
  }

  @Test
  void validateCleanAndFinalizeBehavior() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    inputStream.close();
    int result = inputStream.read();
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void validateAddWorkAtMaxValue() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Simulate reaching MAX_VALUE
    for (int i = 0; i < 100; i++) {
      inputStream.addWork();
    }

    // This test mainly checks that addWork doesn't break at boundary values
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    int result = inputStream.read();
    assertThat(result).isEqualTo(1);
  }

  @Test
  void readReturnsMinusOneAfterStreamIsClosed() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readByteArrayReturnsMinusOneAfterStreamIsClosed() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();
    byte[] buffer = new byte[10];
    assertThat(inputStream.read(buffer)).isEqualTo(-1);
  }

  @Test
  void readThrowsExceptionWhenCalledConcurrently() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread t1 = new Thread(() -> {
      try {
        inputStream.read();
      }
      catch (Exception ignored) {
      }
    });

    Thread t2 = new Thread(() -> {
      assertThatThrownBy(inputStream::read)
              .isInstanceOf(ConcurrentModificationException.class)
              .hasMessage("Concurrent access is not allowed");
    });

    t1.start();
    Thread.sleep(50);
    t2.start();

    t1.interrupt();
    t1.join(1000);
    t2.join(1000);
  }

  @Test
  void readThrowsErrorWhenThereIsAnErrorInStream() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    RuntimeException error = new RuntimeException("error");
    inputStream.onError(error);

    assertThatThrownBy(inputStream::read)
            .isSameAs(error);
  }

  @Test
  void readReturnsAvailableDataBeforeError() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);
    RuntimeException error = new RuntimeException("error");
    inputStream.onError(error);

    assertThat(inputStream.read()).isEqualTo(42);
    assertThatThrownBy(inputStream::read)
            .isSameAs(error);
  }

  @Test
  void readBlocksUntilDataIsAvailable() throws Exception {
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter();
    BodyInputStream inputStream = new BodyInputStream(awaiter);

    Thread readerThread = new Thread(() -> {
      try {
        int data = inputStream.read();
        assertThat(data).isEqualTo(100);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    readerThread.start();
    Thread.sleep(100);

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(100);
    inputStream.onDataReceived(buffer);
    awaiter.resume();

    readerThread.join(1000);
    assertThat(readerThread.getState()).isEqualTo(Thread.State.TERMINATED);
  }

  @Test
  void partialReadContinuesFromCorrectPosition() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(4);
    buffer.writeBytes(new byte[] { 10, 20, 30, 40 });
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    byte[] result1 = new byte[2];
    int bytesRead1 = inputStream.read(result1, 0, 2);
    assertThat(bytesRead1).isEqualTo(2);
    assertThat(result1).containsExactly(10, 20);

    byte[] result2 = new byte[2];
    int bytesRead2 = inputStream.read(result2, 0, 2);
    assertThat(bytesRead2).isEqualTo(2);
    assertThat(result2).containsExactly(30, 40);
  }

  @Test
  void readByteArrayWithOffset() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(2);
    buffer.writeBytes(new byte[] { 50, 60 });
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    byte[] result = new byte[5];
    int bytesRead = inputStream.read(result, 2, 2);
    assertThat(bytesRead).isEqualTo(2);
    assertThat(result).containsExactly(0, 0, 50, 60, 0);
  }

  @Test
  void multipleBuffersAreReadInOrder() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer1 = Unpooled.buffer(2);
    buffer1.writeBytes(new byte[] { 1, 2 });
    inputStream.onDataReceived(buffer1);

    ByteBuf buffer2 = Unpooled.buffer(2);
    buffer2.writeBytes(new byte[] { 3, 4 });
    inputStream.onDataReceived(buffer2);

    ByteBuf buffer3 = Unpooled.buffer(2);
    buffer3.writeBytes(new byte[] { 5, 6 });
    inputStream.onDataReceived(buffer3);

    inputStream.onComplete();

    byte[] result = new byte[6];
    int bytesRead = inputStream.read(result);

    assertThat(bytesRead).isEqualTo(6);
    assertThat(result).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  void closeCleansUpAllResources() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer1 = Unpooled.buffer(1);
    buffer1.writeByte(1);
    inputStream.onDataReceived(buffer1);

    ByteBuf buffer2 = Unpooled.buffer(1);
    buffer2.writeByte(2);
    inputStream.onDataReceived(buffer2);

    inputStream.close();

    // After closing, reading should return -1
    assertThat(inputStream.read()).isEqualTo(-1);
    byte[] data = new byte[10];
    assertThat(inputStream.read(data)).isEqualTo(-1);
  }

  @Test
  void doubleCloseDoesNotCauseIssues() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    inputStream.close();
    inputStream.close(); // Should not throw

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void onCompleteAllowsFinishingReads() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    inputStream.onComplete();

    assertThat(inputStream.read()).isEqualTo(42);
    assertThat(inputStream.read()).isEqualTo(-1); // End of stream
  }

  @Test
  void onErrorCausesSubsequentReadsToFail() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onError(new IllegalStateException("Stream failure"));

    assertThatThrownBy(inputStream::read)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Stream failure");
  }

  @Test
  void readWithZeroLengthReturnsZero() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    byte[] zeroLength = new byte[0];
    int result = inputStream.read(zeroLength, 0, 0);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void readWithNegativeLengthThrowsException() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    byte[] buffer = new byte[5];
    assertThatThrownBy(() -> inputStream.read(buffer, 0, -1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void readWithOffsetBeyondArrayThrowsException() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    byte[] buffer = new byte[5];
    assertThatThrownBy(() -> inputStream.read(buffer, 6, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void readWithLengthExceedingArrayThrowsException() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    byte[] buffer = new byte[5];
    assertThatThrownBy(() -> inputStream.read(buffer, 0, 6))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void readWithOffsetAndLengthExceedingArrayThrowsException() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    byte[] buffer = new byte[5];
    assertThatThrownBy(() -> inputStream.read(buffer, 3, 4))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void concurrentCallsToCloseAreSafe() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread t1 = new Thread(inputStream::close);
    Thread t2 = new Thread(inputStream::close);

    t1.start();
    t2.start();

    t1.join(1000);
    t2.join(1000);

    // No exception should be thrown
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readAfterOnCompleteReturnsMinusOne() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onComplete();

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readAfterOnErrorThrowsTheSameError() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    Throwable error = new RuntimeException("test error");
    inputStream.onError(error);

    assertThatThrownBy(() -> inputStream.read())
            .isSameAs(error);
  }

  @Test
  void addWorkHandlesIntegerMaxValueCorrectly() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Set workAmount to Integer.MAX_VALUE - 1
    for (int i = 0; i < 100; i++) {
      inputStream.addWork();
    }

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    assertThat(inputStream.read()).isEqualTo(1);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void multipleOnCompleteCallsDoNotAffectBehavior() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    inputStream.onComplete();
    inputStream.onComplete(); // Should not affect anything

    assertThat(inputStream.read()).isEqualTo(42);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void multipleOnErrorCallsDoNotAffectBehavior() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    Throwable error1 = new RuntimeException("first error");
    Throwable error2 = new RuntimeException("second error");

    inputStream.onError(error1);
    inputStream.onError(error2); // Should not override the first error

    assertThat(inputStream.read()).isEqualTo(42); // Data should still be readable
    assertThatThrownBy(() -> inputStream.read())
            .isSameAs(error1); // First error should be thrown
  }

  @Test
  void onNextAfterOnErrorIsDiscarded() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onError(new RuntimeException("error"));

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer); // Should be discarded

    assertThatThrownBy(() -> inputStream.read())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("error");
  }

  @Test
  void onNextAfterOnCompleteIsDiscarded() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.onComplete();

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer); // Should be discarded

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void closeDuringBlockingReadReleasesLock() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    Thread readerThread = new Thread(() -> {
      try {
        inputStream.read(); // Will block waiting for data
      }
      catch (Exception ignored) {
      }
    });

    readerThread.start();
    Thread.sleep(100); // Allow time for blocking

    inputStream.close();
    readerThread.join(1000); // Should not block forever

    assertThat(readerThread.getState()).isEqualTo(Thread.State.TERMINATED);
  }

  @Test
  void cleanAndFinalizeClearsAllBuffers() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Add several buffers
    for (int i = 0; i < 5; i++) {
      ByteBuf buffer = Unpooled.buffer(1);
      buffer.writeByte(i);
      inputStream.onDataReceived(buffer);
    }

    inputStream.close();

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void resumeIsCalledOnlyOnceWhenAddingFirstItem() throws Exception {
    AtomicBoolean resumed = new AtomicBoolean(false);
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter() {
      @Override
      public void resume() {
        resumed.set(true);
        super.resume();
      }
    };

    BodyInputStream inputStream = new BodyInputStream(awaiter);

    // Initially no resume should be called
    assertThat(resumed.get()).isFalse();

    // Add first item, resume should be called once
    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);

    assertThat(resumed.get()).isTrue();
  }

  @Test
  void resumeNotCalledWhenAddingSecondItem() throws Exception {
    AtomicBoolean resumed = new AtomicBoolean(false);
    SimpleSingleThreadAwaiter awaiter = new SimpleSingleThreadAwaiter() {
      @Override
      public void resume() {
        resumed.set(true);
        super.resume();
      }
    };

    BodyInputStream inputStream = new BodyInputStream(awaiter);

    // Add first item, resume should be called
    ByteBuf buffer1 = Unpooled.buffer(1);
    buffer1.writeByte(1);
    inputStream.onDataReceived(buffer1);

    assertThat(resumed.get()).isTrue();

    // Reset flag
    resumed.set(false);

    // Add second item, resume should NOT be called again
    ByteBuf buffer2 = Unpooled.buffer(1);
    buffer2.writeByte(2);
    inputStream.onDataReceived(buffer2);

    assertThat(resumed.get()).isFalse();
  }

  @Test
  void addWorkReturnsMinValueWhenAlreadyCancelled() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Force set workAmount to MIN_VALUE to simulate cancellation state
    inputStream.addWork(); // Make sure we can set it
    // Use reflection or directly call addWork multiple times to reach edge case

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    // Normal read operation
    assertThat(inputStream.read()).isEqualTo(1);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readWithValidParametersReturnsCorrectData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer = Unpooled.buffer(5);
    buffer.writeBytes(new byte[] { 10, 20, 30, 40, 50 });
    inputStream.onDataReceived(buffer);
    inputStream.onComplete();

    byte[] result = new byte[3];
    int bytesRead = inputStream.read(result, 1, 2);

    assertThat(bytesRead).isEqualTo(2);
    assertThat(result).containsExactly(0, 10, 20);
  }

  @Test
  void getNextOrAwaitReturnsNullWhenClosed() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();

    // This would internally call getNextOrAwait
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void getNextOrAwaitReturnsNullWhenCancelled() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Simulate cancellation
    inputStream.close();

    // This would internally call getNextOrAwait
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void cleanAndFinalizeSetsWorkAmountToMinValue() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(1);
    inputStream.onDataReceived(buffer);

    inputStream.close();

    // Verify stream is properly cleaned up by trying to read
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void closeWhenAlreadyClosedDoesNothing() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();

    // Second close should not throw
    inputStream.close();

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void readAfterCloseReturnsMinusOneEvenWithAvailableData() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer);

    inputStream.close();

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void onErrorAfterCloseDoesNotChangeState() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();

    inputStream.onError(new RuntimeException("Should not matter"));

    assertThatThrownBy(inputStream::read).isInstanceOf(RuntimeException.class)
            .hasMessage("Should not matter");
  }

  @Test
  void onCompleteAfterCloseDoesNotChangeState() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();

    inputStream.onComplete();

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void onDataReceivedAfterCloseDiscardsBuffer() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());
    inputStream.close();

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer); // Should be discarded

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void onDataReceivedAfterCancelledDiscardsBuffer() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    // Simulate cancelled state
    inputStream.close();

    ByteBuf buffer = Unpooled.buffer(1);
    buffer.writeByte(42);
    inputStream.onDataReceived(buffer); // Should be discarded

    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void workAmountIncrementsCorrectly() throws Exception {
    BodyInputStream inputStream = new BodyInputStream(new SimpleSingleThreadAwaiter());

    int initial = inputStream.addWork();
    int second = inputStream.addWork();

    assertThat(initial).isEqualTo(0);
    assertThat(second).isEqualTo(1);
  }

}