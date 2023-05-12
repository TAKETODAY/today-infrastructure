/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.util.function.Predicate;

import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.annotation.Order;

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
 * to the caller. See {@link AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} for more details.</li>
 * <li>Asynchronous event listener methods cannot publish a subsequent event by returning a
 * value. If you need to publish another event as the result of the processing, inject an
 * {@link ApplicationEventPublisher ApplicationEventPublisher}
 * to publish the event manually.</li>
 * </ul>
 *
 * <h3>Ordering Listeners</h3>
 * <p>It is also possible to define the order in which listeners for a
 * certain event are to be invoked. To do so, add common
 * {@link Order @Order} annotation
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
  @AliasFor("event")
  Class<?>[] value() default {};

  /**
   * The event classes that this listener handles.
   * <p>If this attribute is specified with a single value, the
   * annotated method may optionally accept a single parameter.
   * However, if this attribute is specified with multiple values,
   * the annotated method must <em>not</em> declare any parameters.
   *
   * @since 4.0
   */
  @AliasFor("value")
  Class<?>[] event() default {};

  /**
   * Expression Language (SpEL) expression used for making the event
   * handling conditional.
   * <p>The event will be handled if the expression evaluates to boolean
   * {@code true} or one of the following strings: {@code "true"}, {@code "on"},
   * {@code "yes"}, or {@code "1"}.
   * <p>The default expression is {@code ""}, meaning the event is always handled.
   * <p>The expression will be evaluated against a dedicated context that
   * provides the following metadata:
   * <ul>
   * <li>{@code #{root.event}} or {@code event} for references to the
   * {@link ApplicationEvent}</li>
   * <li>{@code #{root.args}} or {@code args} for references to the method
   * arguments array</li>
   * <li>Method arguments can be accessed by index. For example, the first
   * argument can be accessed via {@code #{root.args[0]}}, {@code args[0]},
   * {@code #{a0}}, or {@code #{p0}}.</li>
   * <li>Method arguments can be accessed by name (with a preceding hash tag)
   * if parameter names are available in the compiled byte code.</li>
   * </ul>
   *
   * @since 4.0
   */
  String condition() default "";

  /**
   * An optional identifier for the listener, defaulting to the fully-qualified
   * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
   *
   * @see SmartApplicationListener#getListenerId()
   * @see ApplicationEventMulticaster#removeApplicationListeners(Predicate)
   * @since 4.0
   */
  String id() default "";

}
