/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassPathIndexFile}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ClassPathIndexFileTests {

  @TempDir
  File temp;

  @Test
  void loadIfPossibleWhenRootIsNotFileReturnsNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ClassPathIndexFile.loadIfPossible(new URL("https://example.com/file"), "test.idx"))
            .withMessage("URL does not reference a file");
  }

  @Test
  void loadIfPossibleWhenRootDoesNotExistReturnsNull() throws Exception {
    File root = new File(this.temp, "missing");
    assertThat(ClassPathIndexFile.loadIfPossible(root.toURI().toURL(), "test.idx")).isNull();
  }

  @Test
  void loadIfPossibleWhenRootIsDirectoryThrowsException() throws Exception {
    File root = new File(this.temp, "directory");
    root.mkdirs();
    assertThat(ClassPathIndexFile.loadIfPossible(root.toURI().toURL(), "test.idx")).isNull();
  }

  @Test
  void loadIfPossibleReturnsInstance() throws Exception {
    ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
    assertThat(indexFile).isNotNull();
  }

  @Test
  void sizeReturnsNumberOfLines() throws Exception {
    ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
    assertThat(indexFile.size()).isEqualTo(5);
  }

  @Test
  void getUrlsReturnsUrls() throws Exception {
    ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
    List<URL> urls = indexFile.getUrls();
    List<File> expected = new ArrayList<>();
    expected.add(new File(this.temp, "APP-INF/layers/one/lib/a.jar"));
    expected.add(new File(this.temp, "APP-INF/layers/one/lib/b.jar"));
    expected.add(new File(this.temp, "APP-INF/layers/one/lib/c.jar"));
    expected.add(new File(this.temp, "APP-INF/layers/two/lib/d.jar"));
    expected.add(new File(this.temp, "APP-INF/layers/two/lib/e.jar"));
    assertThat(urls).containsExactly(expected.stream().map(this::toUrl).toArray(URL[]::new));
  }

  private URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private ClassPathIndexFile copyAndLoadTestIndexFile() throws IOException {
    copyTestIndexFile();
    ClassPathIndexFile indexFile = ClassPathIndexFile.loadIfPossible(this.temp.toURI().toURL(), "test.idx");
    return indexFile;
  }

  private void copyTestIndexFile() throws IOException {
    Files.copy(getClass().getResourceAsStream("classpath-index-file.idx"),
            new File(this.temp, "test.idx").toPath());
  }

}
