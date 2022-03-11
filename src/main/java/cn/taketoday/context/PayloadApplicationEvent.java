/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context;

import java.util.function.Consumer;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.ResolvableTypeProvider;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * An {@link ApplicationEvent} that carries an arbitrary payload.
 *
 * @param <T> the payload type of the event
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationEventPublisher#publishEvent(Object)
 * @see ApplicationListener#forPayload(Consumer)
 * @since 4.0 2022/3/11 23:07
 */
@SuppressWarnings("serial")
public class PayloadApplicationEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {

  private final T payload;

  private final ResolvableType payloadType;

  /**
   * Create a new PayloadApplicationEvent.
   *
   * @param source the object on which the event initially occurred (never {@code null})
   * @param payload the payload object (never {@code null})
   * @param payloadType the type object of payload object (can be {@code null})
   */
  public PayloadApplicationEvent(Object source, T payload, @Nullable ResolvableType payloadType) {
    super(source);
    Assert.notNull(payload, "Payload must not be null");
    this.payload = payload;
    this.payloadType = (payloadType != null) ? payloadType : ResolvableType.fromInstance(payload);
  }

  /**
   * Create a new PayloadApplicationEvent, using the instance to infer its type.
   *
   * @param source the object on which the event initially occurred (never {@code null})
   * @param payload the payload object (never {@code null})
   */
  public PayloadApplicationEvent(Object source, T payload) {
    this(source, payload, null);
  }

  @Override
  public ResolvableType getResolvableType() {
    return ResolvableType.fromClassWithGenerics(getClass(), this.payloadType);
  }

  /**
   * Return the payload of the event.
   */
  public T getPayload() {
    return this.payload;
  }

}
