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
package cn.taketoday.context;

/**
 * @author Today <br>
 * 
 *         2018-09-11 20:02
 */
public interface ObjectPool<T> {

	/**
	 * contains element
	 * 
	 * @param element
	 * @return
	 */
	boolean contains(T element);

	/**
	 * the size of pool
	 * 
	 * @return
	 */
	int size();

	/**
	 * the index os element
	 * 
	 * @param element
	 * @return
	 */
	int indexOf(T element);

	/**
	 * to array
	 * 
	 * @return
	 */
	T[] toArray();

	/**
	 * get a element from pool
	 * 
	 * @param index
	 * @return
	 */
	T get(int index);

	/**
	 * add a element to pool
	 * 
	 * @param element
	 * @return
	 */
	int add(T element);

}
