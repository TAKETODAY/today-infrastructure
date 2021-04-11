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

package cn.taketoday.aop.proxy;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.context.DecoratingProxy;
import cn.taketoday.context.utils.Assert;

/**
 * Utility methods for AOP proxy factories.
 * Mainly for internal use within the AOP framework.
 *
 * <p>See {@link AopUtils} for a collection of
 * generic AOP utility methods which do not depend on AOP framework internals.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:49
 * @see AopUtils
 * @since 3.0
 */
public abstract class AopProxyUtils {

  /**
   * Obtain the singleton target object behind the given proxy, if any.
   *
   * @param candidate
   *         the (potential) proxy to check
   *
   * @return the singleton target object managed in a {@link SingletonTargetSource},
   * or {@code null} in any other case (not a proxy, not an existing singleton target)
   *
   * @see Advised#getTargetSource()
   * @see SingletonTargetSource#getTarget()
   */
  public static Object getSingletonTarget(Object candidate) {
    if (candidate instanceof Advised) {
      TargetSource targetSource = ((Advised) candidate).getTargetSource();
      if (targetSource instanceof SingletonTargetSource) {
        return ((SingletonTargetSource) targetSource).getTarget();
      }
    }
    return null;
  }

  /**
   * Determine the ultimate target class of the given bean instance, traversing
   * not only a top-level proxy but any number of nested proxies as well &mdash;
   * as long as possible without side effects, that is, just for singleton targets.
   *
   * @param candidate
   *         the instance to check (might be an AOP proxy)
   *
   * @return the ultimate target class (or the plain class of the given
   * object as fallback; never {@code null})
   *
   * @see TargetClassAware#getTargetClass()
   * @see Advised#getTargetSource()
   */
  public static Class<?> ultimateTargetClass(Object candidate) {
    Assert.notNull(candidate, "Candidate object must not be null");
    Object current = candidate;
    Class<?> result = null;
    while (current instanceof TargetClassAware) {
      result = ((TargetClassAware) current).getTargetClass();
      current = getSingletonTarget(current);
    }
    if (result == null) {
      result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
    }
    return result;
  }

  /**
   * Determine the complete set of interfaces to proxy for the given AOP configuration.
   * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
   * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
   * {@link StandardProxy} marker interface.
   *
   * @param advised
   *         the proxy config
   *
   * @return the complete set of interfaces to proxy
   *
   * @see StandardProxy
   * @see Advised
   * @since 3.0
   */
  public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
    return completeProxiedInterfaces(advised, false);
  }

  /**
   * Determine the complete set of interfaces to proxy for the given AOP configuration.
   * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
   * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
   * {@link StandardProxy} marker interface.
   *
   * @param advised
   *         the proxy config
   * @param decoratingProxy
   *         whether to expose the {@link DecoratingProxy} interface
   *
   * @return the complete set of interfaces to proxy
   *
   * @see StandardProxy
   * @see Advised
   * @see DecoratingProxy
   * @since 3.0
   */
  public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
    Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
    if (specifiedInterfaces.length == 0) {
      // No user-specified interfaces: check whether target class is an interface.
      Class<?> targetClass = advised.getTargetClass();
      if (targetClass != null) {
        if (targetClass.isInterface()) {
          advised.setInterfaces(targetClass);
        }
        else if (Proxy.isProxyClass(targetClass)) {
          advised.setInterfaces(targetClass.getInterfaces());
        }
        specifiedInterfaces = advised.getProxiedInterfaces();
      }
    }
    boolean addProxy = !advised.isInterfaceProxied(StandardProxy.class);
    boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
    boolean addDecoratingProxy = decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class);
    int nonUserIfcCount = 0;
    if (addProxy) {
      nonUserIfcCount++;
    }
    if (addAdvised) {
      nonUserIfcCount++;
    }
    if (addDecoratingProxy) {
      nonUserIfcCount++;
    }
    Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
    System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
    int index = specifiedInterfaces.length;
    if (addProxy) {
      proxiedInterfaces[index++] = StandardProxy.class;
    }
    if (addDecoratingProxy) {
      proxiedInterfaces[index++] = DecoratingProxy.class;
    }
    if (addAdvised) {
      proxiedInterfaces[index] = Advised.class;
    }
    return proxiedInterfaces;
  }

  /**
   * Extract the user-specified interfaces that the given proxy implements,
   * i.e. all non-Advised interfaces that the proxy implements.
   *
   * @param proxy
   *         the proxy to analyze (usually a JDK dynamic proxy)
   *
   * @return all user-specified interfaces that the proxy implements,
   * in the original order (never {@code null} or empty)
   *
   * @see Advised
   */
  public static Class<?>[] proxiedUserInterfaces(Object proxy) {
    Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
    int nonUserIfcCount = 0;
    if (proxy instanceof StandardProxy) {
      nonUserIfcCount++;
    }
    if (proxy instanceof Advised) {
      nonUserIfcCount++;
    }
    if (proxy instanceof DecoratingProxy) {
      nonUserIfcCount++;
    }
    Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
    Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
    return userInterfaces;
  }

  /**
   * Check equality of the proxies behind the given AdvisedSupport objects.
   * Not the same as equality of the AdvisedSupport objects:
   * rather, equality of interfaces, advisors and target sources.
   */
  public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
    return (a == b ||
            (equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
  }

  /**
   * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
   */
  public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
    return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
  }

  /**
   * Check equality of the advisors behind the given AdvisedSupport objects.
   */
  public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
    return Arrays.equals(a.getAdvisors(), b.getAdvisors());
  }

}
