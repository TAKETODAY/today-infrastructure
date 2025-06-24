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

package infra.jarmode.layertools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import infra.core.io.ClassPathResource;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ExtractCommand}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
//@DisabledOnOs(OS.LINUX)
@ExtendWith(MockitoExtension.class)
class ExtractCommandTests {

  private static final FileTime CREATION_TIME = FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS));

  private static final FileTime LAST_MODIFIED_TIME = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));

  private static final FileTime LAST_ACCESS_TIME = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));

  @TempDir
  File temp;

  @Mock
  private Context context;

  private File jarFile;

  private File extract;

  private final Layers layers = new TestLayers();

  private ExtractCommand command;

  @BeforeEach
  void setup() throws Exception {
    this.jarFile = createJarFile("test.jar");
    this.extract = new File(this.temp, "extract");
    this.extract.mkdir();
    this.command = new ExtractCommand(this.context, this.layers);
  }

  @Test
  void runExtractsLayers() {
    given(this.context.getArchiveFile()).willReturn(this.jarFile);
    given(this.context.getWorkingDir()).willReturn(this.extract);
    this.command.run(Collections.emptyMap(), Collections.emptyList());
    assertThat(this.extract.list()).containsOnly("a", "b", "c", "d");
    assertThat(new File(this.extract, "a/a/a.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "b/b/b.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "c/c/c.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "d")).isDirectory();
    assertThat(new File(this.extract.getParentFile(), "e.jar")).doesNotExist();
  }

  private void timeAttributes(File file) {
    try {
      var basicAttributes = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class).readAttributes();
      assertThat(basicAttributes.lastAccessTime()).isEqualTo(LAST_ACCESS_TIME);
      assertThat(basicAttributes.lastModifiedTime()).isEqualTo(LAST_MODIFIED_TIME);
      assertThat(basicAttributes.creationTime()).satisfiesAnyOf(
              (creationTime) -> assertThat(creationTime).isEqualTo(CREATION_TIME),
              // On macOS (at least) the creation time is the last modified time
              (creationTime) -> assertThat(creationTime).isEqualTo(LAST_MODIFIED_TIME));
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void runWhenHasDestinationOptionExtractsLayers() {
    given(this.context.getArchiveFile()).willReturn(this.jarFile);
    File out = new File(this.extract, "out");
    this.command.run(Collections.singletonMap(ExtractCommand.DESTINATION_OPTION, out.getAbsolutePath()),
            Collections.emptyList());
    assertThat(this.extract.list()).containsOnly("out");
    assertThat(new File(this.extract, "out/a/a/a.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "out/b/b/b.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "out/c/c/c.jar")).exists().satisfies(this::timeAttributes);
  }

  @Test
  void runWhenHasLayerParamsExtractsLimitedLayers() {
    given(this.context.getArchiveFile()).willReturn(this.jarFile);
    given(this.context.getWorkingDir()).willReturn(this.extract);
    this.command.run(Collections.emptyMap(), Arrays.asList("a", "c"));
    assertThat(this.extract.list()).containsOnly("a", "c");
    assertThat(new File(this.extract, "a/a/a.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract, "c/c/c.jar")).exists().satisfies(this::timeAttributes);
    assertThat(new File(this.extract.getParentFile(), "e.jar")).doesNotExist();
  }

  @Test
  void runWithJarFileContainingNoEntriesFails() throws IOException {
    File file = new File(this.temp, "empty.jar");
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("text");
    }
    given(this.context.getArchiveFile()).willReturn(file);
    given(this.context.getWorkingDir()).willReturn(this.extract);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.command.run(Collections.emptyMap(), Collections.emptyList()))
            .withMessageContaining("not compatible with layertools");
  }

  @Test
  void runWithJarFileThatWouldWriteEntriesOutsideDestinationFails() throws Exception {
    this.jarFile = createJarFile("test.jar", (out) -> {
      try {
        out.putNextEntry(new ZipEntry("e/../../e.jar"));
        out.closeEntry();
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    });
    given(this.context.getArchiveFile()).willReturn(this.jarFile);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.command.run(Collections.emptyMap(), Collections.emptyList()))
            .withMessageContaining("Entry 'e/../../e.jar' would be written");
  }

  private File createJarFile(String name) throws Exception {
    return createJarFile(name, (out) -> {
    });
  }

  private File createJarFile(String name, Consumer<ZipOutputStream> streamHandler) throws Exception {
    File file = new File(this.temp, name);
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
      out.putNextEntry(entry("a/"));
      out.closeEntry();
      out.putNextEntry(entry("a/a.jar"));
      out.closeEntry();
      out.putNextEntry(entry("b/"));
      out.closeEntry();
      out.putNextEntry(entry("b/b.jar"));
      out.closeEntry();
      out.putNextEntry(entry("c/"));
      out.closeEntry();
      out.putNextEntry(entry("c/c.jar"));
      out.closeEntry();
      out.putNextEntry(entry("d/"));
      out.closeEntry();
      out.putNextEntry(entry("META-INF/MANIFEST.MF"));
      out.write(getFile("test-manifest.MF").getBytes());
      out.closeEntry();
      streamHandler.accept(out);
    }
    return file;
  }

  private ZipEntry entry(String path) {
    ZipEntry entry = new ZipEntry(path);
    entry.setCreationTime(CREATION_TIME);
    entry.setLastModifiedTime(LAST_MODIFIED_TIME);
    entry.setLastAccessTime(LAST_ACCESS_TIME);
    return entry;
  }

  private String getFile(String fileName) throws Exception {
    ClassPathResource resource = new ClassPathResource(fileName, getClass());
    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

  private static class TestLayers implements Layers {

    @Override
    public Iterator<String> iterator() {
      return Arrays.asList("a", "b", "c", "d").iterator();
    }

    @Override
    public String getLayer(String entryName) {
      if (entryName.startsWith("a")) {
        return "a";
      }
      if (entryName.startsWith("b")) {
        return "b";
      }
      return "c";
    }

    @Override
    public String getApplicationLayerName() {
      return "application";
    }

  }

}
