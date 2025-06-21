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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import infra.core.io.ClassPathResource;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IndexedLayers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IndexedLayersTests {

  @TempDir
  File temp;

  @Test
  void createWhenIndexFileIsEmptyThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers(" \n ", "APP-INF/classes"))
            .withMessage("Empty layer index file loaded");
  }

  @Test
  void createWhenIndexFileIsMalformedThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> new IndexedLayers("test", "APP-INF/classes"))
            .withMessage("Layer index file is malformed");
  }

  @Test
  void iteratorReturnsLayers() throws Exception {
    IndexedLayers layers = new IndexedLayers(getIndex(), "APP-INF/classes");
    assertThat(layers).containsExactly("test", "empty", "application");
  }

  @Test
  void getLayerWhenMatchesNameReturnsLayer() throws Exception {
    IndexedLayers layers = new IndexedLayers(getIndex(), "APP-INF/classes");
    assertThat(layers.getLayer(mockEntry("APP-INF/lib/a.jar"))).isEqualTo("test");
    assertThat(layers.getLayer(mockEntry("APP-INF/classes/Demo.class"))).isEqualTo("application");
  }

  @Test
  void getLayerWhenMatchesNameForMissingLayerThrowsException() throws Exception {
    IndexedLayers layers = new IndexedLayers(getIndex(), "APP-INF/classes");
    assertThatIllegalStateException().isThrownBy(() -> layers.getLayer(mockEntry("file.jar")))
            .withMessage("No layer defined in index for file " + "'file.jar'");
  }

  @Test
  void getLayerWhenMatchesDirectoryReturnsLayer() throws Exception {
    IndexedLayers layers = new IndexedLayers(getIndex(), "APP-INF/classes");
    assertThat(layers.getLayer(mockEntry("META-INF/MANIFEST.MF"))).isEqualTo("application");
    assertThat(layers.getLayer(mockEntry("META-INF/a/sub/directory/and/a/file"))).isEqualTo("application");
  }

  @Test
  void getLayerWhenFileHasSpaceReturnsLayer() throws Exception {
    IndexedLayers layers = new IndexedLayers(getIndex(), "APP-INF/classes");
    assertThat(layers.getLayer(mockEntry("a b/c d"))).isEqualTo("application");
  }

  @Test
  void getShouldReturnIndexedLayersFromContext() throws Exception {
    Context context = mock(Context.class);
    given(context.getArchiveFile()).willReturn(createWarFile("test.war"));
    IndexedLayers layers = IndexedLayers.get(context);
    assertThat(layers.getLayer(mockEntry("WEB-INF/lib/a.jar"))).isEqualTo("test");
  }

  private String getIndex() throws Exception {
    return getFile("test-layers.idx");
  }

  private String getFile(String fileName) throws Exception {
    ClassPathResource resource = new ClassPathResource(fileName, getClass());
    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

  private ZipEntry mockEntry(String name) {
    ZipEntry entry = mock(ZipEntry.class);
    given(entry.getName()).willReturn(name);
    return entry;
  }

  private File createWarFile(String name) throws Exception {
    File file = new File(this.temp, name);
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
      out.putNextEntry(new ZipEntry("WEB-INF/lib/a/"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("WEB-INF/lib/a/a.jar"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("WEB-INF/classes/Demo.class"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      out.write(getFile("test-war-manifest.MF").getBytes());
      out.closeEntry();
      out.putNextEntry(new ZipEntry("WEB-INF/layers.idx"));
      out.write(getFile("test-war-layers.idx").getBytes());
      out.closeEntry();
    }
    return file;
  }

}
