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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.StringValueResolver;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.lang.Assert;
import infra.web.service.annotation.HttpExchange;

/**
 * Implements the invocation of an {@link HttpExchange @HttpExchange}-annotated,
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpExchangeAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class HttpServiceMethod {

  private final Method method;

  private final MethodParameter[] parameters;

  private final List<HttpServiceArgumentResolver> argumentResolvers;

  private final HttpRequestValuesInitializer requestValuesInitializer;

  private final RequestExecution<HttpRequestValues> requestExecution;

  private final HttpRequestValues.Processor requestValuesProcessor;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  HttpServiceMethod(Method method, Class<?> containingClass,
          List<HttpServiceArgumentResolver> argumentResolvers, RequestExecutionFactory factory,
          @Nullable StringValueResolver embeddedValueResolver, HttpRequestValues.Processor requestValuesProcessor) {

    this.method = method;
    this.parameters = initMethodParameters(method);
    this.argumentResolvers = argumentResolvers;
    this.requestValuesProcessor = requestValuesProcessor;
    this.requestExecution = factory.createRequestExecution(method);
    this.requestValuesInitializer = HttpRequestValuesInitializer.create(method, containingClass, embeddedValueResolver, factory);
  }

  private static MethodParameter[] initMethodParameters(Method method) {
    int count = method.getParameterCount();
    if (count == 0) {
      return MethodParameter.EMPTY_ARRAY;
    }

    ParameterNameDiscoverer nameDiscoverer = ParameterNameDiscoverer.getSharedInstance();
    MethodParameter[] parameters = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      parameters[i] = new SynthesizingMethodParameter(method, i);
      parameters[i].initParameterNameDiscovery(nameDiscoverer);
    }
    return parameters;
  }

  public Method getMethod() {
    return this.method;
  }

  public @Nullable Object invoke(@Nullable Object[] arguments) {
    var requestValues = requestValuesInitializer.initialize();
    applyArguments(requestValues, arguments);
    requestValuesProcessor.process(method, parameters, arguments, requestValues);
    return requestExecution.execute(requestValues.build());
  }

  @SuppressWarnings("NullAway")
  private void applyArguments(HttpRequestValues.Builder requestValues, @Nullable Object[] arguments) {
    MethodParameter[] parameters = this.parameters;
    Assert.isTrue(arguments.length == parameters.length, "Method argument mismatch");
    for (int i = 0; i < arguments.length; i++) {
      Object value = arguments[i];
      boolean resolved = false;
      for (HttpServiceArgumentResolver resolver : this.argumentResolvers) {
        if (resolver.resolve(value, parameters[i], requestValues)) {
          resolved = true;
          break;
        }
      }
      if (!resolved) {
        throw new IllegalStateException("Could not resolve parameter [%d] in %s: No suitable resolver"
                .formatted(this.parameters[i].getParameterIndex(), this.parameters[i].getExecutable().toGenericString()));
      }
    }
  }

}
