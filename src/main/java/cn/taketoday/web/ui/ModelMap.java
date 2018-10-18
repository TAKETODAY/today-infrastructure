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
package cn.taketoday.web.ui;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author Today <br>
 *         2018-10-14 20:29
 */
public final class ModelMap implements Model {

	protected final HttpServletRequest request;

	public ModelMap(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public ModelMap addAttribute(String attributeName, Object attributeValue) {
		request.setAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ModelMap addAllAttributes(Map<String, Object> attributes) {
		attributes.forEach(request::setAttribute);
		return this;
	}

	/**
	 * Does this model contain an attribute of the given name?
	 * 
	 * @param attributeName
	 *            the name of the model attribute (never {@code null})
	 * @return whether this model contains a corresponding attribute
	 */
	public boolean containsAttribute(String attributeName) {
		return request.getAttribute(attributeName) == null;
	}

	@Override
	public Map<String, Object> asMap() {
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			String name = attributeNames.nextElement();
			map.put(name, request.getAttribute(name));
		}
		return map;
	}

	@Override
	public void removeAttribute(String name) {
		request.removeAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return request.getAttributeNames();
	}

	@Override
	public int size() {
		int size = 0;
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			size++;
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return containsAttribute((String) key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(Object key) {
		return request.getAttribute((String) key);
	}

	@Override
	public Object put(String key, Object value) {
		request.setAttribute(key, value);
		return null;
	}

	@Override
	public Object remove(Object name) {
		request.removeAttribute((String) name);
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends String, ? extends Object> attributes) {
		addAllAttributes((Map<String, Object>) attributes);
	}
	

	@Override
	public void clear() {
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			request.removeAttribute(attributeNames.nextElement());
		}
	}

	@Override
	public Set<String> keySet() {
		Set<String> keySet = new HashSet<>();
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			keySet.add(attributeNames.nextElement());
		}
		return keySet;
	}

	@Override
	public Collection<Object> values() {
		Set<Object> valueSet = new HashSet<>();
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			valueSet.add(request.getAttribute(attributeNames.nextElement()));
		}
		return valueSet;
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

}
