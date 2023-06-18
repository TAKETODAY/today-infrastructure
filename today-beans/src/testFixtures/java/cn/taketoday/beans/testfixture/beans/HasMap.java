/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.testfixture.beans;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Bean exposing a map. Used for bean factory tests.
 *
 * @author Rod Johnson
 * @since 05.06.2003
 */
public class HasMap {

	private Map<?, ?> map;

	private Set<?> set;

	private Properties props;

	private Object[] objectArray;

	private Integer[] intArray;

	private Class<?>[] classArray;

	private List<Class<?>> classList;

	private IdentityHashMap<?, ?> identityMap;

	private CopyOnWriteArraySet<?> concurrentSet;

	private HasMap() {
	}

	public Map<?, ?> getMap() {
		return map;
	}

	public void setMap(Map<?, ?> map) {
		this.map = map;
	}

	public Set<?> getSet() {
		return set;
	}

	public void setSet(Set<?> set) {
		this.set = set;
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public Object[] getObjectArray() {
		return objectArray;
	}

	public void setObjectArray(Object[] objectArray) {
		this.objectArray = objectArray;
	}

	public Integer[] getIntegerArray() {
		return intArray;
	}

	public void setIntegerArray(Integer[] is) {
		intArray = is;
	}

	public Class<?>[] getClassArray() {
		return classArray;
	}

	public void setClassArray(Class<?>[] classArray) {
		this.classArray = classArray;
	}

	public List<Class<?>> getClassList() {
		return classList;
	}

	public void setClassList(List<Class<?>> classList) {
		this.classList = classList;
	}

	public IdentityHashMap<?, ?> getIdentityMap() {
		return identityMap;
	}

	public void setIdentityMap(IdentityHashMap<?, ?> identityMap) {
		this.identityMap = identityMap;
	}

	public CopyOnWriteArraySet<?> getConcurrentSet() {
		return concurrentSet;
	}

	public void setConcurrentSet(CopyOnWriteArraySet<?> concurrentSet) {
		this.concurrentSet = concurrentSet;
	}

}
