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

package infra.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.util.ReflectionUtils;

/**
 * Base class for {@link RSocketServiceArgumentResolver} test fixtures.
 *
 * @author Rossen Stoyanchev
 */
public abstract class RSocketServiceArgumentResolverTestSupport {

  private @Nullable RSocketServiceArgumentResolver resolver;

  private final RSocketRequestValues.Builder requestValuesBuilder = RSocketRequestValues.builder(null);

  private @Nullable RSocketRequestValues requestValues;

  protected RSocketServiceArgumentResolverTestSupport() {
    this.resolver = initResolver();
  }

  protected abstract RSocketServiceArgumentResolver initResolver();

  protected static MethodParameter initMethodParameter(Class<?> serviceClass, String methodName, int index) {
    Method method = ReflectionUtils.getMethod(serviceClass, methodName, (Class<?>[]) null);
    return new MethodParameter(method, index);
  }

  protected boolean execute(Object payload, MethodParameter parameter) {
    return this.resolver.resolve(payload, parameter, this.requestValuesBuilder);
  }

  protected RSocketRequestValues getRequestValues() {
    this.requestValues = (this.requestValues != null ? this.requestValues : this.requestValuesBuilder.build());
    return this.requestValues;
  }

}
