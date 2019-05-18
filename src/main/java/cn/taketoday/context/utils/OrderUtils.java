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
package cn.taketoday.context.utils;

import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;

/**
 * 
 * @author Today <br>
 *         2018-11-08 19:02
 */
public abstract class OrderUtils {

    /**
     * @param annotated
     * @return
     */
    public static final int getOrder(AnnotatedElement annotated) {
        Order order = annotated.getAnnotation(Order.class);
        if (order != null) {
            return order.value();
        }
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * @param obj
     * @return
     */
    public static final int getOrder(Object obj) {
        if (obj instanceof Ordered) {
            return ((Ordered) obj).getOrder();
        }
        if (obj instanceof AnnotatedElement) {
            return getOrder((AnnotatedElement) obj);
        }
        return getOrder(obj.getClass());
    }

    /**
     * @return
     */
    public static Comparator<Object> getReversedComparator() {
        return Comparator.comparingInt(OrderUtils::getOrder).reversed();
    }

    /**
     * @param list
     */
    public static <T> void reversedSort(List<T> list) {
        list.sort(getReversedComparator());
    }

}
