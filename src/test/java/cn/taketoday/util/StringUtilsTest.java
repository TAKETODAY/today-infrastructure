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

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import cn.taketoday.core.Constant;

import static cn.taketoday.util.StringUtils.collectionToString;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Today <br>
 * 
 *         2018-12-10 19:06
 */
public class StringUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void test_IsEmpty() {
        assert !StringUtils.isEmpty("1234");
        assert !StringUtils.isEmpty(" ");
        assert StringUtils.isEmpty("");
        assert StringUtils.isEmpty(null);
    }

    @Test
    public void test_IsNotEmpty() {
        assert !StringUtils.isArrayNotEmpty((String[]) null);
        assert !StringUtils.isNotEmpty("");
        assert StringUtils.isNotEmpty(" ");
        assert StringUtils.isNotEmpty("1333r");
    }

    @Test
    public void test_Split() {

        String split[] = StringUtils.split("today;yhj,take");
        assert split.length == 3;
        assert split[0].equals("today");
        assert split[1].equals("yhj");
        assert split[2].equals("take");

        String split_[] = StringUtils.split("todayyhjtake;");
        assert split_.length == 1;
        assert split_[0].equals("todayyhjtake");

        assert StringUtils.split(null) == Constant.EMPTY_STRING_ARRAY;

        assert !StringUtils.isArrayEmpty(split_);
        assert StringUtils.isArrayNotEmpty(split_);
        assert !StringUtils.isArrayNotEmpty();
        assert StringUtils.isArrayEmpty();
    }

    @Test
    public void testArrayToString() {

        String split[] = StringUtils.split("today;yhj,take");
        assert StringUtils.arrayToString(split).equals("today,yhj,take");
        assert StringUtils.arrayToString(null) == null;

        assert StringUtils.arrayToString(new String[] { "today" }).equals("today");
    }

    @Test
    public void testCheckPropertiesName() {
        assert StringUtils.checkPropertiesName("info").equals("info.properties");
        assert StringUtils.checkPropertiesName("info.properties").equals("info.properties");
        StringUtils.getUUIDString();
    }

    @Test
    public void testCleanPath() {

        assert StringUtils.cleanPath(null) == (null);
        assert StringUtils.cleanPath("").equals("");
        assert StringUtils.cleanPath("C:\\test\\").equals("C:/test/");
    }

    @Test
    public void testDecodeUrl() {
        assert "四川".equals(StringUtils.decodeUrl("%e5%9b%9b%e5%b7%9d"));
    }

    @Test
    public void testEncodeUrl() {

        assert StringUtils.encodeUrl("四川").equalsIgnoreCase("%e5%9b%9b%e5%b7%9d");
    }

    @Test
    public void testTokenizeToStringArray() {

        final String[] tokenizeToStringArray = StringUtils.tokenizeToStringArray("i,take,today", ",");
        assert tokenizeToStringArray.length == 3;

        final String[] tokenizeToStringArray2 = StringUtils.tokenizeToStringArray("i;take;today", ";");
        assert tokenizeToStringArray2.length == 3;
        assert tokenizeToStringArray.length == tokenizeToStringArray2.length;

        assert StringUtils.tokenizeToStringArray(null, null) == Constant.EMPTY_STRING_ARRAY;

    }

    @Test
    public void testListToString() {

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
    public void hasTextBlank() {
        String blank = "          ";
        assertThat(StringUtils.hasText(blank)).isEqualTo(false);
    }

    @Test
    public void hasTextNullEmpty() {
        assertThat(StringUtils.hasText(null)).isEqualTo(false);
        assertThat(StringUtils.hasText("")).isEqualTo(false);
    }

    @Test
    public void deleteAny() {
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
    public void getFilename() {
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
    public void getFilenameExtension() {
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
    public void cleanPath() {
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
    }

    @Test
    public void concatenateStringArrays() {
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
    public void testCapitalize() {
        assertEquals(StringUtils.capitalize("java"), "Java");
        assertEquals(StringUtils.capitalize("Java"), "Java");
        
        assertEquals(StringUtils.uncapitalize("java"), "java");
        assertEquals(StringUtils.uncapitalize("Java"), "java");
        
        assertEquals(StringUtils.changeFirstCharacterCase("Java", false), "java");
        assertEquals(StringUtils.changeFirstCharacterCase("Java", true), "Java");
        assertEquals(StringUtils.changeFirstCharacterCase(null, true), null);
        assertEquals(StringUtils.changeFirstCharacterCase("", true), "");
        
    }

}
