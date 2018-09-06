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
package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import cn.taketoday.context.core.Scope;
import lombok.NoArgsConstructor;

/**
 * @author Today <br>
 * 
 *         2018-08-22 17:29
 */
@NoArgsConstructor
@SuppressWarnings("all")
public final class ComponentImpl implements Component {
	
	private String[]	value;
	
	private Scope		scope;

	@Override
	public Class<? extends Annotation> annotationType() {
		return Component.class;
	}

	@Override
	public String[] value() {
		return value;
	}

	@Override
	public Scope scope() {
		return scope;
	}
	
	
	@Override
	public String toString() {
		return new StringBuilder().append("@")//
				.append(Component.class.getName())//
				.append("(value=")//
				.append(Arrays.toString(value))//
				.append(", scope=")//
				.append(scope)//
				.append(")")//
				.toString();
	}

}
