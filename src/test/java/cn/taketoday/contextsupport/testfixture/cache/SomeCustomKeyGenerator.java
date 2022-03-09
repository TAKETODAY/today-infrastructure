/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.contextsupport.testfixture.cache;

import java.lang.reflect.Method;

import cn.taketoday.cache.interceptor.KeyGenerator;

/**
 * A custom {@link KeyGenerator} that exposes the algorithm used to compute the key
 * for convenience in test scenarios.
 *
 * @author Stephane Nicoll
 */
public class SomeCustomKeyGenerator implements KeyGenerator {

	@Override
	public Object generate(Object target, Method method, Object... params) {
		return generateKey(method.getName(), params);
	}

	/**
	 * @see #generate(Object, Method, Object...)
	 */
	public static Object generateKey(String methodName, Object... params) {
		final StringBuilder sb = new StringBuilder(methodName);
		for (Object param : params) {
			sb.append(param);
		}
		return sb.toString();
	}

}
