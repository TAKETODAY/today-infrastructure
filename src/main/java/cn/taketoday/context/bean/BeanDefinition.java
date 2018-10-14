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
package cn.taketoday.context.bean;

import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.NoSuchPropertyException;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author Today <br>
 *         2018-06-23 11:23:45
 */
@Getter
@Setter
@NoArgsConstructor
public final class BeanDefinition {

	/**
	 *  bean name
	 *  
	 */
	private String					name;
	/** bean class. */
	private Class<? extends Object>	beanClass;
	/** bean scope. */
	private Scope					scope	= Scope.SINGLETON;
	
	// private Method initMethod;

	/** property values */
	private PropertyValue[]			propertyValues;
	
	/**
	 * <p>
	 * This is a very important property.
	 * <p>
	 * If registry contains it's singleton instance, we don't know the instance has
	 * initialized or not, so must be have a flag to prove it has initialized
	 * 
	 */
	private boolean 				initialized = false;

	/**
	 * Mark as a FactoryBean.
	 */
	private boolean 				factoryBean = false;
	
	/**
	 * Is Abstract ?
	 */
	private boolean 				Abstract 	= false;
	
	
	
	/**
	 * Add PropertyValue to list.
	 * 
	 * @param propertyValue
	 */
	public void addPropertyValue(PropertyValue ... propertyValues_) {
		if (propertyValues == null) {
			propertyValues = propertyValues_ ;
			return ;
		}
		List<PropertyValue> asList = Arrays.asList(propertyValues);
		for (PropertyValue propertyValue : propertyValues_) {
			asList.add(propertyValue);
		}
		propertyValues = asList.toArray(new PropertyValue[0]);
	}
	
	/**
	 * 
	 * @param propertyValues_
	 */
	public void addPropertyValue(List<PropertyValue> propertyValues_) {
		
		if (propertyValues == null) {
			propertyValues = propertyValues_.toArray(new PropertyValue[0]) ;
			return ;
		}
		propertyValues_.addAll(Arrays.asList(propertyValues));
		
		propertyValues = propertyValues_.toArray(new PropertyValue[0]);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchPropertyException
	 */
	public PropertyValue getPropertyValue(String name) throws NoSuchPropertyException {
		
		for (PropertyValue propertyValue : propertyValues) {
			if (propertyValue.getField().getName().equals(name)) {
				return propertyValue;
			}
		}
		throw new NoSuchPropertyException("No such property named -> [" + name + "]");
	}

	public boolean isSingleton() {
		return scope == Scope.SINGLETON;
	}

	public BeanDefinition(String name, Class<? extends Object> beanClass, Scope scope) {
		this.name = name;
		this.scope = scope;
		this.beanClass = beanClass;
	}
 
	public BeanDefinition(String name, Class<? extends Object> beanClass) {
		this.beanClass = beanClass;
		this.name = name;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"name\":\"")//
				.append(name)//
				.append("\",\n\t\"beanClass\":\"")//
				.append(beanClass)//
				.append("\",\n\t\"scope\":\"")//
				.append(scope)//
				.append("\",\n\t\"propertyValues\":\"")//
				.append(Arrays.toString(propertyValues))//
				.append("\"\n}")//
				.toString();
	}
}
