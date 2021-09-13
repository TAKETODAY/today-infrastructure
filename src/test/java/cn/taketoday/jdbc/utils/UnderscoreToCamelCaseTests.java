package cn.taketoday.jdbc.utils;

import junit.framework.TestCase;

import cn.taketoday.util.StringUtils;

public class UnderscoreToCamelCaseTests extends TestCase {

  public void testBasicConversions() {
    assertEquals("myStringVariable", StringUtils.underscoreToCamelCase("my_string_variable"));
    assertEquals("string", StringUtils.underscoreToCamelCase("string"));
    assertEquals("myReallyLongStringVariableName", StringUtils.underscoreToCamelCase("my_really_long_string_variable_name"));
    assertEquals("myString2WithNumbers4", StringUtils.underscoreToCamelCase("my_string2_with_numbers_4"));
    assertEquals("myStringWithMixedCase", StringUtils.underscoreToCamelCase("my_string_with_MixED_CaSe"));
  }

  public void testNullString() {
    assertNull(StringUtils.underscoreToCamelCase(null));
  }

  public void testEmptyStrings() {
    assertEquals("", StringUtils.underscoreToCamelCase(""));
    assertEquals(" ", StringUtils.underscoreToCamelCase(" "));
  }

  public void testWhitespace() {
    assertEquals("\t", StringUtils.underscoreToCamelCase("\t"));
    assertEquals("\n\n", StringUtils.underscoreToCamelCase("\n\n"));
  }
}
