package cn.taketoday.jdbc.utils;

import org.junit.jupiter.api.Test;

import cn.taketoday.util.StringUtils;

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
