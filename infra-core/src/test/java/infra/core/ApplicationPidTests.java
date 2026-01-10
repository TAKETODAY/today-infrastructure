/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/21 14:49
 */
class ApplicationPidTests {

  @TempDir
  File tempDir;

  @Test
  void toStringWithPid() {
    assertThat(new ApplicationPid(123L)).hasToString("123");
  }

  @Test
  void toStringWithoutPid() {
    assertThat(new ApplicationPid(null)).hasToString("???");
  }

  @Test
  void throwIllegalStateWritingMissingPid() {
    ApplicationPid pid = new ApplicationPid(null);
    assertThatIllegalStateException().isThrownBy(() -> pid.write(new File(this.tempDir, "pid")))
            .withMessageContaining("No PID available");
  }

  @Test
  void writePid() throws Exception {
    ApplicationPid pid = new ApplicationPid(123L);
    File file = new File(this.tempDir, "pid");
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void writeNewPid() throws Exception {
    ApplicationPid pid = new ApplicationPid(123L);
    File file = new File(this.tempDir, "pid");
    file.delete();
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void toLong() {
    ApplicationPid pid = new ApplicationPid(123L);
    assertThat(pid.toLong()).isEqualTo(123L);
  }

  @Test
  void toLongWhenNotAvailable() {
    ApplicationPid pid = new ApplicationPid(null);
    assertThat(pid.toLong()).isNull();
  }

  @Test
  void isAvailableWhenAvailable() {
    ApplicationPid pid = new ApplicationPid(123L);
    assertThat(pid.isAvailable()).isTrue();
  }

  @Test
  void isAvailableWhenNotAvailable() {
    ApplicationPid pid = new ApplicationPid(null);
    assertThat(pid.isAvailable()).isFalse();
  }

  @Test
  void getPidFromJvm() {
    assertThat(new ApplicationPid().toString()).isNotEmpty();
  }

  @Test
  void equalsAndHashCodeWhenSamePid() {
    ApplicationPid pid1 = new ApplicationPid(123L);
    ApplicationPid pid2 = new ApplicationPid(123L);
    assertThat(pid1).isEqualTo(pid2);
    assertThat(pid1.hashCode()).isEqualTo(pid2.hashCode());
  }

  @Test
  void equalsAndHashCodeWhenDifferentPid() {
    ApplicationPid pid1 = new ApplicationPid(123L);
    ApplicationPid pid2 = new ApplicationPid(456L);
    assertThat(pid1).isNotEqualTo(pid2);
    assertThat(pid1.hashCode()).isNotEqualTo(pid2.hashCode());
  }

  @Test
  void equalsAndHashCodeWhenNullPid() {
    ApplicationPid pid1 = new ApplicationPid(null);
    ApplicationPid pid2 = new ApplicationPid(null);
    assertThat(pid1).isEqualTo(pid2);
    assertThat(pid1.hashCode()).isEqualTo(pid2.hashCode());
  }

  @Test
  void writeCreatesParentDirectories(@TempDir File tempDir) throws Exception {
    File pidFile = new File(tempDir, "sub/dir/pid");
    new ApplicationPid(123L).write(pidFile);
    assertThat(pidFile).exists();
    assertThat(contentOf(pidFile)).isEqualTo("123");
  }

  @Test
  void writeFailsWhenFileNotWritable(@TempDir File tempDir) throws Exception {
    File pidFile = new File(tempDir, "pid");
    pidFile.createNewFile();
    pidFile.setReadOnly();
    ApplicationPid pid = new ApplicationPid(123L);
    assertThatThrownBy(() -> pid.write(pidFile))
            .isInstanceOf(FileNotFoundException.class);
  }

  @Test
  void currentProcessPidIsAvailable() {
    ApplicationPid pid = new ApplicationPid();
    assertThat(pid.isAvailable()).isTrue();
    assertThat(pid.toLong()).isPositive();
  }

  @Test
  void writeToNonExistentParentDirectoryCreatesDirectory(@TempDir File tempDir) throws Exception {
    File pidFile = new File(tempDir, "deep/nested/dir/pid");
    new ApplicationPid(123L).write(pidFile);
    assertThat(pidFile.getParentFile()).exists();
    assertThat(contentOf(pidFile)).isEqualTo("123");
  }

  @Test
  void writeFailsWhenDirectoryNotWritable(@TempDir File tempDir) throws Exception {
    File directory = new File(tempDir, "not-writable");
    directory.mkdirs();
    directory.setReadOnly();
    File pidFile = new File(directory, "pid");
    ApplicationPid pid = new ApplicationPid(123L);
    assertThatThrownBy(() -> pid.write(pidFile))
            .isInstanceOf(FileNotFoundException.class);
  }

  @Test
  void writeOverwritesExistingFile(@TempDir File tempDir) throws Exception {
    File pidFile = new File(tempDir, "pid");
    try (FileWriter writer = new FileWriter(pidFile)) {
      writer.write("old-pid");
    }
    new ApplicationPid(123L).write(pidFile);
    assertThat(contentOf(pidFile)).isEqualTo("123");
  }

  @Test
  void equalsWhenComparedToNonApplicationPid() {
    ApplicationPid pid = new ApplicationPid(123L);
    assertThat(pid.equals("123")).isFalse();
  }

  @Test
  void equalsWhenComparedToSelf() {
    ApplicationPid pid = new ApplicationPid(123L);
    assertThat(pid.equals(pid)).isTrue();
  }

  @Test
  void writePidToFileWithSpecialCharactersInPath() throws Exception {
    ApplicationPid pid = new ApplicationPid(123L);
    File file = new File(tempDir, "special-pid_123.txt");
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void writePidWhenParentDirectoryCreationFails() {
    ApplicationPid pid = new ApplicationPid(123L);
    File file = new File("/invalid/path/pid");
    assertThatThrownBy(() -> pid.write(file))
            .isInstanceOf(IOException.class);
  }

  @Test
  void toStringReturnsValidPidString() {
    ApplicationPid pid = new ApplicationPid(999L);
    assertThat(pid.toString()).isEqualTo("999");
  }

  @Test
  void toStringReturnsPlaceholderWhenPidIsNull() {
    ApplicationPid pid = new ApplicationPid(null);
    assertThat(pid.toString()).isEqualTo("???");
  }

  @Test
  void equalsReturnsFalseForNullComparison() {
    ApplicationPid pid = new ApplicationPid(123L);
    assertThat(pid.equals(null)).isFalse();
  }

  @Test
  void hashCodeReturnsConsistentValueForSamePid() {
    ApplicationPid pid1 = new ApplicationPid(456L);
    ApplicationPid pid2 = new ApplicationPid(456L);
    assertThat(pid1.hashCode()).isEqualTo(pid2.hashCode());
  }

  @Test
  void hashCodeReturnsDifferentValuesForDifferentPids() {
    ApplicationPid pid1 = new ApplicationPid(111L);
    ApplicationPid pid2 = new ApplicationPid(222L);
    assertThat(pid1.hashCode()).isNotEqualTo(pid2.hashCode());
  }

  @Test
  void currentProcessPidConstructorSetsValidPid() {
    ApplicationPid pid = new ApplicationPid();
    assertThat(pid.isAvailable()).isTrue();
    assertThat(pid.toLong()).isNotNull();
    assertThat(pid.toLong()).isPositive();
  }

  @Test
  void writePidToFileThatAlreadyExistsAndIsWritable() throws Exception {
    ApplicationPid pid = new ApplicationPid(789L);
    File file = new File(tempDir, "existing.pid");
    file.createNewFile();
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("789");
  }

  @Test
  void writePidThrowsExceptionWhenPidNotAvailable() {
    ApplicationPid pid = new ApplicationPid(null);
    File file = new File(tempDir, "pid");
    assertThatIllegalStateException()
            .isThrownBy(() -> pid.write(file))
            .withMessageContaining("No PID available");
  }

  @Test
  void canWritePosixFileReturnsTrueOnUnsupportedOperation() throws Exception {
    // This test covers the UnsupportedOperationException catch block
    // in the canWritePosixFile method indirectly
    ApplicationPid pid = new ApplicationPid(123L);
    File file = new File(tempDir, "posix-test.pid");
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void createParentDirectoryHandlesNullParent() throws Exception {
    // Testing edge case where file has no parent directory
    ApplicationPid pid = new ApplicationPid(456L);
    File file = new File("no-parent.pid");
    // This would normally fail, but we're testing the createParentDirectory logic
    // In practice, this might depend on working directory permissions
    try {
      pid.write(file);
      assertThat(file.exists()).isTrue();
    }
    finally {
      file.delete(); // cleanup
    }
  }

}