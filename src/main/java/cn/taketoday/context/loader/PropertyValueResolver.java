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
package cn.taketoday.context.loader;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.ContextException;

import java.lang.reflect.Field;

/**
 * Resolve field property
 * 
 * @author Today <br>
 * 
 *         2018-08-04 15:04
 */
@FunctionalInterface
public interface PropertyValueResolver {

	/**
	 * According to different annotation resolve different property.
	 * 
	 * @param applicationContext
	 *            Bean definition registry
	 * @param field
	 *            bean's field
	 * @return property value
	 * @throws ContextException
	 */
	PropertyValue resolveProperty(ApplicationContext applicationContext, Field field) throws ContextException;

}
