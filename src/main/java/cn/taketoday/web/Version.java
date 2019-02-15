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
package cn.taketoday.web;

/**
 * @author Today <br>
 * 
 * @version 2.3.3
 * @time 2018 1 ? - 2018 3 8 <br>
 *       <b>2.2.2.RELEASE -> 2018-08-23 14:53</b><br>
 *       <b>2.2.4.RELEASE -> 2018-09-09 18:37</b><br>
 *       <b>2.3.1.RELEASE -> 2018-10-18 20:26</b><br>
 *       ...<br>
 *       <b>2.3.3.RELEASE -> 2018-12-07 21:28</b><br>
 *       <b>2.3.4.RELEASE -> 2019-02-10 18:03</b><br>
 */
public abstract class Version {

	public static String getVersion() {
		return "2.3.5.RELEASE";
	}

	public static String[] getHistoryVersion() {
		return new String[] { //
				"1.0.0", //
				"1.1.1", //
				"2.0.0", //
				"2.1.0.RELEASE", //
				"2.2.0.RELEASE", //
				"2.2.2.RELEASE", //
				"2.2.3.RELEASE", //
				"2.3.1.RELEASE", //
				"2.3.3.RELEASE", //
				"2.3.4.RELEASE", //
				"2.3.5.RELEASE" //
		};
	}
}
