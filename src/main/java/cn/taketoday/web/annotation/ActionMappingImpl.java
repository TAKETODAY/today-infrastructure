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
package cn.taketoday.web.annotation;

import cn.taketoday.web.RequestMethod;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * @author Today <br>
 * 
 *         2018-08-23 10:27
 */
@SuppressWarnings("all")
public class ActionMappingImpl implements ActionMapping {

	private String[] value;
	private boolean exclude;
	private RequestMethod[] method;

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

	@Override
	public boolean exclude() {
		return exclude;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ActionMapping)) {
			return false;
		}

		ActionMapping actionMapping = (ActionMapping) obj;
		RequestMethod[] otherMethods = actionMapping.method();
		String[] otherValues = actionMapping.value();
		if (actionMapping.exclude() != exclude || //
				otherValues.length != value.length || //
				method.length != otherMethods.length) {

			return false;
		}
		for (int i = 0; i < method.length; i++) {
			if (!otherMethods[i].equals(method[i])) {
				return false;
			}
		}
		for (int i = 0; i < value.length; i++) {
			if (!otherValues[i].equals(value[i])) {
				return false;
			}
		}
		return true;
	}

}
