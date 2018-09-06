/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.context.utils;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.NumberUtils;

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
		int[] parseArray = NumberUtils.parseArray(new String[] { "12", "12222", "12121", "56723562" }, int[].class);
		
		assert parseArray.length == 4;
		assert parseArray[0] == 12;
		
		System.out.println(Arrays.toString(parseArray));
	}

}
