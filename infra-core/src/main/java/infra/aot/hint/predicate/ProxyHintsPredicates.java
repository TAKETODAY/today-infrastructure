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

package infra.aot.hint.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import infra.aot.hint.ProxyHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;
import infra.lang.Assert;

/**
 * Generator of {@link ProxyHints} predicates, testing whether the given hints
 * match the expected behavior for proxies.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class ProxyHintsPredicates {

  ProxyHintsPredicates() {
  }

  /**
   * Return a predicate that checks whether a {@link infra.aot.hint.JdkProxyHint}
   * is registered for the given interfaces.
   * <p>Note that the order in which interfaces are given matters.
   *
   * @param interfaces the proxied interfaces
   * @return the {@link RuntimeHints} predicate
   * @see java.lang.reflect.Proxy
   */
  public Predicate<RuntimeHints> forInterfaces(Class<?>... interfaces) {
    Assert.notEmpty(interfaces, "'interfaces' should not be empty");
    return forInterfaces(Arrays.stream(interfaces).map(TypeReference::of).toArray(TypeReference[]::new));
  }

  /**
   * Return a predicate that checks whether a {@link infra.aot.hint.JdkProxyHint}
   * is registered for the given interfaces.
   * <p>Note that the order in which interfaces are given matters.
   *
   * @param interfaces the proxied interfaces as type references
   * @return the {@link RuntimeHints} predicate
   * @see java.lang.reflect.Proxy
   */
  public Predicate<RuntimeHints> forInterfaces(TypeReference... interfaces) {
    Assert.notEmpty(interfaces, "'interfaces' should not be empty");
    List<TypeReference> interfaceList = Arrays.asList(interfaces);
    return hints -> hints.proxies().jdkProxyHints().anyMatch(proxyHint ->
            proxyHint.getProxiedInterfaces().equals(interfaceList));
  }

}
