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

package cn.taketoday.test.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link XmlExpectationsHelper}.
 *
 * @author Matthew Depue
 */
class XmlExpectationsHelperTests {

  @Test
  void assertXmlEqualForEqual() throws Exception {
    String control = "<root><field1>f1</field1><field2>f2</field2></root>";
    String test = "<root><field1>f1</field1><field2>f2</field2></root>";
    XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
    xmlHelper.assertXmlEqual(control, test);
  }

  @Test
  void assertXmlEqualExceptionForIncorrectValue() throws Exception {
    String control = "<root><field1>f1</field1><field2>f2</field2></root>";
    String test = "<root><field1>notf1</field1><field2>f2</field2></root>";
    XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    xmlHelper.assertXmlEqual(control, test))
            .withMessageStartingWith("Body content Expected child 'field1'");
  }

  @Test
  void assertXmlEqualForOutOfOrder() throws Exception {
    String control = "<root><field1>f1</field1><field2>f2</field2></root>";
    String test = "<root><field2>f2</field2><field1>f1</field1></root>";
    XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
    xmlHelper.assertXmlEqual(control, test);
  }

  @Test
  void assertXmlEqualExceptionForMoreEntries() throws Exception {
    String control = "<root><field1>f1</field1><field2>f2</field2></root>";
    String test = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
    XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    xmlHelper.assertXmlEqual(control, test))
            .withMessageContaining("Expected child nodelist length '2' but was '3'");

  }

  @Test
  void assertXmlEqualExceptionForLessEntries() throws Exception {
    String control = "<root><field1>f1</field1><field2>f2</field2><field3>f3</field3></root>";
    String test = "<root><field1>f1</field1><field2>f2</field2></root>";
    XmlExpectationsHelper xmlHelper = new XmlExpectationsHelper();
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    xmlHelper.assertXmlEqual(control, test))
            .withMessageContaining("Expected child nodelist length '3' but was '2'");
  }

}
