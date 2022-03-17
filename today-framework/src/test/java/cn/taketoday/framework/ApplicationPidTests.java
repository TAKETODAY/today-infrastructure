/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
    assertThat(new ApplicationPid("123").toString()).isEqualTo("123");
  }

  @Test
  void toStringWithoutPid() {
    assertThat(new ApplicationPid(null).toString()).isEqualTo("???");
  }

  @Test
  void throwIllegalStateWritingMissingPid() {
    ApplicationPid pid = new ApplicationPid(null);
    assertThatIllegalStateException().isThrownBy(() -> pid.write(new File(this.tempDir, "pid")))
            .withMessageContaining("No PID available");
  }

  @Test
  void writePid() throws Exception {
    ApplicationPid pid = new ApplicationPid("123");
    File file = new File(this.tempDir, "pid");
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void writeNewPid() throws Exception {
    // gh-10784
    ApplicationPid pid = new ApplicationPid("123");
    File file = new File(this.tempDir, "pid");
    file.delete();
    pid.write(file);
    assertThat(contentOf(file)).isEqualTo("123");
  }

  @Test
  void getPidFromJvm() {
    assertThat(new ApplicationPid().toString()).isNotEmpty();
  }

}