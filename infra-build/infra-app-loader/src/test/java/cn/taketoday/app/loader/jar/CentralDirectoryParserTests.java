/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.app.loader.jar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.app.loader.TestJarCreator;
import cn.taketoday.app.loader.data.RandomAccessData;
import cn.taketoday.app.loader.data.RandomAccessDataFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CentralDirectoryParser}.
 *
 * @author Phillip Webb
 */
class CentralDirectoryParserTests {

  private File jarFile;

  private RandomAccessDataFile jarData;

  @BeforeEach
  void setup(@TempDir File tempDir) throws Exception {
    this.jarFile = new File(tempDir, "test.jar");
    TestJarCreator.createTestJar(this.jarFile);
    this.jarData = new RandomAccessDataFile(this.jarFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    this.jarData.close();
  }

  @Test
  void visitsInOrder() throws Exception {
    MockCentralDirectoryVisitor visitor = new MockCentralDirectoryVisitor();
    CentralDirectoryParser parser = new CentralDirectoryParser();
    parser.addVisitor(visitor);
    parser.parse(this.jarData, false);
    List<String> invocations = visitor.getInvocations();
    assertThat(invocations).startsWith("visitStart").endsWith("visitEnd").contains("visitFileHeader");
  }

  @Test
  void visitRecords() throws Exception {
    Collector collector = new Collector();
    CentralDirectoryParser parser = new CentralDirectoryParser();
    parser.addVisitor(collector);
    parser.parse(this.jarData, false);
    Iterator<CentralDirectoryFileHeader> headers = collector.getHeaders().iterator();
    assertThat(headers.next().getName()).hasToString("META-INF/");
    assertThat(headers.next().getName()).hasToString("META-INF/MANIFEST.MF");
    assertThat(headers.next().getName()).hasToString("1.dat");
    assertThat(headers.next().getName()).hasToString("2.dat");
    assertThat(headers.next().getName()).hasToString("d/");
    assertThat(headers.next().getName()).hasToString("d/9.dat");
    assertThat(headers.next().getName()).hasToString("special/");
    assertThat(headers.next().getName()).hasToString("special/\u00EB.dat");
    assertThat(headers.next().getName()).hasToString("nested.jar");
    assertThat(headers.next().getName()).hasToString("another-nested.jar");
    assertThat(headers.next().getName()).hasToString("space nested.jar");
    assertThat(headers.next().getName()).hasToString("multi-release.jar");
    assertThat(headers.hasNext()).isFalse();
  }

  static class Collector implements CentralDirectoryVisitor {

    private final List<CentralDirectoryFileHeader> headers = new ArrayList<>();

    @Override
    public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
    }

    @Override
    public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
      this.headers.add(fileHeader.clone());
    }

    @Override
    public void visitEnd() {
    }

    List<CentralDirectoryFileHeader> getHeaders() {
      return this.headers;
    }

  }

  static class MockCentralDirectoryVisitor implements CentralDirectoryVisitor {

    private final List<String> invocations = new ArrayList<>();

    @Override
    public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
      this.invocations.add("visitStart");
    }

    @Override
    public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
      this.invocations.add("visitFileHeader");
    }

    @Override
    public void visitEnd() {
      this.invocations.add("visitEnd");
    }

    List<String> getInvocations() {
      return this.invocations;
    }

  }

}
