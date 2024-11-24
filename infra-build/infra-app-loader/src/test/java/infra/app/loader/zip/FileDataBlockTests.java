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

package infra.app.loader.zip;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileDataBlock}.
 *
 * @author Phillip Webb
 */
class FileDataBlockTests {

  private static final byte[] CONTENT = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

  @TempDir
  File tempDir;

  File tempFile;

  @BeforeEach
  void writeTempFile() throws IOException {
    this.tempFile = new File(this.tempDir, "content");
    Files.write(this.tempFile.toPath(), CONTENT);
  }

  @AfterEach
  void resetTracker() {
    FileDataBlock.tracker = FileDataBlock.Tracker.NONE;
  }

  @Test
  void sizeReturnsFileSize() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      assertThat(block.size()).isEqualTo(CONTENT.length);
    }
  }

  @Test
  void readReadsFile() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      assertThat(block.read(buffer, 0)).isEqualTo(6);
      assertThat(buffer.array()).containsExactly(CONTENT);
    }
  }

  @Test
  void readReadsFileWhenThreadHasBeenInterrupted() throws IOException {
    Files.write(this.tempFile.toPath(), CONTENT);
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      Thread.currentThread().interrupt();
      assertThat(block.read(buffer, 0)).isEqualTo(6);
      assertThat(buffer.array()).containsExactly(CONTENT);
    }
    finally {
      Thread.interrupted();
    }
  }

  @Test
  void readDoesNotReadPastEndOfFile() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      assertThat(block.read(buffer, 2)).isEqualTo(4);
      assertThat(buffer.array()).containsExactly(0x02, 0x03, 0x04, 0x05, 0x0, 0x0);
    }
  }

  @Test
  void readWhenPosAtSizeReturnsMinusOne() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      assertThat(block.read(buffer, 6)).isEqualTo(-1);
    }
  }

  @Test
  void readWhenPosOverSizeReturnsMinusOne() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      assertThat(block.read(buffer, 7)).isEqualTo(-1);
    }
  }

  @Test
  void readWhenPosIsNegativeThrowsException() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
      assertThatIllegalArgumentException().isThrownBy(() -> block.read(buffer, -1));
    }
  }

  @Test
  void sliceWhenOffsetIsNegativeThrowsException() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      assertThatIllegalArgumentException().isThrownBy(() -> block.slice(-1, 0))
              .withMessage("Offset must not be negative");
    }
  }

  @Test
  void sliceWhenSizeIsNegativeThrowsException() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      assertThatIllegalArgumentException().isThrownBy(() -> block.slice(0, -1))
              .withMessage("Size must not be negative and must be within bounds");
    }
  }

  @Test
  void sliceWhenSizeIsOutOfBoundsThrowsException() throws IOException {
    try (FileDataBlock block = createAndOpenBlock()) {
      assertThatIllegalArgumentException().isThrownBy(() -> block.slice(2, 5))
              .withMessage("Size must not be negative and must be within bounds");
    }
  }

  @Test
  void sliceReturnsSlice() throws IOException {
    try (FileDataBlock slice = createAndOpenBlock().slice(1, 4)) {
      assertThat(slice.size()).isEqualTo(4);
      ByteBuffer buffer = ByteBuffer.allocate(4);
      assertThat(slice.read(buffer, 0)).isEqualTo(4);
      assertThat(buffer.array()).containsExactly(0x01, 0x02, 0x03, 0x04);
    }
  }

  @Test
  void openAndCloseHandleReferenceCounting() throws IOException {
    TestTracker tracker = new TestTracker();
    FileDataBlock.tracker = tracker;
    FileDataBlock block = createBlock();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(0, 0);
    block.open();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(1, 0);
    block.open();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(2);
    tracker.assertOpenCloseCounts(1, 0);
    block.close();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(1, 0);
    block.close();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(1, 1);
    block.open();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(2, 1);
    block.close();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(2, 2);
  }

  @Test
  void openAndCloseSliceHandleReferenceCounting() throws IOException {
    TestTracker tracker = new TestTracker();
    FileDataBlock.tracker = tracker;
    FileDataBlock block = createBlock();
    FileDataBlock slice = block.slice(1, 4);
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(0, 0);
    block.open();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(1, 0);
    slice.open();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(2);
    tracker.assertOpenCloseCounts(1, 0);
    slice.open();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(3);
    tracker.assertOpenCloseCounts(1, 0);
    slice.close();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(2);
    tracker.assertOpenCloseCounts(1, 0);
    slice.close();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(1, 0);
    block.close();
    assertThat(block).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(1, 1);
    slice.open();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(1);
    tracker.assertOpenCloseCounts(2, 1);
    slice.close();
    assertThat(slice).extracting("fileAccess.referenceCount").isEqualTo(0);
    tracker.assertOpenCloseCounts(2, 2);
  }

  private FileDataBlock createAndOpenBlock() throws IOException {
    FileDataBlock block = createBlock();
    block.open();
    return block;
  }

  private FileDataBlock createBlock() throws IOException {
    return new FileDataBlock(this.tempFile.toPath());
  }

  static class TestTracker implements FileDataBlock.Tracker {

    private int openCount;

    private int closeCount;

    @Override
    public void openedFileChannel(Path path) {
      this.openCount++;
    }

    @Override
    public void closedFileChannel(Path path) {
      this.closeCount++;
    }

    void assertOpenCloseCounts(int expectedOpenCount, int expectedCloseCount) {
      assertThat(this.openCount).as("openCount").isEqualTo(expectedOpenCount);
      assertThat(this.closeCount).as("closeCount").isEqualTo(expectedCloseCount);
    }

  }

}
