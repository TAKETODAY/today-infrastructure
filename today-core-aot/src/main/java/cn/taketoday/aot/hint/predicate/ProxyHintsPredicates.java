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

package cn.taketoday.aot.hint.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Assert;

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
   * Return a predicate that checks whether a {@link cn.taketoday.aot.hint.JdkProxyHint}
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
   * Return a predicate that checks whether a {@link cn.taketoday.aot.hint.JdkProxyHint}
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
