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

package cn.taketoday.app.loader.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.ref.Cleaner;
import cn.taketoday.app.loader.testsupport.TestJar;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.app.loader.zip.FileChannelDataBlockManagedFileChannel;
import cn.taketoday.app.loader.zip.ZipContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NestedByteChannel}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class NestedByteChannelTests {

  @TempDir
  File temp;

  private File file;

  private NestedByteChannel channel;

  @BeforeEach
  void setup() throws Exception {
    this.file = new File(this.temp, "test.jar");
    TestJar.create(this.file);
    this.channel = new NestedByteChannel(this.file.toPath(), "nested.jar");
  }

  @AfterEach
  void cleanup() throws Exception {
    this.channel.close();
  }

  @Test
  void isOpenWhenOpenReturnsTrue() {
    assertThat(this.channel.isOpen()).isTrue();
  }

  @Test
  void isOpenWhenClosedReturnsFalse() throws Exception {
    this.channel.close();
    assertThat(this.channel.isOpen()).isFalse();
  }

  @Test
  void closeCleansResources() throws Exception {
    Cleaner cleaner = mock(Cleaner.class);
    Cleanable cleanable = mock(Cleanable.class);
    given(cleaner.register(any(), any())).willReturn(cleanable);
    NestedByteChannel channel = new NestedByteChannel(this.file.toPath(), "nested.jar", cleaner);
    channel.close();
    then(cleanable).should().clean();
    ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
    then(cleaner).should().register(any(), actionCaptor.capture());
    actionCaptor.getValue().run();
  }

  @Test
  void closeWhenAlreadyClosedDoesNothing() throws IOException {
    Cleaner cleaner = mock(Cleaner.class);
    Cleanable cleanable = mock(Cleanable.class);
    given(cleaner.register(any(), any())).willReturn(cleanable);
    NestedByteChannel channel = new NestedByteChannel(this.file.toPath(), "nested.jar", cleaner);
    channel.close();
    then(cleanable).should().clean();
    ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
    then(cleaner).should().register(any(), actionCaptor.capture());
    actionCaptor.getValue().run();
    channel.close();
    then(cleaner).shouldHaveNoMoreInteractions();
  }

  @Test
  void readReadsBytesAndIncrementsPosition() throws IOException {
    ByteBuffer dst = ByteBuffer.allocate(10);
    assertThat(this.channel.position()).isZero();
    this.channel.read(dst);
    assertThat(this.channel.position()).isEqualTo(10L);
    assertThat(dst.array()).isNotEqualTo(ByteBuffer.allocate(10).array());
  }

  @Test
    // gh-38592
  void readReadsAsManyBytesAsPossible() throws Exception {
    // ZipFileSystem checks that the number of bytes read matches an expected value
    // ...if (readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR)
    // but the readFullyAt assumes that all remaining bytes are attempted to be read
    // This doesn't seem to exactly match the contract of ReadableByteChannel.read
    // which states "A read operation might not fill the buffer, and in fact it might
    // not read any bytes at all", but we need to match ZipFileSystem's expectations
    int size = FileChannelDataBlockManagedFileChannel.BUFFER_SIZE * 2;
    byte[] data = new byte[size];
    this.file = new File(this.temp, "testread.jar");
    FileOutputStream fileOutputStream = new FileOutputStream(this.file);
    try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
      JarEntry nestedEntry = new JarEntry("data");
      nestedEntry.setSize(size);
      nestedEntry.setCompressedSize(size);
      CRC32 crc32 = new CRC32();
      crc32.update(data);
      nestedEntry.setCrc(crc32.getValue());
      nestedEntry.setMethod(ZipEntry.STORED);
      jarOutputStream.putNextEntry(nestedEntry);
      jarOutputStream.write(data);
      jarOutputStream.closeEntry();
    }
    this.channel = new NestedByteChannel(this.file.toPath(), null);
    ByteBuffer buffer = ByteBuffer.allocate((int) this.file.length());
    assertThat(this.channel.read(buffer)).isEqualTo(buffer.capacity());
    assertThat(this.file).binaryContent().isEqualTo(buffer.array());
  }

  @Test
  void writeThrowsException() {
    assertThatExceptionOfType(NonWritableChannelException.class)
            .isThrownBy(() -> this.channel.write(ByteBuffer.allocate(10)));
  }

  @Test
  void positionWhenClosedThrowsException() throws Exception {
    this.channel.close();
    assertThatExceptionOfType(ClosedChannelException.class).isThrownBy(() -> this.channel.position());
  }

  @Test
  void positionWhenOpenReturnsPosition() throws Exception {
    assertThat(this.channel.position()).isEqualTo(0L);
  }

  @Test
  void positionWithLongWhenClosedThrowsException() throws Exception {
    this.channel.close();
    assertThatExceptionOfType(ClosedChannelException.class).isThrownBy(() -> this.channel.position(0L));
  }

  @Test
  void positionWithLongWhenLessThanZeroThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.channel.position(-1));
  }

  @Test
  void positionWithLongWhenEqualToSizeThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.channel.position(this.channel.size()));
  }

  @Test
  void positionWithLongWhenOpenUpdatesPosition() throws Exception {
    ByteBuffer dst1 = ByteBuffer.allocate(10);
    ByteBuffer dst2 = ByteBuffer.allocate(10);
    dst2.position(1);
    this.channel.read(dst1);
    this.channel.position(1);
    this.channel.read(dst2);
    dst2.array()[0] = dst1.array()[0];
    assertThat(dst1.array()).isEqualTo(dst2.array());
  }

  @Test
  void sizeWhenClosedThrowsException() throws Exception {
    this.channel.close();
    assertThatExceptionOfType(ClosedChannelException.class).isThrownBy(() -> this.channel.size());
  }

  @Test
  void sizeWhenOpenReturnsSize() throws IOException {
    try (ZipContent content = ZipContent.open(this.file.toPath())) {
      assertThat(this.channel.size()).isEqualTo(content.getEntry("nested.jar").getUncompressedSize());
    }
  }

}
