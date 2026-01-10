/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.utils;

import org.junit.jupiter.api.Test;

import infra.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UnderscoreToCamelCaseTests {
  @Test
  public void testBasicConversions() {
    assertEquals("myStringVariable", StringUtils.underscoreToCamelCase("my_string_variable"));
    assertEquals("string", StringUtils.underscoreToCamelCase("string"));
    assertEquals("myReallyLongStringVariableName", StringUtils.underscoreToCamelCase("my_really_long_string_variable_name"));
    assertEquals("myString2WithNumbers4", StringUtils.underscoreToCamelCase("my_string2_with_numbers_4"));
    assertEquals("myStringWithMixedCase", StringUtils.underscoreToCamelCase("my_string_with_MixED_CaSe"));
  }

  @Test
  public void testNullString() {
    assertNull(StringUtils.underscoreToCamelCase(null));
  }

  @Test
  public void testEmptyStrings() {
    assertEquals("", StringUtils.underscoreToCamelCase(""));
    assertEquals(" ", StringUtils.underscoreToCamelCase(" "));
  }

  @Test
  public void testWhitespace() {
    assertEquals("\t", StringUtils.underscoreToCamelCase("\t"));
    assertEquals("\n\n", StringUtils.underscoreToCamelCase("\n\n"));
  }
}
