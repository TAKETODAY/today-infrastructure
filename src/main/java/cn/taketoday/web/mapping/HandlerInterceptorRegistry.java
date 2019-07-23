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
package cn.taketoday.web.mapping;

import java.util.Arrays;
import java.util.Objects;
import java.util.RandomAccess;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * Intercept registry.
 * 
 * @author TODAY <br>
 *         2018-06-25 20:04:46
 */
@MissingBean(value = Constant.HANDLER_INTERCEPTOR_REGISTRY, type = HandlerInterceptorRegistry.class)
public class HandlerInterceptorRegistry implements RandomAccess {

    private HandlerInterceptor[] array;

    public HandlerInterceptorRegistry() {
        array = new HandlerInterceptor[0];
    }

    public int size() {
        return array.length;
    }

    public int indexOf(Class<? extends HandlerInterceptor> handlerInterceptorClass) {
        if (handlerInterceptorClass == null) {
            for (int i = 0; i < array.length; i++)
                if (array[i] == null)
                    return i;
        }
        else {
            final String className = handlerInterceptorClass.getName();
            for (int i = 0; i < array.length; i++) {
                if (className.equals(array[i].getClass().getName()))
                    return i;
            }
        }
        return -1;
    }

    public HandlerInterceptor[] toArray() {
        return array;
    }

    public final HandlerInterceptor get(int index) {
        return array[index];
    }

    public int add(HandlerInterceptor e) {

        Objects.requireNonNull(e, "HandlerInterceptor instance can't be null");

        HandlerInterceptor[] newArray = new HandlerInterceptor[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = e;
        array = newArray;

        return array.length - 1;
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }

}
