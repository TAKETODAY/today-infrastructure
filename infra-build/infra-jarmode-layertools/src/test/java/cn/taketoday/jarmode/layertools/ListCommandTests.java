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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ListCommand}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ListCommandTests {

  @TempDir
  File temp;

  @Mock
  private Context context;

  private File jarFile;

  private ListCommand command;

  private TestPrintStream out;

  @BeforeEach
  void setup() throws Exception {
    this.jarFile = createJarFile("test.jar");
    given(this.context.getArchiveFile()).willReturn(this.jarFile);
    this.command = new ListCommand(this.context);
    this.out = new TestPrintStream(this);
  }

  @Test
  void listLayersShouldListLayers() {
    Layers layers = IndexedLayers.get(this.context);
    this.command.printLayers(layers, this.out);
    assertThat(this.out).hasSameContentAsResource("list-output.txt");
  }

  private File createJarFile(String name) throws Exception {
    File file = new File(this.temp, name);
    try (ZipOutputStream jarOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      writeLayersIndex(jarOutputStream);
      String entryPrefix = "APP-INF/lib/";
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "a/"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "a/a.jar"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "b/"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "b/b.jar"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "c/"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "c/c.jar"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "d/"));
      jarOutputStream.closeEntry();
      jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      jarOutputStream.write(getFile("test-manifest.MF").getBytes());
      jarOutputStream.closeEntry();
    }
    return file;
  }

  private void writeLayersIndex(ZipOutputStream out) throws IOException {
    JarEntry indexEntry = new JarEntry("APP-INF/layers.idx");
    out.putNextEntry(indexEntry);
    Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    writer.write("- \"0001\":\n");
    writer.write("  - \"APP-INF/lib/a.jar\"\n");
    writer.write("  - \"APP-INF/lib/b.jar\"\n");
    writer.write("- \"0002\":\n");
    writer.write("  - \"APP-INF/lib/c.jar\"\n");
    writer.write("- \"0003\":\n");
    writer.write("  - \"APP-INF/lib/d.jar\"\n");
    writer.flush();
  }

  private String getFile(String fileName) throws Exception {
    ClassPathResource resource = new ClassPathResource(fileName, getClass());
    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

}
