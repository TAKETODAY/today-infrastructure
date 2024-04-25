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

package cn.taketoday.annotation.config.ssl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/22 12:44
 */
class FileWatcherTests {

  private FileWatcher fileWatcher;

  @BeforeEach
  void setUp() {
    this.fileWatcher = new FileWatcher(Duration.ofMillis(10));
  }

  @AfterEach
  void tearDown() throws IOException {
    this.fileWatcher.destroy();
  }

  @Test
  void shouldTriggerOnFileCreation(@TempDir Path tempDir) throws Exception {
    Path newFile = tempDir.resolve("new-file.txt");
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(tempDir), callback);
    Files.createFile(newFile);
    callback.expectChanges();
  }

  @Test
  void shouldTriggerOnFileDeletion(@TempDir Path tempDir) throws Exception {
    Path deletedFile = tempDir.resolve("deleted-file.txt");
    Files.createFile(deletedFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(tempDir), callback);
    Files.delete(deletedFile);
    callback.expectChanges();
  }

  @Test
  void shouldTriggerOnFileModification(@TempDir Path tempDir) throws Exception {
    Path deletedFile = tempDir.resolve("modified-file.txt");
    Files.createFile(deletedFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(tempDir), callback);
    Files.writeString(deletedFile, "Some content");
    callback.expectChanges();
  }

  @Test
  void shouldWatchFile(@TempDir Path tempDir) throws Exception {
    Path watchedFile = tempDir.resolve("watched.txt");
    Files.createFile(watchedFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(watchedFile), callback);
    Files.writeString(watchedFile, "Some content");
    callback.expectChanges();
  }

  @Test
  void shouldIgnoreNotWatchedFiles(@TempDir Path tempDir) throws Exception {
    Path watchedFile = tempDir.resolve("watched.txt");
    Path notWatchedFile = tempDir.resolve("not-watched.txt");
    Files.createFile(watchedFile);
    Files.createFile(notWatchedFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(watchedFile), callback);
    Files.writeString(notWatchedFile, "Some content");
    callback.expectNoChanges();
  }

  @Test
  void shouldFailIfDirectoryOrFileDoesNotExist(@TempDir Path tempDir) {
    Path directory = tempDir.resolve("dir1");
    assertThatExceptionOfType(UncheckedIOException.class)
            .isThrownBy(() -> this.fileWatcher.watch(Set.of(directory), new WaitingCallback()))
            .withMessage("Failed to register paths for watching: [%s]".formatted(directory));
  }

  @Test
  void shouldNotFailIfDirectoryIsRegisteredMultipleTimes(@TempDir Path tempDir) {
    WaitingCallback callback = new WaitingCallback();
    assertThatCode(() -> {
      this.fileWatcher.watch(Set.of(tempDir), callback);
      this.fileWatcher.watch(Set.of(tempDir), callback);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldNotFailIfStoppedMultipleTimes(@TempDir Path tempDir) {
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(tempDir), callback);
    assertThatCode(() -> {
      this.fileWatcher.destroy();
      this.fileWatcher.destroy();
    }).doesNotThrowAnyException();
  }

  @Test
  void testRelativeFiles() throws Exception {
    Path watchedFile = Path.of(UUID.randomUUID() + ".txt");
    Files.createFile(watchedFile);
    try {
      WaitingCallback callback = new WaitingCallback();
      this.fileWatcher.watch(Set.of(watchedFile), callback);
      Files.delete(watchedFile);
      callback.expectChanges();
    }
    finally {
      Files.deleteIfExists(watchedFile);
    }
  }

  @Test
  void testRelativeDirectories() throws Exception {
    Path watchedDirectory = Path.of(UUID.randomUUID() + "/");
    Path file = watchedDirectory.resolve("file.txt");
    Files.createDirectory(watchedDirectory);
    try {
      WaitingCallback callback = new WaitingCallback();
      this.fileWatcher.watch(Set.of(watchedDirectory), callback);
      Files.createFile(file);
      callback.expectChanges();
    }
    finally {
      Files.deleteIfExists(file);
      Files.deleteIfExists(watchedDirectory);
    }
  }

  private static final class WaitingCallback implements Runnable {

    private final CountDownLatch latch = new CountDownLatch(1);

    volatile boolean changed = false;

    @Override
    public void run() {
      this.changed = true;
      this.latch.countDown();
    }

    void expectChanges() throws InterruptedException {
      waitForChanges(true);
      assertThat(this.changed).as("changed").isTrue();
    }

    void expectNoChanges() throws InterruptedException {
      waitForChanges(false);
      assertThat(this.changed).as("changed").isFalse();
    }

    void waitForChanges(boolean fail) throws InterruptedException {
      if (!this.latch.await(5, TimeUnit.SECONDS)) {
        if (fail) {
          fail("Timeout while waiting for changes");
        }
      }
    }

  }

}