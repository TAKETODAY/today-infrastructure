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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.jdbc.mapping;

/**
 * @author TODAY <br>
 *         2019-08-23 23:08
 */
public interface PropertyAccessor {

    /**
     * @param obj
     *            object from which the represented field's value is to be extracted
     * @return the value of the represented field in object {@code obj}; primitive
     *         values are wrapped in an appropriate object before being returned
     */
    Object get(Object obj);

    /**
     * @param obj
     *            the object whose field should be modified
     * @param value
     *            the new value for the field of {@code obj}
     */
    void set(Object obj, Object value);
}
