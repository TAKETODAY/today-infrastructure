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

import cn.taketoday.web.interceptor.HandlerInterceptor;

import java.util.Arrays;
import java.util.RandomAccess;

/**
 * intercept pool.
 * 
 * @author Today <br>
 * 
 *         2018-06-25 20:04:46
 */
public final class InterceptPool implements RandomAccess, Cloneable, java.io.Serializable {

	private static final long serialVersionUID = 8673264195747942595L;

	private transient HandlerInterceptor[] array;

	public InterceptPool() {
		array = new HandlerInterceptor[0];
	}

	public int size() {
		return array.length;
	}

	private int indexOf(Class<HandlerInterceptor> o, Object[] elements, int index, int fence) {

		if (o == null) {
			for (int i = index; i < fence; i++)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i < fence; i++) {
				if (o.getName().equals(elements[i].getClass().getName()))
					return i;
			}
		}
		return -1;
	}

	public int indexOf(Class<HandlerInterceptor> o) {
		return indexOf(o, array, 0, array.length);
	}

	public HandlerInterceptor[] toArray() {
		return array;
	}

	public HandlerInterceptor get(int index) {
		return array[index];
	}

	public boolean add(HandlerInterceptor e) {

		HandlerInterceptor[] newArray = new HandlerInterceptor[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;
		array = newArray;

		return true;
	}

	@Override
	public String toString() {
		return Arrays.toString(array);
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < array.length; ++i) {
			Object obj = array[i];
			hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

}
