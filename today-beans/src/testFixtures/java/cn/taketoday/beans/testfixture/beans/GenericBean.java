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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.io.Resource;

/**
 * @author Juergen Hoeller
 */
public class GenericBean<T> {

	private Set<Integer> integerSet;

	private Set<? extends Number> numberSet;

	private Set<ITestBean> testBeanSet;

	private List<Resource> resourceList;

	private List<TestBean> testBeanList;

	private List<List<Integer>> listOfLists;

	private ArrayList<String[]> listOfArrays;

	private List<Map<Integer, Long>> listOfMaps;

	private Map<?, ?> plainMap;

	private Map<Short, Integer> shortMap;

	private HashMap<Long, ?> longMap;

	private Map<Number, Collection<? extends Object>> collectionMap;

	private Map<String, Map<Integer, Long>> mapOfMaps;

	private Map<Integer, List<Integer>> mapOfLists;

	private CustomEnum customEnum;

	private CustomEnum[] customEnumArray;

	private Set<CustomEnum> customEnumSet;

	private EnumSet<CustomEnum> standardEnumSet;

	private EnumMap<CustomEnum, Integer> standardEnumMap;

	private T genericProperty;

	private List<T> genericListProperty;


	public GenericBean() {
	}

	public GenericBean(Set<Integer> integerSet) {
		this.integerSet = integerSet;
	}

	public GenericBean(Set<Integer> integerSet, List<Resource> resourceList) {
		this.integerSet = integerSet;
		this.resourceList = resourceList;
	}

	public GenericBean(HashSet<Integer> integerSet, Map<Short, Integer> shortMap) {
		this.integerSet = integerSet;
		this.shortMap = shortMap;
	}

	public GenericBean(Map<Short, Integer> shortMap, Resource resource) {
		this.shortMap = shortMap;
		this.resourceList = Collections.singletonList(resource);
	}

	public GenericBean(Map<?, ?> plainMap, Map<Short, Integer> shortMap) {
		this.plainMap = plainMap;
		this.shortMap = shortMap;
	}

	public GenericBean(HashMap<Long, ?> longMap) {
		this.longMap = longMap;
	}

	public GenericBean(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap) {
		this.collectionMap = collectionMap;
	}


	public Set<Integer> getIntegerSet() {
		return integerSet;
	}

	public void setIntegerSet(Set<Integer> integerSet) {
		this.integerSet = integerSet;
	}

	public Set<? extends Number> getNumberSet() {
		return numberSet;
	}

	public void setNumberSet(Set<? extends Number> numberSet) {
		this.numberSet = numberSet;
	}

	public Set<ITestBean> getTestBeanSet() {
		return testBeanSet;
	}

	public void setTestBeanSet(Set<ITestBean> testBeanSet) {
		this.testBeanSet = testBeanSet;
	}

	public List<Resource> getResourceList() {
		return resourceList;
	}

	public void setResourceList(List<Resource> resourceList) {
		this.resourceList = resourceList;
	}

	public List<TestBean> getTestBeanList() {
		return testBeanList;
	}

	public void setTestBeanList(List<TestBean> testBeanList) {
		this.testBeanList = testBeanList;
	}

	public List<List<Integer>> getListOfLists() {
		return listOfLists;
	}

	public ArrayList<String[]> getListOfArrays() {
		return listOfArrays;
	}

	public void setListOfArrays(ArrayList<String[]> listOfArrays) {
		this.listOfArrays = listOfArrays;
	}

	public void setListOfLists(List<List<Integer>> listOfLists) {
		this.listOfLists = listOfLists;
	}

	public List<Map<Integer, Long>> getListOfMaps() {
		return listOfMaps;
	}

	public void setListOfMaps(List<Map<Integer, Long>> listOfMaps) {
		this.listOfMaps = listOfMaps;
	}

	public Map<?, ?> getPlainMap() {
		return plainMap;
	}

	public Map<Short, Integer> getShortMap() {
		return shortMap;
	}

	public void setShortMap(Map<Short, Integer> shortMap) {
		this.shortMap = shortMap;
	}

	public HashMap<Long, ?> getLongMap() {
		return longMap;
	}

	public void setLongMap(HashMap<Long, ?> longMap) {
		this.longMap = longMap;
	}

	public Map<Number, Collection<? extends Object>> getCollectionMap() {
		return collectionMap;
	}

	public void setCollectionMap(Map<Number, Collection<? extends Object>> collectionMap) {
		this.collectionMap = collectionMap;
	}

	public Map<String, Map<Integer, Long>> getMapOfMaps() {
		return mapOfMaps;
	}

	public void setMapOfMaps(Map<String, Map<Integer, Long>> mapOfMaps) {
		this.mapOfMaps = mapOfMaps;
	}

	public Map<Integer, List<Integer>> getMapOfLists() {
		return mapOfLists;
	}

	public void setMapOfLists(Map<Integer, List<Integer>> mapOfLists) {
		this.mapOfLists = mapOfLists;
	}

	public T getGenericProperty() {
		return genericProperty;
	}

	public void setGenericProperty(T genericProperty) {
		this.genericProperty = genericProperty;
	}

	public List<T> getGenericListProperty() {
		return genericListProperty;
	}

	public void setGenericListProperty(List<T> genericListProperty) {
		this.genericListProperty = genericListProperty;
	}

	public CustomEnum getCustomEnum() {
		return customEnum;
	}

	public void setCustomEnum(CustomEnum customEnum) {
		this.customEnum = customEnum;
	}

	public CustomEnum[] getCustomEnumArray() {
		return customEnumArray;
	}

	public void setCustomEnumArray(CustomEnum[] customEnum) {
		this.customEnumArray = customEnum;
	}

	public Set<CustomEnum> getCustomEnumSet() {
		return customEnumSet;
	}

	public void setCustomEnumSet(Set<CustomEnum> customEnumSet) {
		this.customEnumSet = customEnumSet;
	}

	public Set<CustomEnum> getCustomEnumSetMismatch() {
		return customEnumSet;
	}

	public void setCustomEnumSetMismatch(Set<String> customEnumSet) {
		this.customEnumSet = new HashSet<>(customEnumSet.size());
		for (Iterator<String> iterator = customEnumSet.iterator(); iterator.hasNext(); ) {
			this.customEnumSet.add(CustomEnum.valueOf(iterator.next()));
		}
	}

	public EnumSet<CustomEnum> getStandardEnumSet() {
		return standardEnumSet;
	}

	public void setStandardEnumSet(EnumSet<CustomEnum> standardEnumSet) {
		this.standardEnumSet = standardEnumSet;
	}

	public EnumMap<CustomEnum, Integer> getStandardEnumMap() {
		return standardEnumMap;
	}

	public void setStandardEnumMap(EnumMap<CustomEnum, Integer> standardEnumMap) {
		this.standardEnumMap = standardEnumMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(Set<Integer> integerSet) {
		return new GenericBean(integerSet);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(Set<Integer> integerSet, List<Resource> resourceList) {
		return new GenericBean(integerSet, resourceList);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(HashSet<Integer> integerSet, Map<Short, Integer> shortMap) {
		return new GenericBean(integerSet, shortMap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(Map<Short, Integer> shortMap, Resource resource) {
		return new GenericBean(shortMap, resource);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(Map map, Map<Short, Integer> shortMap) {
		return new GenericBean(map, shortMap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(HashMap<Long, ?> longMap) {
		return new GenericBean(longMap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GenericBean createInstance(boolean someFlag, Map<Number, Collection<? extends Object>> collectionMap) {
		return new GenericBean(someFlag, collectionMap);
	}

}
