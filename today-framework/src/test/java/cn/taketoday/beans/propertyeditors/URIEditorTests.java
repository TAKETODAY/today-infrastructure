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
import java.net.URI;

import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
public class URIEditorTests {

  @Test
  public void standardURI() throws Exception {
    doTestURI("mailto:juergen.hoeller@interface21.com");
  }

  @Test
  public void withNonExistentResource() throws Exception {
    doTestURI("gonna:/freak/in/the/morning/freak/in/the.evening");
  }

  @Test
  public void standardURL() throws Exception {
    doTestURI("https://www.springframework.org");
  }

  @Test
  public void standardURLWithFragment() throws Exception {
    doTestURI("https://www.springframework.org#1");
  }

  @Test
  public void standardURLWithWhitespace() throws Exception {
    PropertyEditor uriEditor = new URIEditor();
    uriEditor.setAsText("  https://www.springframework.org  ");
    Object value = uriEditor.getValue();
    boolean condition = value instanceof URI;
    assertThat(condition).isTrue();
    URI uri = (URI) value;
    assertThat(uri.toString()).isEqualTo("https://www.springframework.org");
  }

  @Test
  public void classpathURL() throws Exception {
    PropertyEditor uriEditor = new URIEditor(getClass().getClassLoader());
    uriEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
            "/" + ClassUtils.getShortName(getClass()) + ".class");
    Object value = uriEditor.getValue();
    boolean condition1 = value instanceof URI;
    assertThat(condition1).isTrue();
    URI uri = (URI) value;
    assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
    boolean condition = !uri.getScheme().startsWith("classpath");
    assertThat(condition).isTrue();
  }

  @Test
  public void classpathURLWithWhitespace() throws Exception {
    PropertyEditor uriEditor = new URIEditor(getClass().getClassLoader());
    uriEditor.setAsText("  classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
            "/" + ClassUtils.getShortName(getClass()) + ".class  ");
    Object value = uriEditor.getValue();
    boolean condition1 = value instanceof URI;
    assertThat(condition1).isTrue();
    URI uri = (URI) value;
    assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
    boolean condition = !uri.getScheme().startsWith("classpath");
    assertThat(condition).isTrue();
  }

  @Test
  public void classpathURLAsIs() throws Exception {
    PropertyEditor uriEditor = new URIEditor();
    uriEditor.setAsText("classpath:test.txt");
    Object value = uriEditor.getValue();
    boolean condition = value instanceof URI;
    assertThat(condition).isTrue();
    URI uri = (URI) value;
    assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
    assertThat(uri.getScheme().startsWith("classpath")).isTrue();
  }

  @Test
  public void setAsTextWithNull() throws Exception {
    PropertyEditor uriEditor = new URIEditor();
    uriEditor.setAsText(null);
    assertThat(uriEditor.getValue()).isNull();
    assertThat(uriEditor.getAsText()).isEqualTo("");
  }

  @Test
  public void getAsTextReturnsEmptyStringIfValueNotSet() throws Exception {
    PropertyEditor uriEditor = new URIEditor();
    assertThat(uriEditor.getAsText()).isEqualTo("");
  }

  @Test
  public void encodeURI() throws Exception {
    PropertyEditor uriEditor = new URIEditor();
    uriEditor.setAsText("https://example.com/spaces and \u20AC");
    Object value = uriEditor.getValue();
    boolean condition = value instanceof URI;
    assertThat(condition).isTrue();
    URI uri = (URI) value;
    assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
    assertThat(uri.toASCIIString()).isEqualTo("https://example.com/spaces%20and%20%E2%82%AC");
  }

  @Test
  public void encodeAlreadyEncodedURI() throws Exception {
    PropertyEditor uriEditor = new URIEditor(false);
    uriEditor.setAsText("https://example.com/spaces%20and%20%E2%82%AC");
    Object value = uriEditor.getValue();
    boolean condition = value instanceof URI;
    assertThat(condition).isTrue();
    URI uri = (URI) value;
    assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
    assertThat(uri.toASCIIString()).isEqualTo("https://example.com/spaces%20and%20%E2%82%AC");
  }

  private void doTestURI(String uriSpec) {
    PropertyEditor uriEditor = new URIEditor();
    uriEditor.setAsText(uriSpec);
    Object value = uriEditor.getValue();
    boolean condition = value instanceof URI;
    assertThat(condition).isTrue();
    URI uri = (URI) value;
    assertThat(uri.toString()).isEqualTo(uriSpec);
  }

}
