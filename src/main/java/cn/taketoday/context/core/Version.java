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
package cn.taketoday.context.core;

/**
 * @author Today <br>
 * 			2018-06-22 15:28:04
 * @version 1.0.0
 */
/**
 * 
 * @author Today <br>
 *         2018-06-07 10:56
 * @version 1.0.3
 */
public abstract class Version {

	private final static String	HISTORY_VERSION[]	= { "1.0.0", "1.0.1", "1.0.2" };

	private final static String	VERSION				= "today-context-1.0.3-RELEASE";

	public static String getVersion() {
		return VERSION;
	}

	public static String[] getHistoryVersion() {
		return HISTORY_VERSION;
	}
}



