/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.jarmode.layertools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Context}.
 *
 * @author Phillip Webb
 */
class ContextTests {

  @TempDir
  File temp;

  @Test
  void createWhenSourceIsNullThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> new Context(null, this.temp))
            .withMessage("Unable to find source archive");
  }

  @Test
  void createWhenSourceIsDirectoryThrowsException() {
    File directory = new File(this.temp, "test");
    directory.mkdir();
    assertThatIllegalStateException().isThrownBy(() -> new Context(directory, this.temp))
            .withMessage("Unable to find source archive");
  }

  @Test
  void createWhenSourceIsNotJarOrWarThrowsException() throws Exception {
    File zip = new File(this.temp, "test.zip");
    Files.createFile(zip.toPath());
    assertThatIllegalStateException().isThrownBy(() -> new Context(zip, this.temp))
            .withMessageContaining("test.zip must end with .jar or .war");
  }

  @Test
  void getJarFileReturnsJar() throws Exception {
    File jar = new File(this.temp, "test.jar");
    Files.createFile(jar.toPath());
    Context context = new Context(jar, this.temp);
    assertThat(context.getArchiveFile()).isEqualTo(jar);
  }

  @Test
  void getWorkingDirectoryReturnsWorkingDir() throws IOException {
    File jar = new File(this.temp, "test.jar");
    Files.createFile(jar.toPath());
    Context context = new Context(jar, this.temp);
    assertThat(context.getWorkingDir()).isEqualTo(this.temp);

  }

  @Test
  void getRelativePathReturnsRelativePath() throws Exception {
    File target = new File(this.temp, "target");
    target.mkdir();
    File jar = new File(target, "test.jar");
    Files.createFile(jar.toPath());
    Context context = new Context(jar, this.temp);
    assertThat(context.getRelativeArchiveDir()).isEqualTo("target");
  }

  @Test
  void getRelativePathWhenWorkingDirReturnsNull() throws Exception {
    File jar = new File(this.temp, "test.jar");
    Files.createFile(jar.toPath());
    Context context = new Context(jar, this.temp);
    assertThat(context.getRelativeArchiveDir()).isNull();
  }

  @Test
  void getRelativePathWhenCannotBeDeducedReturnsNull() throws Exception {
    File directory1 = new File(this.temp, "directory1");
    directory1.mkdir();
    File directory2 = new File(this.temp, "directory2");
    directory2.mkdir();
    File jar = new File(directory1, "test.jar");
    Files.createFile(jar.toPath());
    Context context = new Context(jar, directory2);
    assertThat(context.getRelativeArchiveDir()).isNull();
  }

}
