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

package cn.taketoday.app.loader.tools;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LayersIndex}.
 *
 * @author Phillip Webb
 */
class LayersIndexTests {

  private static final Layer LAYER_A = new Layer("a");

  private static final Layer LAYER_B = new Layer("b");

  private static final Layer LAYER_C = new Layer("c");

  private String testMethodName;

  @BeforeEach
  void setup(TestInfo testInfo) {
    this.testMethodName = testInfo.getTestMethod().get().getName();
  }

  @Test
  void writeToWhenSimpleNamesSortsAlphabetically() {
    LayersIndex index = new LayersIndex(LAYER_A);
    index.add(LAYER_A, "cat");
    index.add(LAYER_A, "dog");
    index.add(LAYER_A, "aardvark");
    index.add(LAYER_A, "zerbra");
    index.add(LAYER_A, "hamster");
    assertThatIndex(index).writesExpectedContent();
  }

  @Test
  void writeToWritesLayersInIteratorOrder() {
    LayersIndex index = new LayersIndex(LAYER_B, LAYER_A, LAYER_C);
    index.add(LAYER_A, "a1");
    index.add(LAYER_A, "a2");
    index.add(LAYER_B, "b1");
    index.add(LAYER_B, "b2");
    index.add(LAYER_C, "c1");
    index.add(LAYER_C, "c2");
    assertThatIndex(index).writesExpectedContent();
  }

  @Test
  void writeToWhenLayerNotUsedDoesNotSkipLayer() {
    LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
    index.add(LAYER_A, "a1");
    index.add(LAYER_A, "a2");
    index.add(LAYER_C, "c1");
    index.add(LAYER_C, "c2");
    assertThatIndex(index).writesExpectedContent();
  }

  @Test
  void writeToWhenAllFilesInDirectoryAreInSameLayerUsesDirectory() {
    LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
    index.add(LAYER_A, "a1/b1/c1");
    index.add(LAYER_A, "a1/b1/c2");
    index.add(LAYER_A, "a1/b2/c1");
    index.add(LAYER_B, "a2/b1");
    index.add(LAYER_B, "a2/b2");
    assertThatIndex(index).writesExpectedContent();
  }

  @Test
  void writeToWhenAllFilesInDirectoryAreInNotInSameLayerUsesFiles() {
    LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
    index.add(LAYER_A, "a1/b1/c1");
    index.add(LAYER_B, "a1/b1/c2");
    index.add(LAYER_C, "a1/b2/c1");
    index.add(LAYER_A, "a2/b1");
    index.add(LAYER_B, "a2/b2");
    assertThatIndex(index).writesExpectedContent();
  }

  @Test
  void writeToWhenSpaceInFileName() {
    LayersIndex index = new LayersIndex(LAYER_A);
    index.add(LAYER_A, "a b");
    index.add(LAYER_A, "a b/c");
    index.add(LAYER_A, "a b/d");
    assertThatIndex(index).writesExpectedContent();
  }

  private LayersIndexAssert assertThatIndex(LayersIndex index) {
    return new LayersIndexAssert(index);
  }

  private class LayersIndexAssert extends AbstractObjectAssert<LayersIndexAssert, LayersIndex> {

    LayersIndexAssert(LayersIndex actual) {
      super(actual, LayersIndexAssert.class);
    }

    void writesExpectedContent() {
      try {
        String actualContent = getContent();
        String name = "LayersIndexTests-" + LayersIndexTests.this.testMethodName + ".txt";
        InputStream in = LayersIndexTests.class.getResourceAsStream(name);
        Assert.state(in != null, () -> "Can't read " + name);
        String expectedContent = new String(FileCopyUtils.copyToByteArray(in), StandardCharsets.UTF_8);
        expectedContent = expectedContent.replace("\r", "");
        assertThat(actualContent).isEqualTo(expectedContent);
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }

    }

    private String getContent() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.actual.writeTo(out);
      return out.toString(StandardCharsets.UTF_8);
    }

  }

}
