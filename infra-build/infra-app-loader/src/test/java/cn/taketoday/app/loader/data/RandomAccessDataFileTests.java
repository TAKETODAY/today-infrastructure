/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.app.loader.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link RandomAccessDataFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class RandomAccessDataFileTests {

  private static final byte[] BYTES;

  static {
    BYTES = new byte[256];
    for (int i = 0; i < BYTES.length; i++) {
      BYTES[i] = (byte) i;
    }
  }

  private File tempFile;

  private RandomAccessDataFile file;

  private InputStream inputStream;

  @BeforeEach
  void setup(@TempDir File tempDir) throws Exception {
    this.tempFile = new File(tempDir, "tempFile");
    FileOutputStream outputStream = new FileOutputStream(this.tempFile);
    outputStream.write(BYTES);
    outputStream.close();
    this.file = new RandomAccessDataFile(this.tempFile);
    this.inputStream = this.file.getInputStream();
  }

  @AfterEach
  void cleanup() throws Exception {
    this.inputStream.close();
    this.file.close();
  }

  @Test
  void fileNotNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RandomAccessDataFile(null))
            .withMessageContaining("File must not be null");
  }

  @Test
  void fileExists() {
    File file = new File("/does/not/exist");
    assertThatIllegalArgumentException().isThrownBy(() -> new RandomAccessDataFile(file))
            .withMessageContaining(String.format("File %s must exist", file.getAbsolutePath()));
  }

  @Test
  void readWithOffsetAndLengthShouldRead() throws Exception {
    byte[] read = this.file.read(2, 3);
    assertThat(read).isEqualTo(new byte[] { 2, 3, 4 });
  }

  @Test
  void readWhenOffsetIsBeyondEOFShouldThrowException() {
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> this.file.read(257, 0));
  }

  @Test
  void readWhenOffsetIsBeyondEndOfSubsectionShouldThrowException() {
    RandomAccessData subsection = this.file.getSubsection(0, 10);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> subsection.read(11, 0));
  }

  @Test
  void readWhenOffsetPlusLengthGreaterThanEOFShouldThrowException() {
    assertThatExceptionOfType(EOFException.class).isThrownBy(() -> this.file.read(256, 1));
  }

  @Test
  void readWhenOffsetPlusLengthGreaterThanEndOfSubsectionShouldThrowException() {
    RandomAccessData subsection = this.file.getSubsection(0, 10);
    assertThatExceptionOfType(EOFException.class).isThrownBy(() -> subsection.read(10, 1));
  }

  @Test
  void inputStreamRead() throws Exception {
    for (int i = 0; i <= 255; i++) {
      assertThat(this.inputStream.read()).isEqualTo(i);
    }
  }

  @Test
  void inputStreamReadNullBytes() {
    assertThatNullPointerException().isThrownBy(() -> this.inputStream.read(null))
            .withMessage("Bytes must not be null");
  }

  @Test
  void inputStreamReadNullBytesWithOffset() {
    assertThatNullPointerException().isThrownBy(() -> this.inputStream.read(null, 0, 1))
            .withMessage("Bytes must not be null");
  }

  @Test
  void inputStreamReadBytes() throws Exception {
    byte[] b = new byte[256];
    int amountRead = this.inputStream.read(b);
    assertThat(b).isEqualTo(BYTES);
    assertThat(amountRead).isEqualTo(256);
  }

  @Test
  void inputStreamReadOffsetBytes() throws Exception {
    byte[] b = new byte[7];
    this.inputStream.skip(1);
    int amountRead = this.inputStream.read(b, 2, 3);
    assertThat(b).isEqualTo(new byte[] { 0, 0, 1, 2, 3, 0, 0 });
    assertThat(amountRead).isEqualTo(3);
  }

  @Test
  void inputStreamReadMoreBytesThanAvailable() throws Exception {
    byte[] b = new byte[257];
    int amountRead = this.inputStream.read(b);
    assertThat(b).startsWith(BYTES);
    assertThat(amountRead).isEqualTo(256);
  }

  @Test
  void inputStreamReadPastEnd() throws Exception {
    this.inputStream.skip(255);
    assertThat(this.inputStream.read()).isEqualTo(0xFF);
    assertThat(this.inputStream.read()).isEqualTo(-1);
    assertThat(this.inputStream.read()).isEqualTo(-1);
  }

  @Test
  void inputStreamReadZeroLength() throws Exception {
    byte[] b = new byte[] { 0x0F };
    int amountRead = this.inputStream.read(b, 0, 0);
    assertThat(b).isEqualTo(new byte[] { 0x0F });
    assertThat(amountRead).isZero();
    assertThat(this.inputStream.read()).isZero();
  }

  @Test
  void inputStreamSkip() throws Exception {
    long amountSkipped = this.inputStream.skip(4);
    assertThat(this.inputStream.read()).isEqualTo(4);
    assertThat(amountSkipped).isEqualTo(4L);
  }

  @Test
  void inputStreamSkipMoreThanAvailable() throws Exception {
    long amountSkipped = this.inputStream.skip(257);
    assertThat(this.inputStream.read()).isEqualTo(-1);
    assertThat(amountSkipped).isEqualTo(256L);
  }

  @Test
  void inputStreamSkipPastEnd() throws Exception {
    this.inputStream.skip(256);
    long amountSkipped = this.inputStream.skip(1);
    assertThat(amountSkipped).isZero();
  }

  @Test
  void inputStreamAvailable() throws Exception {
    assertThat(this.inputStream.available()).isEqualTo(256);
    this.inputStream.skip(56);
    assertThat(this.inputStream.available()).isEqualTo(200);
    this.inputStream.skip(200);
    assertThat(this.inputStream.available()).isZero();
  }

  @Test
  void subsectionNegativeOffset() {
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> this.file.getSubsection(-1, 1));
  }

  @Test
  void subsectionNegativeLength() {
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> this.file.getSubsection(0, -1));
  }

  @Test
  void subsectionZeroLength() throws Exception {
    RandomAccessData subsection = this.file.getSubsection(0, 0);
    assertThat(subsection.getInputStream().read()).isEqualTo(-1);
  }

  @Test
  void subsectionTooBig() {
    this.file.getSubsection(0, 256);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> this.file.getSubsection(0, 257));
  }

  @Test
  void subsectionTooBigWithOffset() {
    this.file.getSubsection(1, 255);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> this.file.getSubsection(1, 256));
  }

  @Test
  void subsection() throws Exception {
    RandomAccessData subsection = this.file.getSubsection(1, 1);
    assertThat(subsection.getInputStream().read()).isOne();
  }

  @Test
  void inputStreamReadPastSubsection() throws Exception {
    RandomAccessData subsection = this.file.getSubsection(1, 2);
    InputStream inputStream = subsection.getInputStream();
    assertThat(inputStream.read()).isOne();
    assertThat(inputStream.read()).isEqualTo(2);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void inputStreamReadBytesPastSubsection() throws Exception {
    RandomAccessData subsection = this.file.getSubsection(1, 2);
    InputStream inputStream = subsection.getInputStream();
    byte[] b = new byte[3];
    int amountRead = inputStream.read(b);
    assertThat(b).isEqualTo(new byte[] { 1, 2, 0 });
    assertThat(amountRead).isEqualTo(2);
  }

  @Test
  void inputStreamSkipPastSubsection() throws Exception {
    RandomAccessData subsection = this.file.getSubsection(1, 2);
    InputStream inputStream = subsection.getInputStream();
    assertThat(inputStream.skip(3)).isEqualTo(2L);
    assertThat(inputStream.read()).isEqualTo(-1);
  }

  @Test
  void inputStreamSkipNegative() throws Exception {
    assertThat(this.inputStream.skip(-1)).isZero();
  }

  @Test
  void getFile() {
    assertThat(this.file.getFile()).isEqualTo(this.tempFile);
  }

  @Test
  void concurrentReads() throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(20);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      results.add(executorService.submit(() -> {
        InputStream subsectionInputStream = RandomAccessDataFileTests.this.file.getSubsection(0, 256)
                .getInputStream();
        byte[] b = new byte[256];
        subsectionInputStream.read(b);
        return Arrays.equals(b, BYTES);
      }));
    }
    for (Future<Boolean> future : results) {
      assertThat(future.get()).isTrue();
    }
  }

}
