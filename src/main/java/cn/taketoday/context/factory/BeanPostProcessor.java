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
 * 
 * @author Today <br>
 * 
 *         2018-07-18 1:01:19
 */
public interface BeanPostProcessor {

	/**
	 * before property set
	 * 
	 * @param bean
	 *            bean instance
	 * @param beanName
	 *            bean name
	 * @throws Exception
	 */
	Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception;

	/**
	 * after property set
	 * 
	 * @param bean
	 *            bean instance
	 * @param beanName
	 *            bean name
	 * @throws Exception
	 */
	Object postProcessAfterInitialization(Object bean, String beanName) throws Exception;
}
