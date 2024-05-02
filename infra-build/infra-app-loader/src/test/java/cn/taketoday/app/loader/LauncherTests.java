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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Launcher}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
@AssertFileChannelDataBlocksClosed
class LauncherTests {

  /**
   * Jar Mode tests.
   */
  @Nested
  class JarMode {

    @BeforeEach
    void setup() {
      System.setProperty(JarModeRunner.DISABLE_SYSTEM_EXIT, "true");
    }

    @AfterEach
    void cleanup() {
      System.clearProperty("jarmode");
      System.clearProperty(JarModeRunner.DISABLE_SYSTEM_EXIT);
    }

    @Test
    void launchWhenJarModePropertyIsSetLaunchesJarMode(CapturedOutput out) throws Exception {
      System.setProperty("jarmode", "test");
      new TestLauncher().launch(new String[] { "boot" });
      assertThat(out).contains("running in test jar mode [boot]");
    }

    @Test
    void launchWhenJarModePropertyIsNotAcceptedThrowsException(CapturedOutput out) throws Exception {
      System.setProperty("jarmode", "idontexist");
      new TestLauncher().launch(new String[] { "boot" });
      assertThat(out).contains("Unsupported jarmode 'idontexist'");
    }

  }

  private static final class TestLauncher extends Launcher {

    @Override
    protected String getMainClass() throws Exception {
      throw new IllegalStateException("Should not be called");
    }

    @Override
    protected Archive getArchive() {
      return null;
    }

    @Override
    protected Set<URL> getClassPathUrls() throws Exception {
      return Collections.emptySet();
    }

    @Override
    protected void launch(String[] args) throws Exception {
      super.launch(args);
    }

  }

}
