/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This method level annotation can be used to decorate a Java method that wishes to be called in order
 * to handle errors. See {@link javax.websocket.Endpoint#onError} for
 * a description of the different categories of error.
 *
 * <p>The method may only take the following parameters:-
 * <ul>
 * <li>a {@link java.lang.Throwable}  parameter</li>
 * <li>optional {@link javax.websocket.Session} parameter</li>
 * <li>optional {@link javax.websocket.EndpointConfig} parameter</li>
 * <li>optional {@link cn.taketoday.web.socket.WebSocketSession} parameter</li>
 * <li>parameters annotated with the {@link javax.websocket.server.PathParam} annotation.</li>
 * <li>parameters annotated with the {@link cn.taketoday.web.annotation.PathVariable} annotation.</li>
 * </ul>
 * <p>The parameters may appear in any order.
 *
 * @author dannycoward
 * @author TODAY 2021/5/7 12:39
 * @since 3.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnError {

}

