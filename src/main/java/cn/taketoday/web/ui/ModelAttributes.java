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
package cn.taketoday.web.ui;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import cn.taketoday.web.exception.InternalServerException;

/**
 * 
 * @author Today <br>
 *         2018-10-14 20:29
 */
public class ModelAttributes implements Model, Map<String, Object> {

	private final HttpServletRequest request;

	public ModelAttributes(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public ModelAttributes addAttribute(String attributeName, Object attributeValue) {
		if (attributeName != null) {
			request.setAttribute(attributeName, attributeValue);
		}
		return this;
	}

	@Override
	public ModelAttributes addAllAttributes(Map<String, Object> attributes) {
		attributes.forEach(request::setAttribute);
		return this;
	}

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
	public Collection<String> getAttributeNames() {
		Enumeration<String> enumeration = request.getAttributeNames();
		Collection<String> attributeNames = new HashSet<>();
		while (enumeration.hasMoreElements()) {
			attributeNames.add(enumeration.nextElement());
		}
		return attributeNames;
	}

	@Override
	public int size() {
		int size = 0;
		while (request.getAttributeNames().hasMoreElements()) {
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
		if (!(key instanceof String)) {
			throw new RuntimeException("Attribute name must be a String");
		}
		return request.getAttribute((String) key);
	}

	@Override
	public Object put(String key, Object value) {
		request.setAttribute(key, value);
		return null;
	}

	@Override
	public Object remove(Object name) {
		if (name instanceof String) {
			request.removeAttribute((String) name);
			return null;
		}
		throw new InternalServerException("Attribute name must be a String");
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
		Set<Entry<String, Object>> entries = new HashSet<>();
		Enumeration<String> attributeNames = request.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			String currentKey = attributeNames.nextElement();
			entries.add(new Node(currentKey, request.getAttribute(currentKey)));
		}
		return entries;
	}

	@Override
	public Object getAttribute(String name) {
		return get(name);
	}

	@Override
	public <T> T getAttribute(String name, Class<T> targetClass) {
		return targetClass.cast(get(name));
	}

	private static final class Node implements Entry<String, Object> {

		private final String key;
		private Object value;

		public Node(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public final Object setValue(Object value) {
			Object oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		@Override
		public final Object getValue() {
			return value;
		}

		@Override
		public final String getKey() {
			return key;
		}
	}

}
