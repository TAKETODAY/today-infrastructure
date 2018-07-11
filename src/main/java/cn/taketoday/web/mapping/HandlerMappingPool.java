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
 * @author Today
 * @date 2018年7月1日 下午8:47:06
 */
public final class HandlerMappingPool implements RandomAccess, Cloneable, java.io.Serializable {

	private static final long serialVersionUID = -1108959675710066622L;

	private transient volatile Object[]	array;
	
	final void setArray(Object[] a) {
		array = a;
	}

	public HandlerMappingPool() {
		array = new Object[0];
	}

	public int size() {
		return array.length;
	}

	public boolean isEmpty() {
		return array.length == 0;
	}

	private static int indexOf(Class<HandlerMapping> o, Object[] elements, int index, int fence) {
		
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
	
	
	private static int lastIndexOf(Class<HandlerMapping> o, Object[] elements, int index) {
		if (o == null) {
			for (int i = index; i >= 0; i--)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i >= 0; i--)
				if (o.getName().equals(elements[i].getClass().getName()))
					return i;
		}
		return -1;
	}


	public boolean contains(Class<HandlerMapping> o) {
		return indexOf(o, array, 0, array.length) >= 0;
	}

	public int indexOf(Class<HandlerMapping> o) {
		return indexOf(o, array, 0, array.length);
	}

	public int lastIndexOf(Class<HandlerMapping> o) {
		return lastIndexOf(o, array, array.length - 1);
	}

	public int lastIndexOf(Class<HandlerMapping> e, int index) {
		return lastIndexOf(e, array, index);
	}

	public Object[] toArray() {
		return array;
	}

	public HandlerMapping get(int index) {
		return index > array.length - 1 ? null : (HandlerMapping) array[index];
	}

	public HandlerMapping set(int index, HandlerMapping element) {
		
		if(index <= array.length - 1) {
			array[index] = element;
			return element;
		}
		
		Object[] newArray = new Object[index + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[index] = element;
		
		array = newArray;
		
		return element;
	}


	public int add(HandlerMapping e) {
		
		for (int i = 0; i < array.length; i++) {
			if (e.equals(array[i])) {
				return i;
			}
		}
		
		Object[] newArray = new Object[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;
		
		array = newArray;

		return array.length - 1;
	}

	public void add(int index, HandlerMapping element) {
		this.set(index, element);
	}

	public HandlerMapping remove(int index) {
		Object[] elements = array;
		HandlerMapping oldValue = get(index);
		int numMoved = array.length - index - 1;
		if (numMoved == 0)
			setArray(Arrays.copyOf(elements, array.length - 1));
		else {
			Object[] newElements = new Object[array.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, numMoved);
			setArray(newElements);
		}
		return oldValue;
	}
	

	public String toString() {
		return Arrays.toString(array);
	}



	
	
	
}
