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

import java.util.Enumeration;
import java.util.Map;

/**
 * 
 * @author Today <br>
 *         2018-10-14 20:30
 */
public interface Model extends Map<String, Object>{

	/**
	 * Add the supplied attribute under the supplied name.
	 * 
	 * @param attributeName
	 *            the name of the model attribute (never {@code null})
	 * @param attributeValue
	 *            the model attribute value (can be {@code null})
	 */
	Model addAttribute(String attributeName, Object attributeValue);

	/**
	 * Copy all attributes in the supplied {@code Map} into this {@code Map}.
	 * 
	 * @see #addAttribute(String, Object)
	 */
	Model addAllAttributes(Map<String, Object> attributes);

	/**
	 * Does this model contain an attribute of the given name?
	 * 
	 * @param attributeName
	 *            the name of the model attribute (never {@code null})
	 * @return whether this model contains a corresponding attribute
	 */
	boolean containsAttribute(String attributeName);

	/**
	 * Return the current set of model attributes as a Map.
	 */
	Map<String, Object> asMap();
	
	/**
	 * 
	 * @param name
	 */
	void removeAttribute(String name);

	/**
	 * 
	 * @return
	 */
	Enumeration<String> getAttributeNames();
	
}
