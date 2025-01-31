/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aot.hint.support;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint;
import infra.util.ClassUtils;

/**
 * Utilities for core hint inference on Infra-managed classes,
 * specifically for proxy types such as interface-based JDK proxies
 * and CGLIB-generated subclasses which need proxy/reflection hints.
 *
 * <p>Note that this class does not take specifics of Infra AOP or
 * any other framework arrangement into account. It just operates
 * on the JDK and CGLIB proxy facilities and their core conventions.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.aot.hint.ProxyHints
 * @see infra.aot.hint.ReflectionHints
 * @since 4.0
 */
public abstract class ClassHintUtils {

  private static final Consumer<TypeHint.Builder> asClassBasedProxy = hint ->
          hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                  MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.ACCESS_DECLARED_FIELDS);

  private static final Consumer<TypeHint.Builder> asProxiedUserClass = hint ->
          hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS);

  /**
   * Register a proxy hint for a JDK proxy or corresponding reflection hints
   * for a CGLIB-generated subclass, if necessary.
   *
   * @param candidateClass the class to introspect
   * @param runtimeHints the RuntimeHints instance to register the hints on
   * @see Proxy#isProxyClass(Class)
   * @see ClassUtils#getUserClass(Class)
   */
  public static void registerProxyIfNecessary(Class<?> candidateClass, RuntimeHints runtimeHints) {
    if (Proxy.isProxyClass(candidateClass)) {
      // A JDK proxy class needs an explicit hint
      runtimeHints.proxies().registerJdkProxy(candidateClass.getInterfaces());
    }
    else {
      // Potentially a CGLIB-generated subclass with reflection hints
      Class<?> userClass = ClassUtils.getUserClass(candidateClass);
      if (userClass != candidateClass) {
        runtimeHints.reflection()
                .registerType(candidateClass, asClassBasedProxy)
                .registerType(userClass, asProxiedUserClass);
      }
    }
  }

}
