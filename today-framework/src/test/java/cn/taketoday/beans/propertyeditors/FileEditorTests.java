/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditor;
import java.io.File;

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Risberg
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class FileEditorTests {

  @Test
  public void testClasspathFileName() throws Exception {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class");
    Object value = fileEditor.getValue();
    boolean condition = value instanceof File;
    assertThat(condition).isTrue();
    File file = (File) value;
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void testWithNonExistentResource() throws Exception {
    FileEditor fileEditor = new FileEditor();
    fileEditor.setAsText("classpath:no_way_this_file_is_found.doc");

    Object value = fileEditor.getValue();
    boolean condition1 = value instanceof File;
    assertThat(condition1).isTrue();
    File file = (File) value;
    boolean condition = !file.exists();
    assertThat(condition).isTrue();
  }

  @Test
  public void testWithNonExistentFile() throws Exception {
    FileEditor fileEditor = new FileEditor();
    fileEditor.setAsText("file:no_way_this_file_is_found.doc");
    Object value = fileEditor.getValue();
    boolean condition1 = value instanceof File;
    assertThat(condition1).isTrue();
    File file = (File) value;
    boolean condition = !file.exists();
    assertThat(condition).isTrue();
  }

  @Test
  public void testAbsoluteFileName() throws Exception {
    PropertyEditor fileEditor = new FileEditor();
    fileEditor.setAsText("/no_way_this_file_is_found.doc");
    Object value = fileEditor.getValue();
    boolean condition1 = value instanceof File;
    assertThat(condition1).isTrue();
    File file = (File) value;
    boolean condition = !file.exists();
    assertThat(condition).isTrue();
  }

  @Test
  public void testUnqualifiedFileNameFound() throws Exception {
    PropertyEditor fileEditor = new FileEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".class";
    fileEditor.setAsText(fileName);
    Object value = fileEditor.getValue();
    boolean condition = value instanceof File;
    assertThat(condition).isTrue();
    File file = (File) value;
    assertThat(file.exists()).isTrue();
    String absolutePath = file.getAbsolutePath().replace('\\', '/');
    assertThat(absolutePath.endsWith(fileName)).isTrue();
  }

  @Test
  public void testUnqualifiedFileNameNotFound() throws Exception {
    PropertyEditor fileEditor = new FileEditor();
    String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
            ClassUtils.getShortName(getClass()) + ".clazz";
    fileEditor.setAsText(fileName);
    Object value = fileEditor.getValue();
    boolean condition = value instanceof File;
    assertThat(condition).isTrue();
    File file = (File) value;
    assertThat(file.exists()).isFalse();
    String absolutePath = file.getAbsolutePath().replace('\\', '/');
    assertThat(absolutePath.endsWith(fileName)).isTrue();
  }

}
