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

package cn.taketoday.app.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class path index file that provides ordering information for JARs.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ClassPathIndexFile {

  private final File root;

  private final List<String> lines;

  private ClassPathIndexFile(File root, List<String> lines) {
    this.root = root;
    this.lines = lines.stream().map(this::extractName).toList();
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

  boolean containsEntry(String name) {
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

  static ClassPathIndexFile loadIfPossible(URL root, String location) throws IOException {
    return loadIfPossible(asFile(root), location);
  }

  private static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
    return loadIfPossible(root, new File(root, location));
  }

  private static ClassPathIndexFile loadIfPossible(File root, File indexFile) throws IOException {
    if (indexFile.exists() && indexFile.isFile()) {
      try (InputStream inputStream = new FileInputStream(indexFile)) {
        return new ClassPathIndexFile(root, loadLines(inputStream));
      }
    }
    return null;
  }

  private static List<String> loadLines(InputStream inputStream) throws IOException {
    List<String> lines = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    String line = reader.readLine();
    while (line != null) {
      if (!line.trim().isEmpty()) {
        lines.add(line);
      }
      line = reader.readLine();
    }
    return Collections.unmodifiableList(lines);
  }

  private static File asFile(URL url) {
    if (!"file".equals(url.getProtocol())) {
      throw new IllegalArgumentException("URL does not reference a file");
    }
    try {
      return new File(url.toURI());
    }
    catch (URISyntaxException ex) {
      return new File(url.getPath());
    }
  }

}
