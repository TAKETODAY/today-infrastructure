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
import java.net.URL;

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class URLEditorTests {

  @Test
  public void testCtorWithNullResourceEditor() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new URLEditor(null));
  }

  @Test
  public void testStandardURI() throws Exception {
    PropertyEditor urlEditor = new URLEditor();
    urlEditor.setAsText("mailto:juergen.hoeller@interface21.com");
    Object value = urlEditor.getValue();
    boolean condition = value instanceof URL;
    assertThat(condition).isTrue();
    URL url = (URL) value;
    assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
  }

  @Test
  public void testStandardURL() throws Exception {
    PropertyEditor urlEditor = new URLEditor();
    urlEditor.setAsText("https://www.springframework.org");
    Object value = urlEditor.getValue();
    boolean condition = value instanceof URL;
    assertThat(condition).isTrue();
    URL url = (URL) value;
    assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
  }

  @Test
  public void testClasspathURL() throws Exception {
    PropertyEditor urlEditor = new URLEditor();
    urlEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
            "/" + ClassUtils.getShortName(getClass()) + ".class");
    Object value = urlEditor.getValue();
    boolean condition1 = value instanceof URL;
    assertThat(condition1).isTrue();
    URL url = (URL) value;
    assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
    boolean condition = !url.getProtocol().startsWith("classpath");
    assertThat(condition).isTrue();
  }

  @Test
  public void testWithNonExistentResource() {
    PropertyEditor urlEditor = new URLEditor();
    urlEditor.setAsText("gonna:/freak/in/the/morning/freak/in/the.evening");
  }

  @Test
  public void testSetAsTextWithNull() throws Exception {
    PropertyEditor urlEditor = new URLEditor();
    urlEditor.setAsText(null);
    assertThat(urlEditor.getValue()).isNull();
    assertThat(urlEditor.getAsText()).isEqualTo("");
  }

  @Test
  public void testGetAsTextReturnsEmptyStringIfValueNotSet() throws Exception {
    PropertyEditor urlEditor = new URLEditor();
    assertThat(urlEditor.getAsText()).isEqualTo("");
  }

}
