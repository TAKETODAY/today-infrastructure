/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.core.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.conversion.ConversionException;

import static org.junit.Assert.assertEquals;

/**
 * @author Today <br>
 *         2018-07-12 20:46:41
 */
public class ObjectUtilsTest {

    private long start;

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void end() {
        System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
    }

    @Test
    public void testIsEmpty() throws ConfigurationException {
        // null
        assert ObjectUtils.isEmpty(null);
        assert !ObjectUtils.isNotEmpty(null);
        assert ObjectUtils.isEmpty((Object) null);
        assert !ObjectUtils.isNotEmpty((Object) null);

        // string array
        assert ObjectUtils.isEmpty(new String[0]);
        assert !ObjectUtils.isNotEmpty(new String[0]);
        assert !ObjectUtils.isEmpty(new String[] { "TODAY" });
        assert !ObjectUtils.isEmpty((Object) new String[] { "TODAY" });

        assert !ObjectUtils.isEmpty("TODAY");
        assert ObjectUtils.isNotEmpty("TODAY");
        assert ObjectUtils.isNotEmpty("TODAY");

        // collections
        assert ObjectUtils.isEmpty(Collections.emptySet());
        assert !ObjectUtils.isNotEmpty(Collections.emptySet());
        assert ObjectUtils.isEmpty(Collections.emptyMap());
        assert !ObjectUtils.isNotEmpty(Collections.emptyMap());
        assert ObjectUtils.isEmpty(Collections.emptyList());
        assert !ObjectUtils.isNotEmpty(Collections.emptyList());

    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_toArrayObject() throws ConversionException {

        // int[]
        Object arrayObject = ObjectUtils.toArrayObject(new String[] { "12121", "121212121" }, int[].class);
        assert arrayObject.getClass().equals(int[].class);
        assert ((int[]) arrayObject)[0] == 12121;

        // Integer[]
        Object integerarrayObject = ObjectUtils.toArrayObject(new String[] { "12121", "121212121" }, Integer[].class);
        assert integerarrayObject.getClass().equals(Integer[].class);
        assert ((Integer[]) integerarrayObject)[0] == 12121;

        // Long[]
        final Object LongArrayObject = ObjectUtils.toArrayObject(new String[] { "12121", "121212121" }, Long[].class);
        assert LongArrayObject.getClass().equals(Long[].class);
        assert ((Long[]) LongArrayObject)[0] == 12121l;

        // long[]
        final Object longArrayObject = ObjectUtils.toArrayObject(new String[] { "12121", "121212121" }, long[].class);
        assert longArrayObject.getClass().equals(long[].class);
        assert ((long[]) longArrayObject)[0] == 12121l;

        // String[]
        String[] inputString = new String[] { "12121", "121212121" };
        final Object stringArrayObject = ObjectUtils.toArrayObject(inputString, String[].class);
        assert stringArrayObject == inputString;

        // short[]
        final Object shortArrayObject = ObjectUtils.toArrayObject(new String[] { "1212", "12345" }, short[].class);
        assert shortArrayObject.getClass().equals(short[].class);
        assert ((short[]) shortArrayObject)[0] == 1212;

        // Short[]
        final Object ShortArrayObject = ObjectUtils.toArrayObject(new String[] { "1212", "12345" }, Short[].class);
        assert ShortArrayObject.getClass().equals(Short[].class);
        assert ((Short[]) ShortArrayObject)[0] == 1212;

        // byte[]
        final Object byteArrayObject = ObjectUtils.toArrayObject(new String[] { "125", "12" }, byte[].class);
        assert byteArrayObject.getClass().equals(byte[].class);
        assert ((byte[]) byteArrayObject)[0] == 125;

        // Byte[]
        final Object ByteArrayObject = ObjectUtils.toArrayObject(new String[] { "125", "12" }, Byte[].class);
        assert ByteArrayObject.getClass().equals(Byte[].class);
        assert ((Byte[]) ByteArrayObject)[0] == 125;

        // float[]
        final Object floatArrayObject = ObjectUtils.toArrayObject(new String[] { "125.45", "12.898" }, float[].class);
        assert floatArrayObject.getClass().equals(float[].class);
        assert ((float[]) floatArrayObject)[0] == 125.45f;

        // Float[]
        final Object FloatArrayObject = ObjectUtils.toArrayObject(new String[] { "125.45", "12.898" }, Float[].class);
        assert FloatArrayObject.getClass().equals(Float[].class);
        assert ((Float[]) FloatArrayObject)[0] == 125.45f;

        // double[]
        final Object doubleArrayObject = ObjectUtils.toArrayObject(new String[] { "125.45", "12.898" }, double[].class);
        assert doubleArrayObject.getClass().equals(double[].class);
        assert ((double[]) doubleArrayObject)[0] == 125.45d;

        // Double[]
        final Object DoubleArrayObject = ObjectUtils.toArrayObject(new String[] { "125.45", "12.898" }, Double[].class);
        assert DoubleArrayObject.getClass().equals(Double[].class);
        assert ((Double[]) DoubleArrayObject)[0] == 125.45d;

        // Object[]
        final Class<Class[]> clazz = Class[].class;
        final Object classArrayObject = ObjectUtils.toArrayObject(new String[] { //
            "cn.taketoday.core.utils.NumberUtilsTest", "cn.taketoday.core.utils.ClassUtilsTest" //
        }, clazz);

        assert classArrayObject.getClass().equals(clazz);
        assert ((Class[]) classArrayObject)[1] == ClassUtilsTest.class;

    }

    @Test
    public void test_ParseArray() throws ConversionException {
        int[] parseArray = ObjectUtils.parseArray(new String[] { "12", "12222", "12121", "56723562"
        }, int[].class);

        assert parseArray.length == 4;
        assert parseArray[0] == 12;
        assert parseArray[3] == 56723562;

        System.out.println(Arrays.toString(parseArray));
    }

    @Test
    public void testToHexString() throws ConversionException {

        final String hexString = ObjectUtils.toHexString(this);

        assertEquals(hexString, toString());

        assertEquals(ObjectUtils.toHexString(null), "null");

    }

}
