/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package test.context.utils;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.NumberUtils;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Today
 * @date 2018年7月6日 下午1:36:29
 */
public class NumberUtilsTest {

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
	public void test_ParseArray() throws ConversionException {
		int[] parseArray = NumberUtils.parseArray(new String[] {
				"12", "12222", "12121", "56723562"
		}, int[].class);

		assert parseArray.length == 4;
		assert parseArray[0] == 12;
		assert parseArray[3] == 56723562;

		System.out.println(Arrays.toString(parseArray));
	}

	@Test
	public void test_IsNumber() throws ConversionException {
		assert NumberUtils.isNumber(int.class);
		assert NumberUtils.isNumber(short.class);
		assert NumberUtils.isNumber(Short.class);
		assert NumberUtils.isNumber(Integer.class);
	}

	@Test
	public void test_ParseDigit() throws ConversionException {
		Object parseDigit = NumberUtils.parseDigit("12121", Integer.class);

		assert parseDigit.equals(12121);
		assert parseDigit.getClass() == Integer.class;
	}

	@Test
	public void test_ParseNumber() throws ConversionException {
		Integer parseNumber = NumberUtils.parseNumber("12121", Integer.class);

		assert parseNumber.equals(12121);
		assert parseNumber.getClass() == Integer.class;
	}

	@Test
	public void test_toArrayObject() throws ConversionException {
		Object arrayObject = NumberUtils.toArrayObject(new String[] {
				"12121", "121212121"
		}, Integer[].class);

		assert arrayObject.getClass().equals(Integer[].class);

		assert ((Integer[]) arrayObject)[0] == 12121;
	}

}
