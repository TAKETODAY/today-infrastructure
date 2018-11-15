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
package cn.taketoday.context.utils;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;

import java.lang.reflect.AnnotatedElement;

/**
 * 
 * @author Today <br>
 *         2018-11-08 19:02
 */
public abstract class OrderUtils {

	public static final int getOrder(AnnotatedElement annotated) {
		Order order = annotated.getAnnotation(Order.class);
		if (order != null) {
			return order.value();
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

	public static final int getOrder(Object obj) {
		if (obj instanceof Ordered) {
			return ((Ordered) obj).getOrder();
		}
		if (obj instanceof AnnotatedElement) {
			return getOrder((AnnotatedElement) obj);
		}
		return getOrder(obj.getClass());
	}

}
