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
		this(success, msg, 200, null);
	}

	public Json(String msg, int code, boolean success) {
		this(success, msg, code, null);
	}

	public Json(int code, boolean success) {
		this(success, null, code, null);
	}

	public Json(boolean success, String msg, Object obj) {
		this(success, msg, 200, obj);
	}

	public Json(boolean success, String msg, int code, Object obj) {
		this.success = success;
		this.msg = msg;
		this.data = obj;
		this.code = code;
	}

	public final static Json newJson() {
		return new Json();
	}

	public final static Json newJson(int code, boolean success) {
		return new Json(code, success);
	}

	public final static Json newJson(boolean success, String msg) {
		return new Json(success, msg, 200, null);
	}

	public final static Json newJson(boolean success, String msg, Object data) {
		return new Json(success, msg, 200, data);
	}

	public final static Json newJson(boolean success, String msg, int code, Object obj) {
		return new Json(success, msg, code, obj);
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
