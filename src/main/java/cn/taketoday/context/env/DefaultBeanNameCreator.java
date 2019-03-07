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
package cn.taketoday.context.env;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;

/**
 * Default implementation of {@link BeanNameCreator}
 * 
 * @author Today <br>
 * 
 *         2019-01-13 13:39
 */
public class DefaultBeanNameCreator implements BeanNameCreator {

	private final boolean useSimpleName;

	public DefaultBeanNameCreator(ConfigurableEnvironment environment) {
		final String useSimpleName = environment.getProperty(Constant.KEY_USE_SIMPLE_NAME);
		if (useSimpleName != null)
			this.useSimpleName = Boolean.parseBoolean(useSimpleName);
		else
			this.useSimpleName = true;
	}

	@Override
	public String create(Class<?> beanClass) {
		if (beanClass == null) {
			return Constant.DEFAULT;
		}
		if (useSimpleName) {
			final String simpleName = beanClass.getSimpleName();
			return (simpleName.charAt(0) + "").toLowerCase() + simpleName.substring(1);
		}
		return beanClass.getName(); // full name
	};
}
