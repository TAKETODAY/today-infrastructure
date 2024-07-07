/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditor;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
class PathEditorTests {

  @Test
  void testClasspathPathName() {
    PropertyEditor pathEditor = new PathEditor();
    pathEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class");
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    assertThat(path.toFile()).exists();
  }

  @Test
  void testWithNonExistentResource() {
    PropertyEditor pathEditor = new PathEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            pathEditor.setAsText("classpath:/no_way_this_file_is_found.doc"));
  }

  @Test
  void testWithNonExistentPath() {
    PropertyEditor pathEditor = new PathEditor();
    pathEditor.setAsText("file:/no_way_this_file_is_found.doc");
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    assertThat(path.toFile()).doesNotExist();
  }

  @Test
  void testAbsolutePath() {
    PropertyEditor pathEditor = new PathEditor();
    pathEditor.setAsText("/no_way_this_file_is_found.doc");
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    assertThat(path.toFile()).doesNotExist();
  }

  @Test
  void testWindowsAbsolutePath() {
    PropertyEditor pathEditor = new PathEditor();
    pathEditor.setAsText("C:\\no_way_this_file_is_found.doc");
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    assertThat(path.toFile()).doesNotExist();
  }

  @Test
  void testWindowsAbsoluteFilePath() {
    PropertyEditor pathEditor = new PathEditor();
    try {
      pathEditor.setAsText("file://C:\\no_way_this_file_is_found.doc");
      Object value = pathEditor.getValue();
      assertThat(value).isInstanceOf(Path.class);
      Path path = (Path) value;
      assertThat(path.toFile()).doesNotExist();
    }
    catch (IllegalArgumentException ex) {
      if (File.separatorChar == '\\') {  // on Windows, otherwise silently ignore
        throw ex;
      }
    }
  }

  @Test
  void testCurrentDirectory() {
    PropertyEditor pathEditor = new PathEditor();
    pathEditor.setAsText("file:.");
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    assertThat(path).isEqualTo(Paths.get("."));
  }

  @Test
  void testUnqualifiedPathNameFound() {
    PropertyEditor pathEditor = new PathEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class";
    pathEditor.setAsText(fileName);
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    File file = path.toFile();
    assertThat(file).exists();
    String absolutePath = file.getAbsolutePath();
    if (File.separatorChar == '\\') {
      absolutePath = absolutePath.replace('\\', '/');
    }
    assertThat(absolutePath).endsWith(fileName);
  }

  @Test
  void testUnqualifiedPathNameNotFound() {
    PropertyEditor pathEditor = new PathEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".clazz";
    pathEditor.setAsText(fileName);
    Object value = pathEditor.getValue();
    assertThat(value).isInstanceOf(Path.class);
    Path path = (Path) value;
    File file = path.toFile();
    assertThat(file).doesNotExist();
    String absolutePath = file.getAbsolutePath();
    if (File.separatorChar == '\\') {
      absolutePath = absolutePath.replace('\\', '/');
    }
    assertThat(absolutePath).endsWith(fileName);
  }

}
