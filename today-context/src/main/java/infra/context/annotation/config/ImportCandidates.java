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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import infra.core.io.UrlResource;
import infra.lang.Assert;

/**
 * Contains {@code @Configuration} import candidates, usually auto-configurations.
 * <p>
 * The {@link #load(Class, ClassLoader)} method can be used to discover the import
 * candidates.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 22:46
 */
public final class ImportCandidates implements Iterable<String> {

  private static final String LOCATION = "META-INF/config/%s.imports";

  private static final String COMMENT_START = "#";

  private final ArrayList<String> candidates;

  private ImportCandidates(ArrayList<String> candidates) {
    Assert.notNull(candidates, "'candidates' is required");
    this.candidates = candidates;
  }

  @Override
  public Iterator<String> iterator() {
    return this.candidates.iterator();
  }

  /**
   * Returns the list of loaded import candidates.
   *
   * @return the list of import candidates
   */
  public ArrayList<String> getCandidates() {
    return this.candidates;
  }

  /**
   * Loads the names of import candidates from the classpath. The names of the import
   * candidates are stored in files named
   * {@code META-INF/config/full-qualified-annotation-name.imports} on the classpath.
   * Every line contains the full qualified name of the candidate class. Comments are
   * supported using the # character.
   *
   * @param annotation annotation to load
   * @param classLoader class loader to use for loading
   * @return list of names of annotated classes
   */
  public static ImportCandidates load(Class<?> annotation, @Nullable ClassLoader classLoader) {
    Assert.notNull(annotation, "'annotation' is required");
    ClassLoader classLoaderToUse = decideClassloader(classLoader);
    String location = String.format(LOCATION, annotation.getName());
    Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
    ArrayList<String> importCandidates = new ArrayList<>();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      importCandidates.addAll(readCandidateConfigurations(url));
    }
    return new ImportCandidates(importCandidates);
  }

  private static ClassLoader decideClassloader(@Nullable ClassLoader classLoader) {
    if (classLoader == null) {
      return ImportCandidates.class.getClassLoader();
    }
    return classLoader;
  }

  private static Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
    try {
      return classLoader.getResources(location);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Failed to load configurations from location [%s]".formatted(location), ex);
    }
  }

  private static List<String> readCandidateConfigurations(URL url) {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
      List<String> candidates = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        line = stripComment(line);
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        candidates.add(line);
      }
      return candidates;
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Unable to load configurations from location [%s]".formatted(url), ex);
    }
  }

  private static String stripComment(String line) {
    int commentStart = line.indexOf(COMMENT_START);
    if (commentStart == -1) {
      return line;
    }
    return line.substring(0, commentStart);
  }

}
