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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Today
 * @date 2018年6月23日 上午11:29:23
 * 包装一个对象所有的PropertyValue。<br/>
 * 为什么封装而不是直接用List?因为可以封装一些操作。
 */
public class PropertyValues {

	private final List<PropertyValue> propertyValueList = new ArrayList<PropertyValue>();

	public PropertyValues() {
		
	}

	public void addPropertyValue(PropertyValue pv) {
        //TODO:这里可以对于重复propertyName进行判断，直接用list没法做到
		this.propertyValueList.add(pv);
	}

	public List<PropertyValue> getPropertyValues() {
		return this.propertyValueList;
	}

	@Override
	public String toString() {
		return " {\"propertyValueList\":\"" + propertyValueList + "\"}";
	}
	
}
