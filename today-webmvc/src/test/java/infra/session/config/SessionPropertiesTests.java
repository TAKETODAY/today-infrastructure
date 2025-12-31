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

package infra.session.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 20:48
 */
class SessionPropertiesTests {

  @Test
  void defaultValuesAreSet() {
    SessionProperties properties = new SessionProperties();
    assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(30));
    assertThat(properties.isPersistent()).isFalse();
    assertThat(properties.getStoreDir()).isNull();
    assertThat(properties.getSessionIdLength()).isEqualTo(32);
    assertThat(properties.getMaxSessions()).isEqualTo(10000);
    assertThat(properties.cookie).isNotNull();
  }

  @Test
  void setTimeoutToNullIsAllowed() {
    var properties = new SessionProperties();
    properties.setTimeout(null);
    assertThat(properties.getTimeout()).isNull();
  }

  @Test
  void setPersistentUpdatesPersistent() {
    var properties = new SessionProperties();
    properties.setPersistent(true);
    assertThat(properties.isPersistent()).isTrue();
  }

  @Test
  void setStoreDirUpdatesStoreDir(@TempDir File tempDir) {
    var properties = new SessionProperties();
    properties.setStoreDir(tempDir);
    assertThat(properties.getStoreDir()).isEqualTo(tempDir);
  }

  @Test
  void setSessionIdLengthUpdatesSessionIdLength() {
    var properties = new SessionProperties();
    properties.setSessionIdLength(64);
    assertThat(properties.getSessionIdLength()).isEqualTo(64);
  }

  @Test
  void setSessionIdLengthWithZeroThrowsException() {
    var properties = new SessionProperties();
    assertThatIllegalArgumentException().isThrownBy(() -> properties.setSessionIdLength(0));
  }

  @Test
  void setSessionIdLengthWithNegativeValueThrowsException() {
    var properties = new SessionProperties();
    assertThatIllegalArgumentException().isThrownBy(() -> properties.setSessionIdLength(-1));
  }

  @Test
  void setMaxSessionsUpdatesMaxSessions() {
    var properties = new SessionProperties();
    properties.setMaxSessions(20000);
    assertThat(properties.getMaxSessions()).isEqualTo(20000);
  }

  @Test
  void getValidStoreDirWhenStoreDirIsFileThrowsException(@TempDir File tempDir) throws IOException {
    var properties = new SessionProperties();
    var file = new File(tempDir, "file.txt");
    file.createNewFile();
    properties.setStoreDir(file);
    assertThatIllegalStateException().isThrownBy(() -> properties.getValidStoreDir(null))
            .withMessageContaining("points to a file");
  }

  @Test
  void getValidStoreDirWithRelativePathResolvesRelativeToApplicationHome() {
    // This test assumes ApplicationHome resolves to the project root, which is standard.
    var properties = new SessionProperties();
    var relativeDir = new File("build/sessions");
    properties.setStoreDir(relativeDir);
    var validStoreDir = properties.getValidStoreDir(null);
    assertThat(validStoreDir).isAbsolute();
    assertThat(validStoreDir.getAbsolutePath().endsWith(relativeDir.getPath())).isTrue();
    assertThat(validStoreDir).exists().isDirectory();
    // Cleanup
    validStoreDir.delete();
  }

  @Test
  void getValidStoreDirWhenMkdirsIsFalseAndDirExistsReturnsDir(@TempDir File tempDir) {
    var properties = new SessionProperties();
    properties.setStoreDir(tempDir);
    var validStoreDir = properties.getValidStoreDir(null, false);
    assertThat(validStoreDir).isEqualTo(tempDir);
  }

}
