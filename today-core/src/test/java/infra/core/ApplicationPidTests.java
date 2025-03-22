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

package infra.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

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
}