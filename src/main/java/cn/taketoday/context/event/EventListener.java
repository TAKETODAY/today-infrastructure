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

package cn.taketoday.context.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a listener for application events.
 *
 * <p>If an annotated method supports a single event type, the method may
 * declare a single parameter that reflects the event type to listen to.
 * If an annotated method supports multiple event types, this annotation
 * may refer to one or more supported event types using the {@code value}
 * attribute.
 *
 * <p>Events can be {@link ApplicationEvent} instances as well as arbitrary
 * objects.
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they
 * do, the result of the method invocation is sent as a new event. If the
 * return type is either an array or a collection, each element is sent
 * as a new individual event.
 *
 *
 * <h3>Exception Handling</h3>
 * <p>While it is possible for an event listener to declare that it
 * throws arbitrary exception types, any checked exceptions thrown
 * from an event listener will be wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}
 * since the event publisher can only handle runtime exceptions.
 *
 * <h3>Asynchronous Listeners</h3>
 * <p>If you want a particular listener to process events asynchronously, you
 * can use Async support, but be aware of the following limitations when using asynchronous events.
 *
 * <ul>
 * <li>If an asynchronous event listener throws an exception, it is not propagated
 * to the caller. See {@link cn.taketoday.aop.support.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} for more details.</li>
 * <li>Asynchronous event listener methods cannot publish a subsequent event by returning a
 * value. If you need to publish another event as the result of the processing, inject an
 * {@link cn.taketoday.context.event.ApplicationEventPublisher ApplicationEventPublisher}
 * to publish the event manually.</li>
 * </ul>
 *
 * <h3>Ordering Listeners</h3>
 * <p>It is also possible to define the order in which listeners for a
 * certain event are to be invoked. To do so, add common
 * {@link cn.taketoday.context.annotation.Order @Order} annotation
 * alongside this event listener annotation.
 *
 * @author TODAY 2021/3/16 10:53
 * @since 3.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {

  /**
   * event types
   */
  Class<?>[] value() default {};

}
