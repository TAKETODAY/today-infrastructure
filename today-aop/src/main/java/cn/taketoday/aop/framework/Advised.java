/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.framework;

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;

/**
 * Interface to be implemented by classes that hold the configuration
 * of a factory of AOP proxies. This configuration includes the
 * Interceptors and other advice, Advisors, and the proxied interfaces.
 *
 * <p>Any AOP proxy obtained from  can be cast to this interface to
 * allow manipulation of its AOP advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 19:03
 * @see AdvisedSupport
 * @since 3.0
 */
public interface Advised extends TargetClassAware {

  Advisor[] EMPTY_ADVISORS = new Advisor[0];

  /**
   * Return whether the Advised configuration is frozen,
   * in which case no advice changes can be made.
   */
  boolean isFrozen();

  /**
   * Are we proxying the full target class instead of specified interfaces?
   */
  boolean isProxyTargetClass();

  /**
   * Return the interfaces proxied by the AOP proxy.
   * <p>Will not include the target class, which may also be proxied.
   */
  Class<?>[] getProxiedInterfaces();

  /**
   * Determine whether the given interface is proxied.
   *
   * @param intf the interface to check
   */
  boolean isInterfaceProxied(Class<?> intf);

  /**
   * Change the {@code TargetSource} used by this {@code Advised} object.
   * <p>Only works if the configuration isn't {@linkplain #isFrozen frozen}.
   *
   * @param targetSource new TargetSource to use
   */
  void setTargetSource(TargetSource targetSource);

  /**
   * Return the {@code TargetSource} used by this {@code Advised} object.
   */
  TargetSource getTargetSource();

  /**
   * Set whether the proxy should be exposed by the AOP framework as a
   * {@link ThreadLocal} for retrieval via the {@link AopContext} class.
   * <p>It can be necessary to expose the proxy if an advised object needs
   * to invoke a method on itself with advice applied. Otherwise, if an
   * advised object invokes a method on {@code this}, no advice will be applied.
   * <p>Default is {@code false}, for optimal performance.
   */
  void setExposeProxy(boolean exposeProxy);

  /**
   * Return whether the factory should expose the proxy as a {@link ThreadLocal}.
   * <p>It can be necessary to expose the proxy if an advised object needs
   * to invoke a method on itself with advice applied. Otherwise, if an
   * advised object invokes a method on {@code this}, no advice will be applied.
   * <p>Getting the proxy is analogous to an EJB calling {@code getEJBObject()}.
   *
   * @see AopContext
   */
  boolean isExposeProxy();

  /**
   * Set whether this proxy configuration is pre-filtered so that it only
   * contains applicable advisors (matching this proxy's target class).
   * <p>Default is "false". Set this to "true" if the advisors have been
   * pre-filtered already, meaning that the ClassFilter check can be skipped
   * when building the actual advisor chain for proxy invocations.
   *
   * @see cn.taketoday.aop.ClassFilter
   */
  void setPreFiltered(boolean preFiltered);

  /**
   * Return whether this proxy configuration is pre-filtered so that it only
   * contains applicable advisors (matching this proxy's target class).
   */
  boolean isPreFiltered();

  /**
   * Return the advisors applying to this proxy.
   *
   * @return an array of Advisors applying to this proxy (never {@code null})
   */
  Advisor[] getAdvisors();

  /**
   * Return the number of advisors applying to this proxy.
   * <p>The default implementation delegates to {@code getAdvisors().length}.
   */
  default int getAdvisorCount() {
    return getAdvisors().length;
  }

  /**
   * Add an advisor at the end of the advisor chain.
   * <p>The Advisor may be an {@link cn.taketoday.aop.IntroductionAdvisor},
   * in which new interfaces will be available when a proxy is next obtained
   * from the relevant factory.
   *
   * @param advisor the advisor to add to the end of the chain
   * @throws AopConfigException in case of invalid advice
   */
  void addAdvisor(Advisor advisor) throws AopConfigException;

  /**
   * Add an Advisor at the specified position in the chain.
   *
   * @param advisor the advisor to add at the specified position in the chain
   * @param pos position in chain (0 is head). Must be valid.
   * @throws AopConfigException in case of invalid advice
   */
  void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

  /**
   * Remove the given advisor.
   *
   * @param advisor the advisor to remove
   * @return {@code true} if the advisor was removed; {@code false}
   * if the advisor was not found and hence could not be removed
   */
  boolean removeAdvisor(Advisor advisor);

  /**
   * Remove the advisor at the given index.
   *
   * @param index the index of advisor to remove
   * @throws AopConfigException if the index is invalid
   */
  void removeAdvisor(int index) throws AopConfigException;

  /**
   * Return the index (from 0) of the given advisor,
   * or -1 if no such advisor applies to this proxy.
   * <p>The return value of this method can be used to index into the advisors array.
   *
   * @param advisor the advisor to search for
   * @return index from 0 of this advisor, or -1 if there's no such advisor
   */
  int indexOf(Advisor advisor);

  /**
   * Replace the given advisor.
   * <p><b>Note:</b> If the advisor is an {@link IntroductionAdvisor}
   * and the replacement is not or implements different interfaces, the proxy will need
   * to be re-obtained or the old interfaces won't be supported and the new interface
   * won't be implemented.
   *
   * @param a the advisor to replace
   * @param b the advisor to replace it with
   * @return whether it was replaced. If the advisor wasn't found in the
   * list of advisors, this method returns {@code false} and does nothing.
   * @throws AopConfigException in case of invalid advice
   */
  boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

  /**
   * Add the given AOP Alliance advice to the tail of the advice (interceptor) chain.
   * <p>This will be wrapped in a DefaultPointcutAdvisor with a pointcut that always
   * applies, and returned from the {@code getAdvisors()} method in this wrapped form.
   * <p>Note that the given advice will apply to all invocations on the proxy,
   * even to the {@code toString()} method! Use appropriate advice implementations
   * or specify appropriate pointcuts to apply to a narrower set of methods.
   *
   * @param advice the advice to add to the tail of the chain
   * @throws AopConfigException in case of invalid advice
   * @see #addAdvice(int, Advice)
   * @see DefaultPointcutAdvisor
   */
  void addAdvice(Advice advice) throws AopConfigException;

  /**
   * Add the given AOP Alliance Advice at the specified position in the advice chain.
   * <p>This will be wrapped in a {@link DefaultPointcutAdvisor}
   * with a pointcut that always applies, and returned from the {@link #getAdvisors()}
   * method in this wrapped form.
   * <p>Note: The given advice will apply to all invocations on the proxy,
   * even to the {@code toString()} method! Use appropriate advice implementations
   * or specify appropriate pointcuts to apply to a narrower set of methods.
   *
   * @param pos index from 0 (head)
   * @param advice the advice to add at the specified position in the advice chain
   * @throws AopConfigException in case of invalid advice
   */
  void addAdvice(int pos, Advice advice) throws AopConfigException;

  /**
   * Remove the Advisor containing the given advice.
   *
   * @param advice the advice to remove
   * @return {@code true} of the advice was found and removed;
   * {@code false} if there was no such advice
   */
  boolean removeAdvice(Advice advice);

  /**
   * Return the index (from 0) of the given AOP Alliance Advice,
   * or -1 if no such advice is an advice for this proxy.
   * <p>The return value of this method can be used to index into
   * the advisors array.
   *
   * @param advice the AOP Alliance advice to search for
   * @return index from 0 of this advice, or -1 if there's no such advice
   */
  int indexOf(Advice advice);

  /**
   * As {@code toString()} will normally be delegated to the target,
   * this returns the equivalent for the AOP proxy.
   *
   * @return a string description of the proxy configuration
   */
  String toProxyConfigString();

}
