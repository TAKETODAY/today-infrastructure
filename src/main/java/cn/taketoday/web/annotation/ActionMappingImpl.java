/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import cn.taketoday.web.RequestMethod;

/**
 * @author Today <br>
 * 
 *         2018-08-23 10:27
 */
@SuppressWarnings("all")
public class ActionMappingImpl implements ActionMapping {

	private String[]		value;

	private RequestMethod[]	method;

	@Override
	public Class<? extends Annotation> annotationType() {
		return ActionMapping.class;
	}

	@Override
	public String[] value() {
		return value;
	}

	@Override
	public RequestMethod[] method() {
		return method;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("@")//
				.append(ActionMapping.class.getName())//
				.append("(value=")//
				.append(Arrays.toString(value))//
				.append(", method=")//
				.append(Arrays.toString(method))//
				.append(")")//
				.toString();
	}

}
