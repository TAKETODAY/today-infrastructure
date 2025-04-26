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

package infra.annotation.config.ssl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.util.FileSystemUtils;

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
  void shouldFollowSymlink(@TempDir Path tempDir) throws Exception {
    Path realFile = tempDir.resolve("realFile.txt");
    Path symLink = tempDir.resolve("symlink.txt");
    Files.createFile(realFile);
    Files.createSymbolicLink(symLink, realFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(symLink), callback);
    Files.writeString(realFile, "Some content");
    callback.expectChanges();
  }

  @Test
  void shouldFollowSymlinkRecursively(@TempDir Path tempDir) throws Exception {
    Path realFile = tempDir.resolve("realFile.txt");
    Path symLink = tempDir.resolve("symlink.txt");
    Path symLink2 = tempDir.resolve("symlink2.txt");
    Files.createFile(realFile);
    Files.createSymbolicLink(symLink, symLink2);
    Files.createSymbolicLink(symLink2, realFile);
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(symLink), callback);
    Files.writeString(realFile, "Some content");
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

  /*
   * Replicating a letsencrypt folder structure like:
   * "/folder/live/certname/privkey.pem -> ../../archive/certname/privkey32.pem"
   */
  @Test
  void shouldFollowRelativePathSymlinks(@TempDir Path tempDir) throws Exception {
    Path folder = tempDir.resolve("folder");
    Path live = folder.resolve("live").resolve("certname");
    Path archive = folder.resolve("archive").resolve("certname");
    Path link = live.resolve("privkey.pem");
    Path targetFile = archive.resolve("privkey32.pem");
    Files.createDirectories(live);
    Files.createDirectories(archive);
    Files.createFile(targetFile);
    Path relativePath = Path.of("../../archive/certname/privkey32.pem");
    Files.createSymbolicLink(link, relativePath);
    try {
      WaitingCallback callback = new WaitingCallback();
      this.fileWatcher.watch(Set.of(link), callback);
      Files.writeString(targetFile, "Some content");
      callback.expectChanges();
    }
    finally {
      FileSystemUtils.deleteRecursively(folder);
    }
  }

  /*
   * Replicating a k8s configmap folder structure like:
   * "secret.txt -> ..data/secret.txt",
   * "..data/ -> ..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f/",
   * "..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f/secret.txt"
   *
   * After a secret update, this will look like: "secret.txt -> ..data/secret.txt",
   * "..data/ -> ..bba2a61f-ce04-4c35-93aa-e455110d4487/",
   * "..bba2a61f-ce04-4c35-93aa-e455110d4487/secret.txt"
   */
  @Test
  void shouldTriggerOnConfigMapUpdates(@TempDir Path tempDir) throws Exception {
    Path configMap1 = createConfigMap(tempDir, "secret.txt");
    Path configMap2 = createConfigMap(tempDir, "secret.txt");
    Path data = tempDir.resolve("..data");
    Files.createSymbolicLink(data, configMap1);
    Path secretFile = tempDir.resolve("secret.txt");
    Files.createSymbolicLink(secretFile, data.resolve("secret.txt"));
    try {
      WaitingCallback callback = new WaitingCallback();
      this.fileWatcher.watch(Set.of(secretFile), callback);
      Files.delete(data);
      Files.createSymbolicLink(data, configMap2);
      FileSystemUtils.deleteRecursively(configMap1);
      callback.expectChanges();
    }
    finally {
      FileSystemUtils.deleteRecursively(configMap2);
      Files.delete(data);
      Files.delete(secretFile);
    }
  }

  /**
   * Updates many times K8s ConfigMap/Secret with atomic move. <pre>
   * .
   * ├── ..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f
   * │   ├── keystore.jks
   * ├── ..data -> ..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f
   * ├── keystore.jks -> ..data/keystore.jks
   * </pre>
   *
   * After a first a ConfigMap/Secret update, this will look like: <pre>
   * .
   * ├── ..bba2a61f-ce04-4c35-93aa-e455110d4487
   * │   ├── keystore.jks
   * ├── ..data -> ..bba2a61f-ce04-4c35-93aa-e455110d4487
   * ├── keystore.jks -> ..data/keystore.jks
   * </pre> After a second a ConfigMap/Secret update, this will look like: <pre>
   * .
   * ├── ..134887f0-df8f-4433-b70c-7784d2a33bd1
   * │   ├── keystore.jks
   * ├── ..data -> ..134887f0-df8f-4433-b70c-7784d2a33bd1
   * ├── keystore.jks -> ..data/keystore.jks
   * </pre>
   * <p>
   * When Kubernetes updates either the ConfigMap or Secret, it performs the following
   * steps:
   * <ul>
   * <li>Creates a new unique directory.</li>
   * <li>Writes the ConfigMap/Secret content to the newly created directory.</li>
   * <li>Creates a symlink {@code ..data_tmp} pointing to the newly created
   * directory.</li>
   * <li>Performs an atomic rename of {@code ..data_tmp} to {@code ..data}.</li>
   * <li>Deletes the old ConfigMap/Secret directory.</li>
   * </ul>
   *
   * @param tempDir temp directory
   * @throws Exception if a failure occurs
   */
  @Test
  void shouldTriggerOnConfigMapAtomicMoveUpdates(@TempDir Path tempDir) throws Exception {
    Path configMap1 = createConfigMap(tempDir, "keystore.jks");
    Path data = Files.createSymbolicLink(tempDir.resolve("..data"), configMap1);
    Files.createSymbolicLink(tempDir.resolve("keystore.jks"), data.resolve("keystore.jks"));
    WaitingCallback callback = new WaitingCallback();
    this.fileWatcher.watch(Set.of(tempDir.resolve("keystore.jks")), callback);
    // First update
    Path configMap2 = createConfigMap(tempDir, "keystore.jks");
    Path dataTmp = Files.createSymbolicLink(tempDir.resolve("..data_tmp"), configMap2);
    move(dataTmp, data);
    FileSystemUtils.deleteRecursively(configMap1);
    callback.expectChanges();
    callback.reset();
    // Second update
    Path configMap3 = createConfigMap(tempDir, "keystore.jks");
    dataTmp = Files.createSymbolicLink(tempDir.resolve("..data_tmp"), configMap3);
    move(dataTmp, data);
    FileSystemUtils.deleteRecursively(configMap2);
    callback.expectChanges();
  }

  Path createConfigMap(Path parentDir, String secretFileName) throws IOException {
    Path configMapFolder = parentDir.resolve(".." + UUID.randomUUID());
    Files.createDirectory(configMapFolder);
    Path secret = configMapFolder.resolve(secretFileName);
    Files.createFile(secret);
    return configMapFolder;
  }

  private void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (AccessDeniedException ex) {
      // Windows
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static final class WaitingCallback implements Runnable {

    private CountDownLatch latch = new CountDownLatch(1);

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

    void reset() {
      this.latch = new CountDownLatch(1);
      this.changed = false;
    }

  }

}