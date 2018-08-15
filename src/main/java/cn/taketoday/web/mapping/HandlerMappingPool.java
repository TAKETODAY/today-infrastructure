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
package cn.taketoday.web.mapping;

import java.util.Arrays;
import java.util.RandomAccess;

/**
 * 
 * @author Today <br>
 * 
 *         2018-07-1 20:47:06
 */
public final class HandlerMappingPool implements RandomAccess, Cloneable, java.io.Serializable {

	private static final long					serialVersionUID	= -1108959675710066622L;

	/** pool **/
	private transient volatile HandlerMapping[]	array;

	public HandlerMappingPool() {
		array = new HandlerMapping[0];
	}

	/**
	 * Get HandlerMapping count.
	 * 
	 * @return HandlerMapping count
	 */
	public int size() {
		return array.length;
	}

	/**
	 * Get HandlerMapping instance.
	 * 
	 * @param index
	 *            the HandlerMapping number
	 * @return
	 */
	public HandlerMapping get(int index) {
		return array[index];
	}

	/**
	 * Add HandlerMapping to pool.
	 * 
	 * @param e
	 *            HandlerMapping instance
	 * @return
	 */
	public int add(HandlerMapping e) {

		for (int i = 0; i < array.length; i++) {
			if (e.equals(array[i])) {
				return i;
			}
		}

		HandlerMapping[] newArray = new HandlerMapping[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;

		array = newArray;

		return array.length - 1;
	}

	public String toString() {
		return Arrays.toString(array);
	}

}
