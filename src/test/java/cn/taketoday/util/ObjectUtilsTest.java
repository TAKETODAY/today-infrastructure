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

import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.conversion.ConversionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static cn.taketoday.util.ObjectUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Today <br>
 * 2018-07-12 20:46:41
 */
class ObjectUtilsTest {

	@Test
	void testIsEmpty() throws ConfigurationException {
		// null
		assert isEmpty(null);
		assert !ObjectUtils.isNotEmpty(null);
		assert isEmpty((Object) null);
		assert !ObjectUtils.isNotEmpty((Object) null);

		// string array
		assert isEmpty(new String[0]);
		assert !ObjectUtils.isNotEmpty(new String[0]);
		assert !isEmpty(new String[] { "TODAY" });
		assert !isEmpty((Object) new String[] { "TODAY" });

		assert !isEmpty("TODAY");
		assert ObjectUtils.isNotEmpty("TODAY");
		assert ObjectUtils.isNotEmpty("TODAY");

		// collections
		assert isEmpty(Collections.emptySet());
		assert !ObjectUtils.isNotEmpty(Collections.emptySet());
		assert isEmpty(Collections.emptyMap());
		assert !ObjectUtils.isNotEmpty(Collections.emptyMap());
		assert isEmpty(Collections.emptyList());
		assert !ObjectUtils.isNotEmpty(Collections.emptyList());

	}

	@SuppressWarnings("rawtypes")
	@Test
	void test_toArrayObject() throws ConversionException {

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
						"cn.taketoday.util.NumberUtilsTest", "cn.taketoday.util.ClassUtilsTest" //
		}, clazz);

		assert classArrayObject.getClass().equals(clazz);
		assert ((Class[]) classArrayObject)[1] == ClassUtilsTest.class;

	}

	@Test
	void test_ParseArray() throws ConversionException {
		int[] parseArray = ObjectUtils.parseArray(new String[] { "12", "12222", "12121", "56723562"
		}, int[].class);

		assert parseArray.length == 4;
		assert parseArray[0] == 12;
		assert parseArray[3] == 56723562;

		System.out.println(Arrays.toString(parseArray));
	}

	@Test
	void testToHexString() throws ConversionException {

		final String hexString = ObjectUtils.toHexString(this);

		Assertions.assertEquals(hexString, toString());
		Assertions.assertEquals(ObjectUtils.toHexString(null), "null");
	}


	@Test
	void isCheckedException() {
		assertThat(ObjectUtils.isCheckedException(new Exception())).isTrue();
		assertThat(ObjectUtils.isCheckedException(new SQLException())).isTrue();

		assertThat(ObjectUtils.isCheckedException(new RuntimeException())).isFalse();
		assertThat(ObjectUtils.isCheckedException(new IllegalArgumentException(""))).isFalse();

		// Any Throwable other than RuntimeException and Error
		// has to be considered checked according to the JLS.
		assertThat(ObjectUtils.isCheckedException(new Throwable())).isTrue();
	}

	@Test
	void isCompatibleWithThrowsClause() {
		Class<?>[] empty = new Class<?>[0];
		Class<?>[] exception = new Class<?>[] { Exception.class };
		Class<?>[] sqlAndIO = new Class<?>[] { SQLException.class, IOException.class };
		Class<?>[] throwable = new Class<?>[] { Throwable.class };

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException())).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), empty)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), sqlAndIO)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new RuntimeException(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), sqlAndIO)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Exception(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), exception)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), sqlAndIO)).isTrue();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new SQLException(), throwable)).isTrue();

		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable())).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), empty)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), exception)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), sqlAndIO)).isFalse();
		assertThat(ObjectUtils.isCompatibleWithThrowsClause(new Throwable(), throwable)).isTrue();
	}

	@Test
	void isEmptyNull() {
		assertThat(isEmpty(null)).isTrue();
	}

	@Test
	void isEmptyArray() {
		assertThat(isEmpty(new char[0])).isTrue();
		assertThat(isEmpty(new Object[0])).isTrue();
		assertThat(isEmpty(new Integer[0])).isTrue();

		assertThat(isEmpty(new int[] { 42 })).isFalse();
		assertThat(isEmpty(new Integer[] { 42 })).isFalse();
	}

	@Test
	void isEmptyCollection() {
		assertThat(isEmpty(Collections.emptyList())).isTrue();
		assertThat(isEmpty(Collections.emptySet())).isTrue();

		Set<String> set = new HashSet<>();
		set.add("foo");
		assertThat(isEmpty(set)).isFalse();
		assertThat(isEmpty(Collections.singletonList("foo"))).isFalse();
	}

	@Test
	void isEmptyMap() {
		assertThat(isEmpty(Collections.emptyMap())).isTrue();

		HashMap<String, Object> map = new HashMap<>();
		map.put("foo", 42L);
		assertThat(isEmpty(map)).isFalse();
	}

	@Test
	void isEmptyCharSequence() {
		assertThat(isEmpty(new StringBuilder())).isTrue();
		assertThat(isEmpty("")).isTrue();

		assertThat(isEmpty(new StringBuilder("foo"))).isFalse();
		assertThat(isEmpty("   ")).isFalse();
		assertThat(isEmpty("\t")).isFalse();
		assertThat(isEmpty("foo")).isFalse();
	}

	@Test
	void isEmptyUnsupportedObjectType() {
		assertThat(isEmpty(42L)).isFalse();
		assertThat(isEmpty(new Object())).isFalse();
	}

	@Test
	void toObjectArray() {
		int[] a = new int[] { 1, 2, 3, 4, 5 };
		Integer[] wrapper = (Integer[]) ObjectUtils.toObjectArray(a);
		assertThat(wrapper.length == 5).isTrue();
		for (int i = 0; i < wrapper.length; i++) {
			assertThat(wrapper[i].intValue()).isEqualTo(a[i]);
		}
	}

	@Test
	void toObjectArrayWithNull() {
		Object[] objects = ObjectUtils.toObjectArray(null);
		assertThat(objects).isNotNull();
		assertThat(objects.length).isEqualTo(0);
	}

	@Test
	void toObjectArrayWithEmptyPrimitiveArray() {
		Object[] objects = ObjectUtils.toObjectArray(new byte[] { });
		assertThat(objects).isNotNull();
		assertThat(objects).hasSize(0);
	}

	@Test
	void toObjectArrayWithNonArrayType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						ObjectUtils.toObjectArray("Not an []"));
	}

	@Test
	void toObjectArrayWithNonPrimitiveArray() {
		String[] source = new String[] { "Bingo" };
		assertThat(ObjectUtils.toObjectArray(source)).isEqualTo(source);
	}

	@Test
	void addObjectToArraySunnyDay() {
		String[] array = new String[] { "foo", "bar" };
		String newElement = "baz";
		Object[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray).hasSize(3);
		assertThat(newArray[2]).isEqualTo(newElement);
	}

	@Test
	void addObjectToArrayWhenEmpty() {
		String[] array = new String[0];
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray).hasSize(1);
		assertThat(newArray[0]).isEqualTo(newElement);
	}

	@Test
	void addObjectToSingleNonNullElementArray() {
		String existingElement = "foo";
		String[] array = new String[] { existingElement };
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray).hasSize(2);
		assertThat(newArray[0]).isEqualTo(existingElement);
		assertThat(newArray[1]).isEqualTo(newElement);
	}

	@Test
	void addObjectToSingleNullElementArray() {
		String[] array = new String[] { null };
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertThat(newArray).hasSize(2);
		assertThat(newArray[0]).isNull();
		assertThat(newArray[1]).isEqualTo(newElement);
	}

	@Test
	void addObjectToNullArray() throws Exception {
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(null, newElement);
		assertThat(newArray).hasSize(1);
		assertThat(newArray[0]).isEqualTo(newElement);
	}

	@Test
	void addNullObjectToNullArray() throws Exception {
		Object[] newArray = ObjectUtils.addObjectToArray(null, null);
		assertThat(newArray).hasSize(1);
		assertThat(newArray[0]).isNull();
	}

	@Test
	void nullSafeEqualsWithArrays() throws Exception {
		assertThat(ObjectUtils.nullSafeEquals(new String[] { "a", "b", "c" }, new String[] { "a", "b", "c" })).isTrue();
		assertThat(ObjectUtils.nullSafeEquals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 })).isTrue();
	}

	@Test
	void identityToString() {
		Object obj = new Object();
		String expected = obj.getClass().getName() + "@" + ObjectUtils.getIdentityHexString(obj);
		String actual = ObjectUtils.identityToString(obj);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void identityToStringWithNullObject() {
		assertThat(ObjectUtils.identityToString(null)).isEmpty();
	}

	@Test
	void isArrayOfPrimitivesWithBooleanArray() {
		assertThat(ClassUtils.isPrimitiveArray(boolean[].class)).isTrue();
	}

	@Test
	void isArrayOfPrimitivesWithObjectArray() {
		assertThat(ClassUtils.isPrimitiveArray(Object[].class)).isFalse();
	}

	@Test
	void isArrayOfPrimitivesWithNonArray() {
		assertThat(ClassUtils.isPrimitiveArray(String.class)).isFalse();
	}

	@Test
	void isPrimitiveOrWrapperWithBooleanPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(boolean.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithBooleanWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Boolean.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithBytePrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(byte.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithByteWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Byte.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithCharacterClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Character.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithCharClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(char.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithDoublePrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(double.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithDoubleWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Double.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithFloatPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(float.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithFloatWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Float.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithIntClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(int.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithIntegerClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Integer.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithLongPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(long.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithLongWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Long.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithNonPrimitiveOrWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Object.class)).isFalse();
	}

	@Test
	void isPrimitiveOrWrapperWithShortPrimitiveClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(short.class)).isTrue();
	}

	@Test
	void isPrimitiveOrWrapperWithShortWrapperClass() {
		assertThat(ClassUtils.isPrimitiveOrWrapper(Short.class)).isTrue();
	}

	@Test
	void nullSafeHashCodeWithBooleanArray() {
		int expected = 31 * 7 + Boolean.TRUE.hashCode();
		expected = 31 * expected + Boolean.FALSE.hashCode();

		boolean[] array = { true, false };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithBooleanArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((boolean[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithByteArray() {
		int expected = 31 * 7 + 8;
		expected = 31 * expected + 10;

		byte[] array = { 8, 10 };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithByteArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((byte[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithCharArray() {
		int expected = 31 * 7 + 'a';
		expected = 31 * expected + 'E';

		char[] array = { 'a', 'E' };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithCharArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((char[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithDoubleArray() {
		long bits = Double.doubleToLongBits(8449.65);
		int expected = 31 * 7 + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(9944.923);
		expected = 31 * expected + (int) (bits ^ (bits >>> 32));

		double[] array = { 8449.65, 9944.923 };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithDoubleArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((double[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithFloatArray() {
		int expected = 31 * 7 + Float.floatToIntBits(9.6f);
		expected = 31 * expected + Float.floatToIntBits(7.4f);

		float[] array = { 9.6f, 7.4f };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithFloatArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((float[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithIntArray() {
		int expected = 31 * 7 + 884;
		expected = 31 * expected + 340;

		int[] array = { 884, 340 };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithIntArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((int[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithLongArray() {
		long lng = 7993L;
		int expected = 31 * 7 + (int) (lng ^ (lng >>> 32));
		lng = 84320L;
		expected = 31 * expected + (int) (lng ^ (lng >>> 32));

		long[] array = { 7993L, 84320L };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithLongArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((long[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithObject() {
		String str = "Luke";
		assertThat(ObjectUtils.nullSafeHashCode(str)).isEqualTo(str.hashCode());
	}

	@Test
	void nullSafeHashCodeWithObjectArray() {
		int expected = 31 * 7 + "Leia".hashCode();
		expected = 31 * expected + "Han".hashCode();

		Object[] array = { "Leia", "Han" };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithObjectArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((Object[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingBooleanArray() {
		Object array = new boolean[] { true, false };
		int expected = ObjectUtils.nullSafeHashCode((boolean[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingByteArray() {
		Object array = new byte[] { 6, 39 };
		int expected = ObjectUtils.nullSafeHashCode((byte[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingCharArray() {
		Object array = new char[] { 'l', 'M' };
		int expected = ObjectUtils.nullSafeHashCode((char[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingDoubleArray() {
		Object array = new double[] { 68930.993, 9022.009 };
		int expected = ObjectUtils.nullSafeHashCode((double[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingFloatArray() {
		Object array = new float[] { 9.9f, 9.54f };
		int expected = ObjectUtils.nullSafeHashCode((float[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingIntArray() {
		Object array = new int[] { 89, 32 };
		int expected = ObjectUtils.nullSafeHashCode((int[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingLongArray() {
		Object array = new long[] { 4389, 320 };
		int expected = ObjectUtils.nullSafeHashCode((long[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingObjectArray() {
		Object array = new Object[] { "Luke", "Anakin" };
		int expected = ObjectUtils.nullSafeHashCode((Object[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectBeingShortArray() {
		Object array = new short[] { 5, 3 };
		int expected = ObjectUtils.nullSafeHashCode((short[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	void nullSafeHashCodeWithObjectEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((Object) null)).isEqualTo(0);
	}

	@Test
	void nullSafeHashCodeWithShortArray() {
		int expected = 31 * 7 + 70;
		expected = 31 * expected + 8;

		short[] array = { 70, 8 };
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void nullSafeHashCodeWithShortArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeHashCode((short[]) null)).isEqualTo(0);
	}

	@Test
	void nullSafeToStringWithBooleanArray() {
		boolean[] array = { true, false };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{true, false}");
	}

	@Test
	void nullSafeToStringWithBooleanArrayBeingEmpty() {
		boolean[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithBooleanArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((boolean[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithByteArray() {
		byte[] array = { 5, 8 };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{5, 8}");
	}

	@Test
	void nullSafeToStringWithByteArrayBeingEmpty() {
		byte[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithByteArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((byte[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithCharArray() {
		char[] array = { 'A', 'B' };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{'A', 'B'}");
	}

	@Test
	void nullSafeToStringWithCharArrayBeingEmpty() {
		char[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithCharArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((char[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithDoubleArray() {
		double[] array = { 8594.93, 8594023.95 };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{8594.93, 8594023.95}");
	}

	@Test
	void nullSafeToStringWithDoubleArrayBeingEmpty() {
		double[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithDoubleArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((double[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithFloatArray() {
		float[] array = { 8.6f, 43.8f };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{8.6, 43.8}");
	}

	@Test
	void nullSafeToStringWithFloatArrayBeingEmpty() {
		float[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithFloatArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((float[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithIntArray() {
		int[] array = { 9, 64 };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{9, 64}");
	}

	@Test
	void nullSafeToStringWithIntArrayBeingEmpty() {
		int[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithIntArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((int[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithLongArray() {
		long[] array = { 434L, 23423L };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{434, 23423}");
	}

	@Test
	void nullSafeToStringWithLongArrayBeingEmpty() {
		long[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithLongArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((long[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithPlainOldString() {
		assertThat(ObjectUtils.nullSafeToString("I shoh love tha taste of mangoes")).isEqualTo("I shoh love tha taste of mangoes");
	}

	@Test
	void nullSafeToStringWithObjectArray() {
		Object[] array = { "Han", Long.valueOf(43) };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{Han, 43}");
	}

	@Test
	void nullSafeToStringWithObjectArrayBeingEmpty() {
		Object[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithObjectArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((Object[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithShortArray() {
		short[] array = { 7, 9 };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{7, 9}");
	}

	@Test
	void nullSafeToStringWithShortArrayBeingEmpty() {
		short[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithShortArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((short[]) null)).isEqualTo("null");
	}

	@Test
	void nullSafeToStringWithStringArray() {
		String[] array = { "Luke", "Anakin" };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{Luke, Anakin}");
	}

	@Test
	void nullSafeToStringWithStringArrayBeingEmpty() {
		String[] array = { };
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo("{}");
	}

	@Test
	void nullSafeToStringWithStringArrayEqualToNull() {
		assertThat(ObjectUtils.nullSafeToString((String[]) null)).isEqualTo("null");
	}

	@Test
	void containsConstant() {
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BaR")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "bar")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BAZ")).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "baz")).isTrue();

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "BOGUS")).isFalse();

		assertThat(ObjectUtils.containsConstant(Tropes.values(), "FOO", true)).isTrue();
		assertThat(ObjectUtils.containsConstant(Tropes.values(), "foo", true)).isFalse();
	}

	@Test
	void containsElement() {
		Object[] array = { "foo", "bar", 42, new String[] { "baz", "quux" } };

		assertThat(ObjectUtils.containsElement(null, "foo")).isFalse();
		assertThat(ObjectUtils.containsElement(array, null)).isFalse();
		assertThat(ObjectUtils.containsElement(array, "bogus")).isFalse();

		assertThat(ObjectUtils.containsElement(array, "foo")).isTrue();
		assertThat(ObjectUtils.containsElement(array, "bar")).isTrue();
		assertThat(ObjectUtils.containsElement(array, 42)).isTrue();
		assertThat(ObjectUtils.containsElement(array, new String[] { "baz", "quux" })).isTrue();
	}

	@Test
	void caseInsensitiveValueOf() {
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "foo")).isEqualTo(Tropes.FOO);
		assertThat(ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "BAR")).isEqualTo(Tropes.BAR);

		assertThatIllegalArgumentException()
						.isThrownBy(() -> ObjectUtils.caseInsensitiveValueOf(Tropes.values(), "bogus"))
						.withMessage("Constant [bogus] does not exist in enum type cn.taketoday.util.ObjectUtilsTest$Tropes");
	}

	private void assertEqualHashCodes(int expected, Object array) {
		int actual = ObjectUtils.nullSafeHashCode(array);
		assertThat(actual).isEqualTo(expected);
		assertThat(array.hashCode() != actual).isTrue();
	}

	enum Tropes {FOO, BAR, baz}

}
