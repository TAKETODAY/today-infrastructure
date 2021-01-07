/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package test.jdbc.model;

/**
 *
 * @author TODAY <br>
 *         2019-03-18 15:56
 */
public enum UserStatus {

  NORMAL(0, "正常"),
  INACTIVE(1, "账号尚未激活"),
  LOCKED(2, "账号被锁"),
  RECYCLE(3, "账号被冻结");

  private final int code;
  private final String msg;

  //@off
	public static UserStatus valueOf(int code) {
		switch (code)
		{
			case 0 : return NORMAL;
			case 1 : return INACTIVE;
			case 2 : return LOCKED;
			
			case 3 :
			default: return RECYCLE;
		}
	}
	//@on

  UserStatus(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public int getCode() {
    return code;
  }

  public String getMsg() {
    return msg;
  }
}
