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
