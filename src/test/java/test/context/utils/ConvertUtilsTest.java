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

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.utils.ConvertUtils;

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:43:53
 */
public class ConvertUtilsTest {

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
	public void test_Convert() {
		Object convert = ConvertUtils.convert("123", Integer.class);
		assert convert.getClass() == Integer.class;
		assert convert.equals(123);

		Integer[] convertArray = (Integer[]) ConvertUtils.convert("12;456,121", Integer[].class);

		System.err.println(Arrays.toString(convertArray));

		assert convertArray.getClass().isArray();
		assert convertArray.length == 3;
		assert convertArray[0] == 12;

	}

}
