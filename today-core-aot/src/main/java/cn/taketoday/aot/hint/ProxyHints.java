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

package cn.taketoday.aot.hint;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gather the need for using proxies at runtime.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class ProxyHints {

  private final Set<JdkProxyHint> jdkProxies = new LinkedHashSet<>();

  /**
   * Return the interface-based proxies that are required.
   *
   * @return a stream of {@link JdkProxyHint}
   */
  public Stream<JdkProxyHint> jdkProxyHints() {
    return this.jdkProxies.stream();
  }

  /**
   * Register a {@link JdkProxyHint}.
   *
   * @param jdkProxyHint the consumer of the hint builder
   * @return {@code this}, to facilitate method chaining
   */
  public ProxyHints registerJdkProxy(Consumer<JdkProxyHint.Builder> jdkProxyHint) {
    JdkProxyHint.Builder builder = new JdkProxyHint.Builder();
    jdkProxyHint.accept(builder);
    this.jdkProxies.add(builder.build());
    return this;
  }

  /**
   * Register that a JDK proxy implementing the interfaces defined by the
   * specified {@linkplain TypeReference type references} is required.
   *
   * @param proxiedInterfaces the type references for the interfaces the proxy
   * should implement
   * @return {@code this}, to facilitate method chaining
   */
  public ProxyHints registerJdkProxy(TypeReference... proxiedInterfaces) {
    return registerJdkProxy(jdkProxyHint ->
            jdkProxyHint.proxiedInterfaces(proxiedInterfaces));
  }

  /**
   * Register that a JDK proxy implementing the specified interfaces is
   * required.
   * <p>When registering a JDK proxy for Infra AOP, consider using
   * {@link cn.taketoday.aop.framework.AopProxyUtils#completeJdkProxyInterfaces(Class...)
   * AopProxyUtils.completeJdkProxyInterfaces()} for convenience.
   *
   * @param proxiedInterfaces the interfaces the proxy should implement
   * @return {@code this}, to facilitate method chaining
   */
  public ProxyHints registerJdkProxy(Class<?>... proxiedInterfaces) {
    return registerJdkProxy(jdkProxyHint ->
            jdkProxyHint.proxiedInterfaces(proxiedInterfaces));
  }

}
