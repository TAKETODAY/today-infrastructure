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

package cn.taketoday.jarmode.layertools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link HelpCommand}.
 *
 * @author Phillip Webb
 */
class HelpCommandTests {

  private HelpCommand command;

  private TestPrintStream out;

  @TempDir
  File temp;

  @BeforeEach
  void setup() throws Exception {
    Context context = Mockito.mock(Context.class);
    given(context.getArchiveFile()).willReturn(createJarFile("test.jar"));
    this.command = new HelpCommand(context, LayerToolsJarMode.Runner.getCommands(context));
    this.out = new TestPrintStream(this);
  }

  @Test
  void runWhenHasNoParametersPrintsUsage() {
    this.command.run(this.out, Collections.emptyMap(), Collections.emptyList());
    assertThat(this.out).hasSameContentAsResource("help-output.txt");
  }

  @Test
  void runWhenHasNoCommandParameterPrintsUsage() {
    this.command.run(this.out, Collections.emptyMap(), Arrays.asList("extract"));
    System.out.println(this.out);
    assertThat(this.out).hasSameContentAsResource("help-extract-output.txt");
  }

  private File createJarFile(String name) throws Exception {
    File file = new File(this.temp, name);
    try (ZipOutputStream jarOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      jarOutputStream.write(getFile("test-manifest.MF").getBytes());
      jarOutputStream.closeEntry();
      JarEntry indexEntry = new JarEntry("APP-INF/layers.idx");
      jarOutputStream.putNextEntry(indexEntry);
      Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
      writer.write("- \"0001\":\n");
      writer.write("  - \"APP-INF/lib/a.jar\"\n");
      writer.write("  - \"APP-INF/lib/b.jar\"\n");
      writer.write("- \"0002\":\n");
      writer.write("  - \"APP-INF/lib/c.jar\"\n");
      writer.write("- \"0003\":\n");
      writer.write("  - \"APP-INF/lib/d.jar\"\n");
      writer.flush();
    }
    return file;
  }

  private String getFile(String fileName) throws Exception {
    ClassPathResource resource = new ClassPathResource(fileName, getClass());
    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

}
