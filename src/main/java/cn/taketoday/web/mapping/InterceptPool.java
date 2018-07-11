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
import java.util.function.Consumer;

import cn.taketoday.web.interceptor.InterceptProcessor;

/**
 * intercept pool like list
 * 
 * @author Today
 * @date 2018年6月25日 下午8:04:46
 * @version 2.0.0
 */
public final class InterceptPool implements RandomAccess, Cloneable, java.io.Serializable {

	private static final long	serialVersionUID	= 8673264195747942595L;

	private transient volatile Object[]	array;

	final void setArray(Object[] a) {
		array = a;
	}

	public InterceptPool() {
		array = new Object[0];
	}

	public int size() {
		return array.length;
	}

	public boolean isEmpty() {
		return array.length == 0;
	}

	private static int indexOf(Class<InterceptProcessor> o, Object[] elements, int index, int fence) {
		
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
	
	
	private static int lastIndexOf(Class<InterceptProcessor> o, Object[] elements, int index) {
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


	public boolean contains(Class<InterceptProcessor> o) {
		return indexOf(o, array, 0, array.length) >= 0;
	}

	public int indexOf(Class<InterceptProcessor> o) {
		return indexOf(o, array, 0, array.length);
	}

	public int lastIndexOf(Class<InterceptProcessor> o) {
		return lastIndexOf(o, array, array.length - 1);
	}

	public int lastIndexOf(Class<InterceptProcessor> e, int index) {
		return lastIndexOf(e, array, index);
	}

	public Object[] toArray() {
		return array;
	}

	public InterceptProcessor get(int index) {
		return index > array.length - 1 ? null : (InterceptProcessor) array[index];
	}

	public InterceptProcessor set(int index, InterceptProcessor element) {
		
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


	public boolean add(InterceptProcessor e) {
		Object[] newArray = new Object[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;
		array = newArray;

		return true;
	}

	public void add(int index, InterceptProcessor element) {
		this.set(index, element);
	}

	public InterceptProcessor remove(int index) {
		Object[] elements = array;
		InterceptProcessor oldValue = get(index);
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
	

	public void clear() {
		setArray(new Object[0]);
	}

	public void forEach(Consumer<? super InterceptProcessor> action) {
		if (action == null)
			throw new NullPointerException();
		
		for (int i = 0; i < array.length; ++i) {
			InterceptProcessor e = (InterceptProcessor) array[i];
			action.accept(e);
		}
	}

	public String toString() {
		return Arrays.toString(array);
	}


	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < array.length; ++i) {
			Object obj = array[i];
			hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	
	
}

