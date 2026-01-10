/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.ResolvableTypeProvider;
import infra.lang.Assert;

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
   * Create a new PayloadApplicationEvent, using the instance to infer its type.
   *
   * @param source the object on which the event initially occurred (never {@code null})
   * @param payload the payload object (never {@code null})
   */
  public PayloadApplicationEvent(Object source, T payload) {
    this(source, payload, null);
  }

  /**
   * Create a new PayloadApplicationEvent based on the provided payload type.
   *
   * @param source the object on which the event initially occurred (never {@code null})
   * @param payload the payload object (never {@code null})
   * @param payloadType the type object of payload object (can be {@code null}).
   * Note that this is meant to indicate the payload type (e.g. {@code String}),
   * not the full event type (such as {@code PayloadApplicationEvent<&lt;String&gt;}).
   */
  public PayloadApplicationEvent(Object source, T payload, @Nullable ResolvableType payloadType) {
    super(source);
    Assert.notNull(payload, "Payload is required");
    this.payload = payload;
    this.payloadType = payloadType != null ? payloadType : ResolvableType.forInstance(payload);
  }

  @Nullable
  @Override
  public ResolvableType getResolvableType() {
    return ResolvableType.forClassWithGenerics(getClass(), this.payloadType);
  }

  /**
   * Return the payload of the event.
   */
  public T getPayload() {
    return this.payload;
  }

}
