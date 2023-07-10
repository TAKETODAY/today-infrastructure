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

package cn.taketoday.maven;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.ListAssert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Base class for archive (jar or war) related Maven plugin integration tests.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
abstract class AbstractArchiveIntegrationTests {

  protected String buildLog(File project) {
    return contentOf(new File(project, "target/build.log"));
  }

  protected String launchScript(File jar) {
    String content = contentOf(jar);
    return content.substring(0, content.indexOf(new String(new byte[] { 0x50, 0x4b, 0x03, 0x04 })));
  }

  protected AssertProvider<JarAssert> jar(File file) {
    return new AssertProvider<>() {

      @Override
      @Deprecated(since = "2.3.0", forRemoval = false)
      public JarAssert assertThat() {
        return new JarAssert(file);
      }

    };
  }

  protected Map<String, List<String>> readLayerIndex(JarFile jarFile) throws IOException {
    if (getLayersIndexLocation() == null) {
      return Collections.emptyMap();
    }
    Map<String, List<String>> index = new LinkedHashMap<>();
    String layerPrefix = "- ";
    String entryPrefix = "  - ";
    ZipEntry indexEntry = jarFile.getEntry(getLayersIndexLocation());
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(indexEntry)))) {
      String line = reader.readLine();
      String layer = null;
      while (line != null) {
        if (line.startsWith(layerPrefix)) {
          layer = line.substring(layerPrefix.length() + 1, line.length() - 2);
          index.put(layer, new ArrayList<>());
        }
        else if (line.startsWith(entryPrefix)) {
          index.computeIfAbsent(layer, (key) -> new ArrayList<>())
                  .add(line.substring(entryPrefix.length() + 1, line.length() - 1));
        }
        line = reader.readLine();
      }
      return index;
    }
  }

  protected String getLayersIndexLocation() {
    return null;
  }

  protected List<String> readClasspathIndex(JarFile jarFile, String location) throws IOException {
    List<String> index = new ArrayList<>();
    String entryPrefix = "- ";
    ZipEntry indexEntry = jarFile.getEntry(location);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(indexEntry)))) {
      String line = reader.readLine();
      while (line != null) {
        if (line.startsWith(entryPrefix)) {
          index.add(line.substring(entryPrefix.length() + 1, line.length() - 1));
        }
        line = reader.readLine();
      }
    }
    return index;
  }

  static final class JarAssert extends AbstractAssert<JarAssert, File> {

    private JarAssert(File actual) {
      super(actual, JarAssert.class);
      assertThat(actual).exists();
    }

    JarAssert doesNotHaveEntryWithName(String name) {
      withJarFile((jarFile) -> {
        withEntries(jarFile, (entries) -> {
          Optional<JarEntry> match = entries.filter((entry) -> entry.getName().equals(name)).findFirst();
          assertThat(match).isNotPresent();
        });
      });
      return this;
    }

    JarAssert hasEntryWithName(String name) {
      withJarFile((jarFile) -> {
        withEntries(jarFile, (entries) -> {
          Optional<JarEntry> match = entries.filter((entry) -> entry.getName().equals(name)).findFirst();
          assertThat(match).hasValueSatisfying((entry) -> assertThat(entry.getComment()).isNull());
        });
      });
      return this;
    }

    JarAssert hasEntryWithNameStartingWith(String prefix) {
      withJarFile((jarFile) -> {
        withEntries(jarFile, (entries) -> {
          Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
                  .findFirst();
          assertThat(match).hasValueSatisfying((entry) -> assertThat(entry.getComment()).isNull());
        });
      });
      return this;
    }

    JarAssert hasUnpackEntryWithNameStartingWith(String prefix) {
      withJarFile((jarFile) -> {
        withEntries(jarFile, (entries) -> {
          Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
                  .findFirst();
          assertThat(match).as("Name starting with %s", prefix)
                  .hasValueSatisfying((entry) -> assertThat(entry.getComment()).startsWith("UNPACK:"));
        });
      });
      return this;
    }

    JarAssert doesNotHaveEntryWithNameStartingWith(String prefix) {
      withJarFile((jarFile) -> {
        withEntries(jarFile, (entries) -> {
          Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
                  .findFirst();
          assertThat(match).isNotPresent();
        });
      });
      return this;
    }

    ListAssert<String> entryNamesInPath(String path) {
      List<String> matches = new ArrayList<>();
      withJarFile((jarFile) -> withEntries(jarFile,
              (entries) -> matches.addAll(entries.map(ZipEntry::getName)
                      .filter((name) -> name.startsWith(path) && name.length() > path.length())
                      .toList())));
      return new ListAssert<>(matches);
    }

    JarAssert manifest(Consumer<ManifestAssert> consumer) {
      withJarFile((jarFile) -> {
        try {
          consumer.accept(new ManifestAssert(jarFile.getManifest()));
        }
        catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
      return this;
    }

    void withJarFile(Consumer<JarFile> consumer) {
      try (JarFile jarFile = new JarFile(this.actual)) {
        consumer.accept(jarFile);
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    void withEntries(JarFile jarFile, Consumer<Stream<JarEntry>> entries) {
      entries.accept(Collections.list(jarFile.entries()).stream());
    }

    static final class ManifestAssert extends AbstractAssert<ManifestAssert, Manifest> {

      private ManifestAssert(Manifest actual) {
        super(actual, ManifestAssert.class);
      }

      ManifestAssert hasStartClass(String expected) {
        assertThat(this.actual.getMainAttributes().getValue("Start-Class")).isEqualTo(expected);
        return this;
      }

      ManifestAssert hasMainClass(String expected) {
        assertThat(this.actual.getMainAttributes().getValue("Main-Class")).isEqualTo(expected);
        return this;
      }

      ManifestAssert hasAttribute(String name, String value) {
        assertThat(this.actual.getMainAttributes().getValue(name)).isEqualTo(value);
        return this;
      }

      ManifestAssert doesNotHaveAttribute(String name) {
        assertThat(this.actual.getMainAttributes().getValue(name)).isNull();
        return this;
      }

    }

  }

}
