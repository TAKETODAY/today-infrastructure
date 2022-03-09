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

package cn.taketoday.context.testfixture.jndi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import cn.taketoday.jndi.JndiTemplate;

/**
 * Copy of the standard {@link cn.taketoday.context.testfixture.jndi.jndi.ExpectedLookupTemplate}
 * for testing purposes.
 *
 * <p>Simple extension of the JndiTemplate class that always returns a given object.
 *
 * <p>Very useful for testing. Effectively a mock object.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ExpectedLookupTemplate extends JndiTemplate {

	private final Map<String, Object> jndiObjects = new ConcurrentHashMap<>(16);


	/**
	 * Construct a new JndiTemplate that will always return given objects for
	 * given names. To be populated through {@code addObject} calls.
	 * @see #addObject(String, Object)
	 */
	public ExpectedLookupTemplate() {
	}

	/**
	 * Construct a new JndiTemplate that will always return the given object,
	 * but honour only requests for the given name.
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public ExpectedLookupTemplate(String name, Object object) {
		addObject(name, object);
	}


	/**
	 * Add the given object to the list of JNDI objects that this template will expose.
	 * @param name the name the client is expected to look up
	 * @param object the object that will be returned
	 */
	public void addObject(String name, Object object) {
		this.jndiObjects.put(name, object);
	}

	/**
	 * If the name is the expected name specified in the constructor, return the
	 * object provided in the constructor. If the name is unexpected, a
	 * respective NamingException gets thrown.
	 */
	@Override
	public Object lookup(String name) throws NamingException {
		Object object = this.jndiObjects.get(name);
		if (object == null) {
			throw new NamingException("Unexpected JNDI name '" + name + "': expecting " + this.jndiObjects.keySet());
		}
		return object;
	}

}
