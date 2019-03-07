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

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-30 18:23:40
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("serial")
public class Json implements Serializable {

	private String msg;
	private Object data;
	private int code = 200;
	private boolean success;

	public Json(boolean success) {
		this.success = success;
	}

	public Json(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public Json(String msg, int code, boolean success) {
		this.msg = msg;
		this.code = code;
		this.success = success;
	}
	
	public Json(int code, boolean success) {
		this.code = code;
		this.success = success;
	}

	public Json(boolean success, String msg, Object obj) {
		this.success = success;
		this.msg = msg;
		data = obj;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\"msg\":\"").append(msg)//
				.append("\",\"code\":\"").append(code)//
				.append("\",\"data\":\"").append(data)//
				.append("\",\"success\":\"").append(success)//
				.append("\"}")//
				.toString();
	}

}
