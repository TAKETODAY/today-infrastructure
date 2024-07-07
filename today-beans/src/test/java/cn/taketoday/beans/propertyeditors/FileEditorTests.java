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

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Thomas Risberg
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class FileEditorTests {

  @Test
  void testClasspathFileName() {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class");
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).exists();
  }

  @Test
  void testWithNonExistentResource() {
    PropertyEditor fileEditor = new FileEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            fileEditor.setAsText("classpath:no_way_this_file_is_found.doc"));
  }

  @Test
  void testWithNonExistentFile() {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("file:no_way_this_file_is_found.doc");
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).doesNotExist();
  }

  @Test
  void testAbsoluteFileName() {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("/no_way_this_file_is_found.doc");
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).doesNotExist();
  }

  @Test
  void testCurrentDirectory() {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("file:.");
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).isEqualTo(new File("."));
  }

  @Test
  void testUnqualifiedFileNameFound() {
    PropertyEditor fileEditor = new FileEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class";
    fileEditor.setAsText(fileName);
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).exists();
    String absolutePath = file.getAbsolutePath().replace('\\', '/');
    assertThat(absolutePath).endsWith(fileName);
  }

  @Test
  void testUnqualifiedFileNameNotFound() {
    PropertyEditor fileEditor = new FileEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".clazz";
    fileEditor.setAsText(fileName);
    Object value = fileEditor.getValue();
    assertThat(value).isInstanceOf(File.class);
    File file = (File) value;
    assertThat(file).doesNotExist();
    String absolutePath = file.getAbsolutePath().replace('\\', '/');
    assertThat(absolutePath).endsWith(fileName);
  }

}
