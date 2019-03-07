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
package cn.taketoday.web.utils;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Today <br>
 * 
 *         2018-09-23 15:58
 */
@Setter
@Getter
@NoArgsConstructor
public class Pagination<T> {

	private String msg;
	private int code = 200;
	private boolean success;

	/**
	 * amount of page
	 */
	private long num;

	/**
	 * all row in database
	 */
	@JSONField(serialize = false)
	private long all;

	/**
	 * every page size
	 */
	private int size;

	/**
	 * current page
	 */
	private int current;

	/**
	 * data
	 */
	private List<T> data;

	/**
	 * @return
	 */
	public Pagination<T> setNum() {
		num = (all - 1) / size + 1;
		return this;
	}

	public Pagination<T> setData(List<T> data) {
		this.data = data;
		return this;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\"num\":\"").append(num)//
				.append("\",\"all\":\"").append(all)//
				.append("\",\"size\":\"").append(size)//
				.append("\",\"current\":\"").append(current)//
				.append("\",\"success\":\"").append(success)//
				.append("\"}")//
				.toString();
	}

}