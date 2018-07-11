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
package cn.taketoday.web.core;

/**
 * @author YHJ
 * @version 1.0
 * @time 2017 08 29 22:20
 * @version 2.0
 * @time 2018 1 ? - 2018 3 8
 */
public final class Version {

	public static final String	HISTORY_VERSION	= "1.0.0";
	/**
	 * 当前版本
	 */
	public static final String	VERSION			= "2.1.0";

	/**
	 * 当前版本
	 * 
	 * @return
	 */
	public String getVersion() {
		return VERSION;
	}

	public static String getHistoryVersion() {
		return HISTORY_VERSION;
	}
}
