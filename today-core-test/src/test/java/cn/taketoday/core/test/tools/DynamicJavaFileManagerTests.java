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

package cn.taketoday.core.test.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link DynamicJavaFileManager}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class DynamicJavaFileManagerTests {

  private static final byte[] DUMMY_BYTECODE = new byte[] { 'a' };

  private static final String DUMMY_RESOURCE = "a";

  @Mock
  private JavaFileManager parentFileManager;

  @Mock
  private Location location;

  private ClassLoader classLoader;

  private DynamicJavaFileManager fileManager;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    this.classLoader = new ClassLoader() { };
    ClassFiles classFiles = ClassFiles.of(
            ClassFile.of("com.example.one.ClassOne", DUMMY_BYTECODE),
            ClassFile.of("com.example.two.ClassTwo", DUMMY_BYTECODE));
    ResourceFiles resourceFiles = ResourceFiles.of(
            ResourceFile.of("com/example/one/resource.one", DUMMY_RESOURCE),
            ResourceFile.of("com/example/two/resource.two", DUMMY_RESOURCE));
    this.fileManager = new DynamicJavaFileManager(this.parentFileManager, this.classLoader,
            classFiles, resourceFiles);
  }

  @Test
  void getClassLoaderReturnsClassLoader() {
    assertThat(this.fileManager.getClassLoader(this.location)).isSameAs(
            this.classLoader);
  }

  @Test
  void getJavaFileForOutputWhenClassKindReturnsDynamicClassFile() throws Exception {
    JavaFileObject fileObject = this.fileManager.getJavaFileForOutput(this.location,
            "com.example.MyClass", Kind.CLASS, null);
    assertThat(fileObject).isInstanceOf(DynamicClassFileObject.class);
  }

  @Test
  void getJavaFileForOutputWhenClassKindAndAlreadySeenReturnsSameDynamicClassFile()
          throws Exception {
    JavaFileObject fileObject1 = this.fileManager.getJavaFileForOutput(this.location,
            "com.example.MyClass", Kind.CLASS, null);
    writeDummyResource(fileObject1);
    JavaFileObject fileObject2 = this.fileManager.getJavaFileForOutput(this.location,
            "com.example.MyClass", Kind.CLASS, null);
    assertThat(fileObject1).isSameAs(fileObject2);
  }

  @Test
  void getJavaFileForOutputWhenNotClassKindDelegatesToParentFileManager()
          throws Exception {
    this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass",
            Kind.SOURCE, null);
    then(this.parentFileManager).should().getJavaFileForOutput(this.location,
            "com.example.MyClass", Kind.SOURCE, null);
  }

  @Test
  void getClassFilesReturnsClassFiles() throws Exception {
    writeDummyBytecode(this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass1",
            Kind.CLASS, null));
    writeDummyBytecode(this.fileManager.getJavaFileForOutput(this.location, "com.example.MyClass2",
            Kind.CLASS, null));
    assertThat(this.fileManager.getDynamicClassFiles()).containsKeys(
            "com.example.MyClass1", "com.example.MyClass2");
  }

  @Test
  void listWithoutRecurseReturnsClassesInRequestedPackage() throws IOException {
    Iterable<JavaFileObject> listed = this.fileManager.list(
            this.location, "com.example.one", EnumSet.allOf(Kind.class), false);
    assertThat(listed).hasSize(1);
    assertThat(listed).extracting(JavaFileObject::getName).containsExactly("/com/example/one/ClassOne.class");
  }

  @Test
  void listWithRecurseReturnsClassesInRequestedPackageAndSubpackages() throws IOException {
    Iterable<JavaFileObject> listed = this.fileManager.list(
            this.location, "com.example", EnumSet.allOf(Kind.class), true);
    assertThat(listed).hasSize(2);
    assertThat(listed).extracting(JavaFileObject::getName)
            .containsExactly("/com/example/one/ClassOne.class", "/com/example/two/ClassTwo.class");
  }

  @Test
  void listWithoutClassKindDoesNotReturnClasses() throws IOException {
    Iterable<JavaFileObject> listed = this.fileManager.list(
            this.location, "com.example", EnumSet.of(Kind.SOURCE), true);
    assertThat(listed).isEmpty();
  }

  @Test
  void getFileForOutputReturnsDynamicResourceFile() {
    FileObject fileObject = this.fileManager.getFileForOutput(this.location,
            "", "META-INF/generated.properties", null);
    assertThat(fileObject).isInstanceOf(DynamicResourceFileObject.class);
  }

  @Test
  void getFileForOutputReturnsFile() throws Exception {
    writeDummyResource(this.fileManager.getFileForOutput(this.location, "", "META-INF/first.properties", null));
    writeDummyResource(this.fileManager.getFileForOutput(this.location, "", "META-INF/second.properties", null));
    assertThat(this.fileManager.getDynamicResourceFiles()).containsKeys("META-INF/first.properties",
            "META-INF/second.properties");
  }

  private void writeDummyBytecode(JavaFileObject fileObject) throws IOException {
    try (OutputStream outputStream = fileObject.openOutputStream()) {
      StreamUtils.copy(DUMMY_BYTECODE, outputStream);
    }
  }

  private void writeDummyResource(FileObject fileObject) throws IOException {
    try (OutputStream outputStream = fileObject.openOutputStream()) {
      StreamUtils.copy(DUMMY_RESOURCE.getBytes(), outputStream);
    }
  }

}
