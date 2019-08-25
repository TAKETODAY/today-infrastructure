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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author TODAY <br>
 *         2019-08-23 23:27
 */
public class MethodBasedPropertyAccessor implements PropertyAccessor {

    private final Method readMethod;
    private final Method writeMethod;

    public MethodBasedPropertyAccessor(Method readMethod, Method writeMethod) {
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    @Override
    public Object get(Object obj) {
        try {
            return readMethod.invoke(obj);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public void set(Object obj, Object value) {
        try {
            writeMethod.invoke(obj, value);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
