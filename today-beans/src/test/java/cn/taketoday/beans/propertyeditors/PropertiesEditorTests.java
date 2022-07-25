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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the conversion of Strings to {@link Properties} objects,
 * and other property editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class PropertiesEditorTests {

  @Test
  public void oneProperty() {
    String s = "foo=bar";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 1).as("contains one entry").isTrue();
    assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
  }

  @Test
  public void twoProperties() {
    String s = "foo=bar with whitespace\n" +
            "me=mi";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 2).as("contains two entries").isTrue();
    assertThat(p.get("foo").equals("bar with whitespace")).as("foo=bar with whitespace").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
  }

  @Test
  public void handlesEqualsInValue() {
    String s = "foo=bar\n" +
            "me=mi\n" +
            "x=y=z";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 3).as("contains two entries").isTrue();
    assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
    assertThat(p.get("x").equals("y=z")).as("x='y=z'").isTrue();
  }

  @Test
  public void handlesEmptyProperty() {
    String s = "foo=bar\nme=mi\nx=";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 3).as("contains two entries").isTrue();
    assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
    assertThat(p.get("x").equals("")).as("x='y=z'").isTrue();
  }

  @Test
  public void handlesEmptyPropertyWithoutEquals() {
    String s = "foo\nme=mi\nx=x";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 3).as("contains three entries").isTrue();
    assertThat(p.get("foo").equals("")).as("foo is empty").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
  }

  /**
   * Comments begin with #
   */
  @Test
  public void ignoresCommentLinesAndEmptyLines() {
    String s = "#Ignore this comment\n" +
            "foo=bar\n" +
            "#Another=comment more junk /\n" +
            "me=mi\n" +
            "x=x\n" +
            "\n";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.entrySet().size() == 3).as("contains three entries").isTrue();
    assertThat(p.get("foo").equals("bar")).as("foo is bar").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
  }

  /**
   * We'll typically align by indenting with tabs or spaces.
   * These should be ignored if at the beginning of a line.
   * We must ensure that comment lines beginning with whitespace are
   * still ignored: The standard syntax doesn't allow this on JDK 1.3.
   */
  @Test
  public void ignoresLeadingSpacesAndTabs() {
    String s = "    #Ignore this comment\n" +
            "\t\tfoo=bar\n" +
            "\t#Another comment more junk \n" +
            " me=mi\n" +
            "x=x\n" +
            "\n";
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(s);
    Properties p = (Properties) pe.getValue();
    assertThat(p.size() == 3).as("contains 3 entries, not " + p.size()).isTrue();
    assertThat(p.get("foo").equals("bar")).as("foo is bar").isTrue();
    assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
  }

  @Test
  public void nullValue() {
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText(null);
    Properties p = (Properties) pe.getValue();
    assertThat(p.size()).isEqualTo(0);
  }

  @Test
  public void emptyString() {
    PropertiesEditor pe = new PropertiesEditor();
    pe.setAsText("");
    Properties p = (Properties) pe.getValue();
    assertThat(p.isEmpty()).as("empty string means empty properties").isTrue();
  }

  @Test
  public void usingMapAsValueSource() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("one", "1");
    map.put("two", "2");
    map.put("three", "3");
    PropertiesEditor pe = new PropertiesEditor();
    pe.setValue(map);
    Object value = pe.getValue();
    assertThat(value).isNotNull();
    boolean condition = value instanceof Properties;
    assertThat(condition).isTrue();
    Properties props = (Properties) value;
    assertThat(props.size()).isEqualTo(3);
    assertThat(props.getProperty("one")).isEqualTo("1");
    assertThat(props.getProperty("two")).isEqualTo("2");
    assertThat(props.getProperty("three")).isEqualTo("3");
  }

}
