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
package test.web.utils;

import cn.taketoday.web.utils.Json;
import cn.taketoday.web.utils.ParamList;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-12-11 09:28
 */
public class ParamListTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void test_IsEmpty() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}
		assert !params.isEmpty();
	}

	@Test
	public void test_Contains() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}
		for (int i = 0; i < 10; i++) {
			assert params.contains(i);
		}
		List<String> params_ = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params_.add(i + "today");
		}
		for (int i = 0; i < 10; i++) {
			assert params_.contains(i + "today");
		}
	}

	@Test
	public void test_IndexOfObject() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}
		for (int i = 0; i < 10; i++) {
			assert params.indexOf(i) == i;
		}
		List<String> params_ = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params_.add(i + "today");
		}
		for (int i = 0; i < 10; i++) {
			assert params_.indexOf(i + "today") == i;
		}
	}

	@Test
	public void test_LastIndexOfObject() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}
		for (int i = 0; i < 10; i++) {
			assert params.lastIndexOf(i) == i;
		}

		List<String> params_ = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params_.add(i + "today");
		}
		for (int i = 0; i < 10; i++) {
			assert params_.lastIndexOf(i + "today") == i;
		}
	}

	@Test
	public void test_ToArray() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}
		Object[] array = params.toArray();
		assert array.length == 10;

		for (int i = 0; i < 10; i++) {
			assert (int) array[i] == i;
		}
		List<String> params_ = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params_.add(i + "today");
		}
		Object[] array_ = params_.toArray();
		assert array_.length == 10;
		for (int i = 0; i < 10; i++) {
			assert array_[i].equals(i + "today");
		}
	}

	@Test
	public void test_Get() {
		List<Integer> params = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params.add(i);
		}

		for (int i = 0; i < 10; i++) {
			assert (int) params.get(i) == i;
		}

		List<String> params_ = new ParamList<>();
		for (int i = 0; i < 10; i++) {
			params_.add(i + "today");
		}

		for (int i = 0; i < 10; i++) {
			assert params_.get(i).equals(i + "today");
		}
	}

	@Test
	public void test_Set() {

	}

	@Test
	public void test_AddE() {

		List<Object> params = new ParamList<>();
		Json json = new Json();
		json.setCode(100);
		json.setMsg("today");
		params.add(json);
		params.add(json);
		params.add(json);

		assert params.size() == 3;
	}

	@Test
	public void test_AddIntE() {

		List<Object> params = new ParamList<>();
		Json json = new Json();
		json.setCode(100);
		json.setMsg("today");
		params.add(json);
		params.add(5, json);
		assert params.get(5).equals(params.get(0));
	}

	@Test
	public void test_RemoveInt() {

		List<Object> params = new ParamList<>();
		Json json = new Json();
		json.setCode(100);
		json.setMsg("today");

		params.add(json);
		params.add(5, json);
		Object remove = params.remove(4);

		assert remove == null;
		assert params.get(5) == null;

		assert params.get(4).equals(params.get(0));
	}

	@Test
	public void test_RemoveObject() {

		List<Object> params = new ParamList<>();
		Json json = new Json()//
				.setCode(100)//
				.setMsg("today");

		params.add(json);
		params.add(5, json);

		assert params.remove(json);
		assert params.size() == 5;
		assert params.get(5) == null;
		assert params.get(4).equals(json);
	}

}
