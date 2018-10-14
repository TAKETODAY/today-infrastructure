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
package cn.taketoday.context.factory;

/**
 * @author Today <br>
 * 
 *         2018-09-11 11:01
 */
@FunctionalInterface
public interface ObjectFactory {

	/**
	 * Creates a new object with default constructor.
	 * 
	 * @param type
	 *             Object type
	 * @return
	 */
	default <T> T create(Class<T> type) {
		return create(type, null);
	}

	/**
	 * Creates a new object with the specified constructor and parameters.
	 * 
	 * @param type
	 *                            Object type
	 * @param constructorArgTypes
	 *                            Constructor argument types
	 * @param constructorArgs
	 *                            Constructor argument values
	 * @return
	 */
	<T> T create(Class<T> type, Class<?>[] constructorArgTypes, Object... constructorArgs);

}