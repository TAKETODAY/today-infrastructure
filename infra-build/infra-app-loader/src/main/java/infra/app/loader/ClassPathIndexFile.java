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

package infra.app.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * A class path index file that provides an ordered classpath for exploded JARs.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class ClassPathIndexFile {

  @Nullable
  private final File root;

  private final Set<String> lines;

  private ClassPathIndexFile(@Nullable File root, List<String> lines) {
    this.root = root;
    this.lines = lines.stream().map(this::extractName).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private String extractName(String line) {
    if (line.startsWith("- \"") && line.endsWith("\"")) {
      return line.substring(3, line.length() - 1);
    }
    throw new IllegalStateException("Malformed classpath index line [" + line + "]");
  }

  int size() {
    return this.lines.size();
  }

  boolean containsEntry(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    return this.lines.contains(name);
  }

  List<URL> getUrls() {
    return this.lines.stream().map(this::asUrl).toList();
  }

  private URL asUrl(String line) {
    try {
      return new File(this.root, line).toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Nullable
  static ClassPathIndexFile loadIfPossible(@Nullable File root, String location) throws IOException {
    return loadIfPossible(root, new File(root, location));
  }

  @Nullable
  private static ClassPathIndexFile loadIfPossible(@Nullable File root, File indexFile) throws IOException {
    if (indexFile.exists() && indexFile.isFile()) {
      List<String> lines = Files.readAllLines(indexFile.toPath())
              .stream()
              .filter(ClassPathIndexFile::lineHasText)
              .toList();
      return new ClassPathIndexFile(root, lines);
    }
    return null;
  }

  private static boolean lineHasText(String line) {
    return !line.trim().isEmpty();
  }

}
