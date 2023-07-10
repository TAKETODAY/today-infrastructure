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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.FileCopyUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LayerToolsJarMode}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class LayerToolsJarModeTests {

  private static final String[] NO_ARGS = {};

  private TestPrintStream out;

  private PrintStream systemOut;

  @TempDir
  File temp;

  @BeforeEach
  void setup() throws Exception {
    Context context = mock(Context.class);
    given(context.getArchiveFile()).willReturn(createJarFile("test.jar"));
    this.out = new TestPrintStream(this);
    this.systemOut = System.out;
    System.setOut(this.out);
    LayerToolsJarMode.Runner.contextOverride = context;
  }

  @AfterEach
  void restore() {
    System.setOut(this.systemOut);
    LayerToolsJarMode.Runner.contextOverride = null;
  }

  @Test
  void mainWithNoParametersShowsHelp() {
    new LayerToolsJarMode().run("layertools", NO_ARGS);
    Assertions.assertThat(this.out).hasSameContentAsResource("help-output.txt");
  }

  @Test
  void mainWithArgRunsCommand() {
    new LayerToolsJarMode().run("layertools", new String[] { "list" });
    Assertions.assertThat(this.out).hasSameContentAsResource("list-output.txt");
  }

  @Test
  void mainWithUnknownCommandShowsErrorAndHelp() {
    new LayerToolsJarMode().run("layertools", new String[] { "invalid" });
    Assertions.assertThat(this.out).hasSameContentAsResource("error-command-unknown-output.txt");
  }

  @Test
  void mainWithUnknownOptionShowsErrorAndCommandHelp() {
    new LayerToolsJarMode().run("layertools", new String[] { "extract", "--invalid" });
    Assertions.assertThat(this.out).hasSameContentAsResource("error-option-unknown-output.txt");
  }

  @Test
  void mainWithOptionMissingRequiredValueShowsErrorAndCommandHelp() {
    new LayerToolsJarMode().run("layertools", new String[] { "extract", "--destination" });
    Assertions.assertThat(this.out).hasSameContentAsResource("error-option-missing-value-output.txt");
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
      writer.write("  - \"0002 APP-INF/lib/c.jar\"\n");
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
