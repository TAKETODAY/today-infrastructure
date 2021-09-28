/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import cn.taketoday.core.Constant;

import static cn.taketoday.util.StringUtils.collectionToString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Today <br>
 *
 * 2018-12-10 19:06
 */
class StringUtilsTest {

  @Test
  void test_IsEmpty() {
    assert !StringUtils.isEmpty("1234");
    assert !StringUtils.isEmpty(" ");
    assert StringUtils.isEmpty("");
    assert StringUtils.isEmpty(null);
  }

  @Test
  void test_IsNotEmpty() {
    assert !StringUtils.isNotEmpty("");
    assert StringUtils.isNotEmpty(" ");
    assert StringUtils.isNotEmpty("1333r");
  }

  @Test
  void test_Split() {

    String split[] = StringUtils.split("today;yhj,take");
    assert split.length == 3;
    assert split[0].equals("today");
    assert split[1].equals("yhj");
    assert split[2].equals("take");

    String split_[] = StringUtils.split("todayyhjtake;");
    assert split_.length == 1;
    assert split_[0].equals("todayyhjtake");

    assert StringUtils.split(null) == Constant.EMPTY_STRING_ARRAY;

  }

  @Test
  void testArrayToString() {

    String[] split = StringUtils.split("today;yhj,take");
    assert StringUtils.arrayToString(split).equals("today,yhj,take");
    assert StringUtils.arrayToString(null) == null;

    assert StringUtils.arrayToString(new String[] { "today" }).equals("today");
  }

  @Test
  void testCheckPropertiesName() {
    assert StringUtils.checkPropertiesName("info").equals("info.properties");
    assert StringUtils.checkPropertiesName("info.properties").equals("info.properties");
    StringUtils.getUUIDString();
  }

  @Test
  void testCleanPath() {

    assert StringUtils.cleanPath(null) == (null);
    assert StringUtils.cleanPath("").equals("");
    assert StringUtils.cleanPath("C:\\test\\").equals("C:/test/");
  }

  @Test
  void testDecodeUrl() {
    assert "四川".equals(StringUtils.decodeURL("%e5%9b%9b%e5%b7%9d"));
  }

  @Test
  void testEncodeUrl() {

    assert StringUtils.encodeURL("四川").equalsIgnoreCase("%e5%9b%9b%e5%b7%9d");
  }

  @Test
  void testTokenizeToStringArray() {

    final String[] tokenizeToStringArray = StringUtils.tokenizeToStringArray("i,take,today", ",");
    assert tokenizeToStringArray.length == 3;

    final String[] tokenizeToStringArray2 = StringUtils.tokenizeToStringArray("i;take;today", ";");
    assert tokenizeToStringArray2.length == 3;
    assert tokenizeToStringArray.length == tokenizeToStringArray2.length;

    assert StringUtils.tokenizeToStringArray(null, null) == Constant.EMPTY_STRING_ARRAY;

  }

  @Test
  void testListToString() {

    final List<String> asList = asList("i", "take", "today");
    final String listToString = collectionToString(asList);
    assertEquals(listToString, "i,take,today");

    assertEquals(listToString, collectionToString(asList, ","));

    assertEquals(collectionToString(asList("i"), ","), "i");

    assertNull(collectionToString(null));

    //Set
    assertEquals(collectionToString(new HashSet<String>() {
      private static final long serialVersionUID = 1L;

      {
        add("i");
        add("take");
        add("today");
      }
    }).length(), "i,take,today".length());

  }

  @Test
  void hasTextBlank() {
    String blank = "          ";
    assertThat(StringUtils.hasText(blank)).isEqualTo(false);
  }

  @Test
  void hasTextNullEmpty() {
    assertThat(StringUtils.hasText(null)).isEqualTo(false);
    assertThat(StringUtils.hasText("")).isEqualTo(false);
  }

  @Test
  void deleteAny() {
    String inString = "Able was I ere I saw Elba";

    String res = StringUtils.deleteAny(inString, "I");
    assertThat(res.equals("Able was  ere  saw Elba")).as("Result has no Is [" + res + "]").isTrue();

    res = StringUtils.deleteAny(inString, "AeEba!");
    assertThat(res.equals("l ws I r I sw l")).as("Result has no Is [" + res + "]").isTrue();

    String mismatch = StringUtils.deleteAny(inString, "#@$#$^");
    assertThat(mismatch.equals(inString)).as("Result is unchanged").isTrue();

    String whitespace = "This is\n\n\n    \t   a messagy string with whitespace\n";
    assertThat(whitespace.contains("\n")).as("Has CR").isTrue();
    assertThat(whitespace.contains("\t")).as("Has tab").isTrue();
    assertThat(whitespace.contains(" ")).as("Has  sp").isTrue();
    String cleaned = StringUtils.deleteAny(whitespace, "\n\t ");
    boolean condition2 = !cleaned.contains("\n");
    assertThat(condition2).as("Has no CR").isTrue();
    boolean condition1 = !cleaned.contains("\t");
    assertThat(condition1).as("Has no tab").isTrue();
    boolean condition = !cleaned.contains(" ");
    assertThat(condition).as("Has no sp").isTrue();
    assertThat(cleaned.length() > 10).as("Still has chars").isTrue();
  }

  @Test
  void getFilename() {
    assertThat(StringUtils.getFilename(null)).isEqualTo(null);
    assertThat(StringUtils.getFilename("")).isEqualTo("");
    assertThat(StringUtils.getFilename("myfile")).isEqualTo("myfile");
    assertThat(StringUtils.getFilename("mypath/myfile")).isEqualTo("myfile");
    assertThat(StringUtils.getFilename("myfile.")).isEqualTo("myfile.");
    assertThat(StringUtils.getFilename("mypath/myfile.")).isEqualTo("myfile.");
    assertThat(StringUtils.getFilename("myfile.txt")).isEqualTo("myfile.txt");
    assertThat(StringUtils.getFilename("mypath/myfile.txt")).isEqualTo("myfile.txt");
  }

  @Test
  void cleanPath() {
    assertThat(StringUtils.cleanPath("mypath/myfile")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.cleanPath("mypath\\myfile")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.cleanPath("mypath/../mypath/myfile")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.cleanPath("mypath/myfile/../../mypath/myfile")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.cleanPath("../mypath/myfile")).isEqualTo("../mypath/myfile");
    assertThat(StringUtils.cleanPath("../mypath/../mypath/myfile")).isEqualTo("../mypath/myfile");
    assertThat(StringUtils.cleanPath("mypath/../../mypath/myfile")).isEqualTo("../mypath/myfile");
    assertThat(StringUtils.cleanPath("/../mypath/myfile")).isEqualTo("/../mypath/myfile");
    assertThat(StringUtils.cleanPath("/a/:b/../../mypath/myfile")).isEqualTo("/mypath/myfile");
    assertThat(StringUtils.cleanPath("/")).isEqualTo("/");
    assertThat(StringUtils.cleanPath("/mypath/../")).isEqualTo("/");
    assertThat(StringUtils.cleanPath("mypath/..")).isEqualTo("");
    assertThat(StringUtils.cleanPath("mypath/../.")).isEqualTo("");
    assertThat(StringUtils.cleanPath("mypath/../")).isEqualTo("./");
    assertThat(StringUtils.cleanPath("././")).isEqualTo("./");
    assertThat(StringUtils.cleanPath("./")).isEqualTo("./");
    assertThat(StringUtils.cleanPath("../")).isEqualTo("../");
    assertThat(StringUtils.cleanPath("./../")).isEqualTo("../");
    assertThat(StringUtils.cleanPath(".././")).isEqualTo("../");
    assertThat(StringUtils.cleanPath("file:/")).isEqualTo("file:/");
    assertThat(StringUtils.cleanPath("file:/mypath/../")).isEqualTo("file:/");
    assertThat(StringUtils.cleanPath("file:mypath/..")).isEqualTo("file:");
    assertThat(StringUtils.cleanPath("file:mypath/../.")).isEqualTo("file:");
    assertThat(StringUtils.cleanPath("file:mypath/../")).isEqualTo("file:./");
    assertThat(StringUtils.cleanPath("file:././")).isEqualTo("file:./");
    assertThat(StringUtils.cleanPath("file:./")).isEqualTo("file:./");
    assertThat(StringUtils.cleanPath("file:../")).isEqualTo("file:../");
    assertThat(StringUtils.cleanPath("file:./../")).isEqualTo("file:../");
    assertThat(StringUtils.cleanPath("file:.././")).isEqualTo("file:../");
    assertThat(StringUtils.cleanPath("file:/mypath/spring.factories")).isEqualTo("file:/mypath/spring.factories");
    assertThat(StringUtils.cleanPath("file:///c:/some/../path/the%20file.txt")).isEqualTo("file:///c:/path/the%20file.txt");
    assertThat(StringUtils.cleanPath("jar:file:///c:\\some\\..\\path\\.\\the%20file.txt")).isEqualTo("jar:file:///c:/path/the%20file.txt");
    assertThat(StringUtils.cleanPath("jar:file:///c:/some/../path/./the%20file.txt")).isEqualTo("jar:file:///c:/path/the%20file.txt");

  }

  @Test
  void concatenateStringArrays() {
    String[] input1 = new String[] { "myString2" };
    String[] input2 = new String[] { "myString1", "myString2" };
    String[] result = StringUtils.concatenateStringArrays(input1, input2);
    assertThat(result.length).isEqualTo(3);
    assertThat(result[0]).isEqualTo("myString2");
    assertThat(result[1]).isEqualTo("myString1");
    assertThat(result[2]).isEqualTo("myString2");

    assertThat(StringUtils.concatenateStringArrays(input1, null)).isEqualTo(input1);
    assertThat(StringUtils.concatenateStringArrays(null, input2)).isEqualTo(input2);
    assertThat(StringUtils.concatenateStringArrays(null, null)).isNull();
  }

  @Test
  void testCapitalize() {
    assertEquals(StringUtils.capitalize("java"), "Java");
    assertEquals(StringUtils.capitalize("Java"), "Java");

    assertEquals(StringUtils.uncapitalize("java"), "java");
    assertEquals(StringUtils.uncapitalize("Java"), "java");

    assertEquals(StringUtils.changeFirstCharacterCase("Java", false), "java");
    assertEquals(StringUtils.changeFirstCharacterCase("Java", true), "Java");
    assertEquals(StringUtils.changeFirstCharacterCase(null, true), null);
    assertEquals(StringUtils.changeFirstCharacterCase("", true), "");

  }

  @Test
  void uncapitalize() {
    String capitalized = "I am capitalized";
    assertThat(StringUtils.uncapitalize(capitalized)).isEqualTo("i am capitalized");
  }

  @Test
  void getFilenameExtension() {
    assertThat(StringUtils.getFilenameExtension(null)).isEqualTo(null);
    assertThat(StringUtils.getFilenameExtension("")).isEqualTo(null);
    assertThat(StringUtils.getFilenameExtension("myfile")).isEqualTo(null);
    assertThat(StringUtils.getFilenameExtension("myPath/myfile")).isEqualTo(null);
    assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile")).isEqualTo(null);
    assertThat(StringUtils.getFilenameExtension("myfile.")).isEqualTo("");
    assertThat(StringUtils.getFilenameExtension("myPath/myfile.")).isEqualTo("");
    assertThat(StringUtils.getFilenameExtension("myfile.txt")).isEqualTo("txt");
    assertThat(StringUtils.getFilenameExtension("mypath/myfile.txt")).isEqualTo("txt");
    assertThat(StringUtils.getFilenameExtension("/home/user/.m2/settings/myfile.txt")).isEqualTo("txt");

  }

  @Test
  void stripFilenameExtension() {
    assertThat(StringUtils.stripFilenameExtension("")).isEqualTo("");
    assertThat(StringUtils.stripFilenameExtension("myfile")).isEqualTo("myfile");
    assertThat(StringUtils.stripFilenameExtension("myfile.")).isEqualTo("myfile");
    assertThat(StringUtils.stripFilenameExtension("myfile.txt")).isEqualTo("myfile");
    assertThat(StringUtils.stripFilenameExtension("mypath/myfile")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.stripFilenameExtension("mypath/myfile.")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.stripFilenameExtension("mypath/myfile.txt")).isEqualTo("mypath/myfile");
    assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile")).isEqualTo("/home/user/.m2/settings/myfile");
    assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.")).isEqualTo("/home/user/.m2/settings/myfile");
    assertThat(StringUtils.stripFilenameExtension("/home/user/.m2/settings/myfile.txt")).isEqualTo("/home/user/.m2/settings/myfile");
  }

  @Test
  void pathEquals() {
    assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for the same strings").isTrue();
    assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\dummy2\\dummy3")).as("Must be true for the same win strings").isTrue();
    assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for one top path on 1").isTrue();
    assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\dummy3")).as("Must be true for one win top path on 2").isTrue();
    assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/bin/../dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for two top paths on 1").isTrue();
    assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\..\\dummy2\\bin\\..\\dummy3")).as("Must be true for two win top paths on 2").isTrue();
    assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be true for double top paths on 1").isTrue();
    assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dum/dum/../../dummy2/dummy3")).as("Must be true for double top paths on 2 with similarity").isTrue();
    assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be true for current paths").isTrue();
    assertThat(StringUtils.pathEquals("./dummy1/dummy2/dummy3", "/dummy1/dum/./dum/../../dummy2/dummy3")).as("Must be false for relative/absolute paths").isFalse();
    assertThat(StringUtils.pathEquals("/dummy1/dummy2/dummy3", "/dummy1/dummy4/dummy3")).as("Must be false for different strings").isFalse();
    assertThat(StringUtils.pathEquals("/dummy1/bin/tmp/../dummy2/dummy3", "/dummy1/dummy2/dummy3")).as("Must be false for one false path on 1").isFalse();
    assertThat(StringUtils.pathEquals("C:\\dummy1\\dummy2\\dummy3", "C:\\dummy1\\bin\\tmp\\..\\dummy2\\dummy3")).as("Must be false for one false win top path on 2").isFalse();
    assertThat(StringUtils.pathEquals("/dummy1/bin/../dummy2/dummy3", "/dummy1/dummy2/dummy4")).as("Must be false for top path on 1 + difference").isFalse();
  }

  @Test
  void sortStringArray() {
    String[] input = new String[] { "myString2" };
    input = StringUtils.addStringToArray(input, "myString1");
    assertThat(input[0]).isEqualTo("myString2");
    assertThat(input[1]).isEqualTo("myString1");

    StringUtils.sortArray(input);
    assertThat(input[0]).isEqualTo("myString1");
    assertThat(input[1]).isEqualTo("myString2");
  }

  @Test
  void removeDuplicateStrings() {
    String[] input = new String[] { "myString2", "myString1", "myString2" };
    input = StringUtils.removeDuplicateStrings(input);
    assertThat(input[0]).isEqualTo("myString2");
    assertThat(input[1]).isEqualTo("myString1");
  }

  //

  @Test
  void hasTextValid() {
    assertThat(StringUtils.hasText("t")).isEqualTo(true);
  }

  @Test
  void containsWhitespace() {
    assertThat(StringUtils.containsWhitespace(null)).isFalse();
    assertThat(StringUtils.containsWhitespace("")).isFalse();
    assertThat(StringUtils.containsWhitespace("a")).isFalse();
    assertThat(StringUtils.containsWhitespace("abc")).isFalse();
    assertThat(StringUtils.containsWhitespace(" ")).isTrue();
    assertThat(StringUtils.containsWhitespace("\t")).isTrue();
    assertThat(StringUtils.containsWhitespace("\n")).isTrue();
    assertThat(StringUtils.containsWhitespace(" a")).isTrue();
    assertThat(StringUtils.containsWhitespace("abc ")).isTrue();
    assertThat(StringUtils.containsWhitespace("a b")).isTrue();
    assertThat(StringUtils.containsWhitespace("a  b")).isTrue();
  }

  @Test
  void trimWhitespace() {
    assertThat(StringUtils.trimWhitespace(null)).isEqualTo(null);
    assertThat(StringUtils.trimWhitespace("")).isEqualTo("");
    assertThat(StringUtils.trimWhitespace(" ")).isEqualTo("");
    assertThat(StringUtils.trimWhitespace("\t")).isEqualTo("");
    assertThat(StringUtils.trimWhitespace("\n")).isEqualTo("");
    assertThat(StringUtils.trimWhitespace(" \t\n")).isEqualTo("");
    assertThat(StringUtils.trimWhitespace(" a")).isEqualTo("a");
    assertThat(StringUtils.trimWhitespace("a ")).isEqualTo("a");
    assertThat(StringUtils.trimWhitespace(" a ")).isEqualTo("a");
    assertThat(StringUtils.trimWhitespace(" a b ")).isEqualTo("a b");
    assertThat(StringUtils.trimWhitespace(" a b  c ")).isEqualTo("a b  c");
  }

  @Test
  void trimAllWhitespace() {
    assertThat(StringUtils.trimAllWhitespace(null)).isEqualTo(null);
    assertThat(StringUtils.trimAllWhitespace("")).isEqualTo("");
    assertThat(StringUtils.trimAllWhitespace(" ")).isEqualTo("");
    assertThat(StringUtils.trimAllWhitespace("\t")).isEqualTo("");
    assertThat(StringUtils.trimAllWhitespace("\n")).isEqualTo("");
    assertThat(StringUtils.trimAllWhitespace(" \t\n")).isEqualTo("");
    assertThat(StringUtils.trimAllWhitespace(" a")).isEqualTo("a");
    assertThat(StringUtils.trimAllWhitespace("a ")).isEqualTo("a");
    assertThat(StringUtils.trimAllWhitespace(" a ")).isEqualTo("a");
    assertThat(StringUtils.trimAllWhitespace(" a b ")).isEqualTo("ab");
    assertThat(StringUtils.trimAllWhitespace(" a b  c ")).isEqualTo("abc");
  }

  @Test
  void trimLeadingWhitespace() {
    assertThat(StringUtils.trimLeadingWhitespace(null)).isEqualTo(null);
    assertThat(StringUtils.trimLeadingWhitespace("")).isEqualTo("");
    assertThat(StringUtils.trimLeadingWhitespace(" ")).isEqualTo("");
    assertThat(StringUtils.trimLeadingWhitespace("\t")).isEqualTo("");
    assertThat(StringUtils.trimLeadingWhitespace("\n")).isEqualTo("");
    assertThat(StringUtils.trimLeadingWhitespace(" \t\n")).isEqualTo("");
    assertThat(StringUtils.trimLeadingWhitespace(" a")).isEqualTo("a");
    assertThat(StringUtils.trimLeadingWhitespace("a ")).isEqualTo("a ");
    assertThat(StringUtils.trimLeadingWhitespace(" a ")).isEqualTo("a ");
    assertThat(StringUtils.trimLeadingWhitespace(" a b ")).isEqualTo("a b ");
    assertThat(StringUtils.trimLeadingWhitespace(" a b  c ")).isEqualTo("a b  c ");
  }

  @Test
  void trimTrailingWhitespace() {
    assertThat(StringUtils.trimTrailingWhitespace(null)).isEqualTo(null);
    assertThat(StringUtils.trimTrailingWhitespace("")).isEqualTo("");
    assertThat(StringUtils.trimTrailingWhitespace(" ")).isEqualTo("");
    assertThat(StringUtils.trimTrailingWhitespace("\t")).isEqualTo("");
    assertThat(StringUtils.trimTrailingWhitespace("\n")).isEqualTo("");
    assertThat(StringUtils.trimTrailingWhitespace(" \t\n")).isEqualTo("");
    assertThat(StringUtils.trimTrailingWhitespace("a ")).isEqualTo("a");
    assertThat(StringUtils.trimTrailingWhitespace(" a")).isEqualTo(" a");
    assertThat(StringUtils.trimTrailingWhitespace(" a ")).isEqualTo(" a");
    assertThat(StringUtils.trimTrailingWhitespace(" a b ")).isEqualTo(" a b");
    assertThat(StringUtils.trimTrailingWhitespace(" a b  c ")).isEqualTo(" a b  c");
  }

  @Test
  void trimLeadingCharacter() {
    assertThat(StringUtils.trimLeadingCharacter(null, ' ')).isEqualTo(null);
    assertThat(StringUtils.trimLeadingCharacter("", ' ')).isEqualTo("");
    assertThat(StringUtils.trimLeadingCharacter(" ", ' ')).isEqualTo("");
    assertThat(StringUtils.trimLeadingCharacter("\t", ' ')).isEqualTo("\t");
    assertThat(StringUtils.trimLeadingCharacter(" a", ' ')).isEqualTo("a");
    assertThat(StringUtils.trimLeadingCharacter("a ", ' ')).isEqualTo("a ");
    assertThat(StringUtils.trimLeadingCharacter(" a ", ' ')).isEqualTo("a ");
    assertThat(StringUtils.trimLeadingCharacter(" a b ", ' ')).isEqualTo("a b ");
    assertThat(StringUtils.trimLeadingCharacter(" a b  c ", ' ')).isEqualTo("a b  c ");
  }

  @Test
  void trimTrailingCharacter() {
    assertThat(StringUtils.trimTrailingCharacter(null, ' ')).isEqualTo(null);
    assertThat(StringUtils.trimTrailingCharacter("", ' ')).isEqualTo("");
    assertThat(StringUtils.trimTrailingCharacter(" ", ' ')).isEqualTo("");
    assertThat(StringUtils.trimTrailingCharacter("\t", ' ')).isEqualTo("\t");
    assertThat(StringUtils.trimTrailingCharacter("a ", ' ')).isEqualTo("a");
    assertThat(StringUtils.trimTrailingCharacter(" a", ' ')).isEqualTo(" a");
    assertThat(StringUtils.trimTrailingCharacter(" a ", ' ')).isEqualTo(" a");
    assertThat(StringUtils.trimTrailingCharacter(" a b ", ' ')).isEqualTo(" a b");
    assertThat(StringUtils.trimTrailingCharacter(" a b  c ", ' ')).isEqualTo(" a b  c");
  }

  @Test
  void startsWithIgnoreCase() {
    String prefix = "fOo";
    assertThat(StringUtils.startsWithIgnoreCase("foo", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("Foo", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("foobar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("foobarbar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("Foobar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("FoobarBar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("foObar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("FOObar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase("fOobar", prefix)).isTrue();
    assertThat(StringUtils.startsWithIgnoreCase(null, prefix)).isFalse();
    assertThat(StringUtils.startsWithIgnoreCase("fOobar", null)).isFalse();
    assertThat(StringUtils.startsWithIgnoreCase("b", prefix)).isFalse();
    assertThat(StringUtils.startsWithIgnoreCase("barfoo", prefix)).isFalse();
    assertThat(StringUtils.startsWithIgnoreCase("barfoobar", prefix)).isFalse();
  }

  @Test
  void endsWithIgnoreCase() {
    String suffix = "fOo";
    assertThat(StringUtils.endsWithIgnoreCase("foo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("Foo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barfoo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barbarfoo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barFoo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barBarFoo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barfoO", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barFOO", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase("barfOo", suffix)).isTrue();
    assertThat(StringUtils.endsWithIgnoreCase(null, suffix)).isFalse();
    assertThat(StringUtils.endsWithIgnoreCase("barfOo", null)).isFalse();
    assertThat(StringUtils.endsWithIgnoreCase("b", suffix)).isFalse();
    assertThat(StringUtils.endsWithIgnoreCase("foobar", suffix)).isFalse();
    assertThat(StringUtils.endsWithIgnoreCase("barfoobar", suffix)).isFalse();
  }

  @Test
  void substringMatch() {
    assertThat(StringUtils.substringMatch("foo", 0, "foo")).isTrue();
    assertThat(StringUtils.substringMatch("foo", 1, "oo")).isTrue();
    assertThat(StringUtils.substringMatch("foo", 2, "o")).isTrue();
    assertThat(StringUtils.substringMatch("foo", 0, "fOo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 1, "fOo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 2, "fOo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 3, "fOo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 1, "Oo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 2, "Oo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 3, "Oo")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 2, "O")).isFalse();
    assertThat(StringUtils.substringMatch("foo", 3, "O")).isFalse();
  }
  //

  @Test
  void parseLocaleStringSunnyDay() {
    Locale expectedLocale = Locale.UK;
    Locale locale = StringUtils.parseLocaleString(expectedLocale.toString());
    assertThat(locale).as("When given a bona-fide Locale string, must not return null.").isNotNull();
    assertThat(locale).isEqualTo(expectedLocale);
  }

  @Test
  void parseLocaleStringWithMalformedLocaleString() {
    Locale locale = StringUtils.parseLocaleString("_banjo_on_my_knee");
    assertThat(locale).as("When given a malformed Locale string, must not return null.").isNotNull();
  }

  @Test
  void parseLocaleStringWithEmptyLocaleStringYieldsNullLocale() {
    Locale locale = StringUtils.parseLocaleString("");
    assertThat(locale).as("When given an empty Locale string, must return null.").isNull();
  }

  @Test  // SPR-8637
  void parseLocaleWithMultiSpecialCharactersInVariant() {
    String variant = "proper-northern";
    String localeString = "en_GB_" + variant;
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-3671
  void parseLocaleWithMultiValuedVariant() {
    String variant = "proper_northern";
    String localeString = "en_GB_" + variant;
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-3671
  void parseLocaleWithMultiValuedVariantUsingSpacesAsSeparators() {
    String variant = "proper northern";
    String localeString = "en GB " + variant;
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-3671
  void parseLocaleWithMultiValuedVariantUsingMixtureOfUnderscoresAndSpacesAsSeparators() {
    String variant = "proper northern";
    String localeString = "en_GB_" + variant;
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-3671
  void parseLocaleWithMultiValuedVariantUsingSpacesAsSeparatorsWithLotsOfLeadingWhitespace() {
    String variant = "proper northern";
    String localeString = "en GB            " + variant;  // lots of whitespace
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-3671
  void parseLocaleWithMultiValuedVariantUsingUnderscoresAsSeparatorsWithLotsOfLeadingWhitespace() {
    String variant = "proper_northern";
    String localeString = "en_GB_____" + variant;  // lots of underscores
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Multi-valued variant portion of the Locale not extracted correctly.").isEqualTo(variant);
  }

  @Test  // SPR-7779
  void parseLocaleWithInvalidCharacters() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> StringUtils.parseLocaleString("%0D%0AContent-length:30%0D%0A%0D%0A%3Cscript%3Ealert%28123%29%3C/script%3E"));
  }

  @Test  // SPR-9420
  void parseLocaleWithSameLowercaseTokenForLanguageAndCountry() {
    assertThat(StringUtils.parseLocaleString("tr_tr").toString()).isEqualTo("tr_TR");
    assertThat(StringUtils.parseLocaleString("bg_bg_vnt").toString()).isEqualTo("bg_BG_vnt");
  }

  @Test  // SPR-11806
  void parseLocaleWithVariantContainingCountryCode() {
    String variant = "GBtest";
    String localeString = "en_GB_" + variant;
    Locale locale = StringUtils.parseLocaleString(localeString);
    assertThat(locale.getVariant()).as("Variant containing country code not extracted correctly").isEqualTo(variant);
  }

  @Test  // SPR-14718, SPR-7598
  void parseJava7Variant() {
    assertThat(StringUtils.parseLocaleString("sr__#LATN").toString()).isEqualTo("sr__#LATN");
  }

  @Test  // SPR-16651
  void availableLocalesWithLocaleString() {
    for (Locale locale : Locale.getAvailableLocales()) {
      Locale parsedLocale = StringUtils.parseLocaleString(locale.toString());
      if (parsedLocale == null) {
        assertThat(locale.getLanguage()).isEqualTo("");
      }
      else {
        assertThat(locale.toString()).isEqualTo(parsedLocale.toString());
      }
    }
  }

  @Test  // SPR-16651
  void availableLocalesWithLanguageTag() {
    for (Locale locale : Locale.getAvailableLocales()) {
      Locale parsedLocale = StringUtils.parseLocale(locale.toLanguageTag());
      if (parsedLocale == null) {
        assertThat(locale.getLanguage()).isEqualTo("");
      }
      else {
        assertThat(locale.toLanguageTag()).isEqualTo(parsedLocale.toLanguageTag());
      }
    }
  }

  @Test
  void invalidLocaleWithLocaleString() {
    assertThat(StringUtils.parseLocaleString("invalid")).isEqualTo(new Locale("invalid"));
    assertThat(StringUtils.parseLocaleString("invalidvalue")).isEqualTo(new Locale("invalidvalue"));
    assertThat(StringUtils.parseLocaleString("invalidvalue_foo")).isEqualTo(new Locale("invalidvalue", "foo"));
    assertThat(StringUtils.parseLocaleString("")).isNull();
  }

  @Test
  void invalidLocaleWithLanguageTag() {
    assertThat(StringUtils.parseLocale("invalid")).isEqualTo(new Locale("invalid"));
    assertThat(StringUtils.parseLocale("invalidvalue")).isEqualTo(new Locale("invalidvalue"));
    assertThat(StringUtils.parseLocale("invalidvalue_foo")).isEqualTo(new Locale("invalidvalue", "foo"));
    assertThat(StringUtils.parseLocale("")).isNull();
  }

  @Test
  void split() {
    assertThat(StringUtils.split("Hello, world", ",")).containsExactly("Hello", " world");
    assertThat(StringUtils.split(",Hello world", ",")).containsExactly("", "Hello world");
    assertThat(StringUtils.split("Hello world,", ",")).containsExactly("Hello world", "");
    assertThat(StringUtils.split("Hello, world,", ",")).containsExactly("Hello", " world,");
  }

  @Test
  void splitWithEmptyStringOrNull() {
    assertThat(StringUtils.split("Hello, world", "")).isNull();
    assertThat(StringUtils.split("", ",")).isNull();
    assertThat(StringUtils.split(null, ",")).isNull();
    assertThat(StringUtils.split("Hello, world", null)).isNull();
    assertThat(StringUtils.split(null, null)).isNull();
  }

}
