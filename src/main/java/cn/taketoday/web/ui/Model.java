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
import java.util.Map;

/**
 * 
 * @author Today <br>
 *         2018-10-14 20:30
 */
public interface Model {

    /**
     * Contains a attribute with given {@link attributeName}
     * 
     * @param attributeName
     *            Attribute name
     * @return
     */
    boolean containsAttribute(String attributeName);

    /**
     * get attribute
     * 
     * @param name
     *            Attribute name
     * @return
     */
    Object getAttribute(String name);

    /**
     * Get a attribute with given name and required type
     * 
     * @param name
     *            Attribute name
     * @param targetClass
     *            Required type
     * @return
     */
    <T> T getAttribute(String name, Class<T> targetClass);

    /**
     * Add the attributes from map
     * 
     * @param attributes
     *            The attributes
     * @return
     */
    Model addAllAttributes(Map<String, Object> attributes);

    /**
     * Add a attribute in model
     * 
     * @param attributeName
     *            Attribute name
     * @param attributeValue
     *            Attribute value
     * @return
     */
    Model addAttribute(String attributeName, Object attributeValue);

    /**
     * convert this model to a {@link Map}
     * 
     * @return
     */
    Map<String, Object> asMap();

    /**
     * Delete attribute
     * 
     * @param name
     *            Attribute name
     */
    void removeAttribute(String name);

    /**
     * Get all the attribute names
     * 
     * @return
     */
    Collection<String> getAttributeNames();

    /**
     * Clear all attributes
     * 
     * @since 2.3.3
     */
    void clear();

}
