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

package cn.taketoday.util.xml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static cn.taketoday.util.xml.XmlValidationModeDetector.VALIDATION_DTD;
import static cn.taketoday.util.xml.XmlValidationModeDetector.VALIDATION_XSD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link XmlValidationModeDetector}.
 *
 * @author Sam Brannen
 */
class XmlValidationModeDetectorTests {

  private final XmlValidationModeDetector xmlValidationModeDetector = new XmlValidationModeDetector();

  @ParameterizedTest
  @ValueSource(strings = {
          "dtdWithNoComments.xml",
          "dtdWithLeadingComment.xml",
          "dtdWithTrailingComment.xml",
          "dtdWithTrailingCommentAcrossMultipleLines.xml",
          "dtdWithCommentOnNextLine.xml",
          "dtdWithMultipleComments.xml"
  })
  void dtdDetection(String fileName) throws Exception {
    assertValidationMode(fileName, VALIDATION_DTD);
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "xsdWithNoComments.xml",
          "xsdWithMultipleComments.xml",
          "xsdWithDoctypeInComment.xml",
          "xsdWithDoctypeInOpenCommentWithAdditionalCommentOnSameLine.xml"
  })
  void xsdDetection(String fileName) throws Exception {
    assertValidationMode(fileName, VALIDATION_XSD);
  }

  private void assertValidationMode(String fileName, int expectedValidationMode) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
      assertThat(xmlValidationModeDetector.detectValidationMode(inputStream))
              .as("Validation Mode")
              .isEqualTo(expectedValidationMode);
    }
  }

}
