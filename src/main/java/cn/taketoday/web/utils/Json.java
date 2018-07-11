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
package cn.taketoday.web.utils;

import java.io.Serializable;


/**
 * 
 * @author Today
 * @date 2018年6月30日 下午6:23:40
 */
public final class Json implements Serializable {

	private static final long	serialVersionUID	= -5925945582314435750L;

	private boolean				success;
	private String				msg;
	private Object				data;

	public Json() {

	}

	public Json(boolean success, String msg) {
		this.success = success;
		this.msg = msg;
	}

	public Json(boolean success, String msg, Object obj) {
		this.success = success;
		this.msg = msg;
		data = obj;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return " {\"success\":" + success + ",\"msg\":\"" + msg + "\",\"data\":\"" + data + "\"}";
	}

	
	
}
