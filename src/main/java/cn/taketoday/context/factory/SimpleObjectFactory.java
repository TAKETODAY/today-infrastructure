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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-09-11 11:04
 */
@Slf4j
public class SimpleObjectFactory implements ObjectFactory {

	@Override
	public <T> T create(Class<T> type, Class<?>[] constructorArgTypes, Object... constructorArgs) {

		if (type == List.class || type == Collection.class) {
			return type.cast(new ArrayList<>());
		} else if (type == Map.class) {
			
			return type.cast(new HashMap<>());
		} else if (type == SortedSet.class) {
			
			return type.cast(new TreeSet<>());
		} else if (type == Set.class) {
			
			return type.cast(new HashSet<>());
		}

		try {
			
			if (constructorArgTypes == null || constructorArgs == null) {
				return type.getDeclaredConstructor().newInstance();
			}
			return type.cast(type.getConstructor(constructorArgTypes).newInstance(constructorArgs));
		} //
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {

			log.error("Can't create a -> [{}] instance with given args -> [{}].", type.getName(),
					Arrays.toString(constructorArgs), e);
			
			return null;
		}
	}

}
