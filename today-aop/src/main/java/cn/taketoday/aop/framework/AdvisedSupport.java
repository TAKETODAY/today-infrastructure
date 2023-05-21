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

package cn.taketoday.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.DefaultInterceptorChainFactory;
import cn.taketoday.aop.DynamicIntroductionAdvice;
import cn.taketoday.aop.InterceptorChainFactory;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.IntroductionInfo;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.target.EmptyTargetSource;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * Base class for AOP proxy configuration managers.
 * These are not themselves AOP proxies, but subclasses of this class are
 * normally factories from which AOP proxy instances are obtained directly.
 *
 * <p>This class frees subclasses of the housekeeping of Advices
 * and Advisors, but doesn't actually implement proxy creation
 * methods, which are provided by subclasses.
 *
 * <p>This class is serializable; subclasses need not be.
 * This class is used to hold snapshots of proxies.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AopProxy
 * @since 3.0 2021/2/1 20:23
 */
public class AdvisedSupport extends ProxyConfig implements Advised {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Canonical TargetSource when there's no target, and behavior is
   * supplied by the advisors.
   */
  public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;

  /** Package-protected to allow direct access for efficiency. */
  TargetSource targetSource = EMPTY_TARGET_SOURCE;

  /** Whether the Advisors are already filtered for the specific target class. */
  private boolean preFiltered = false;

  /** Cache with Method as key and advisor chain List as value. */
  private transient ConcurrentHashMap<MethodCacheKey, MethodInterceptor[]> methodCache;

  /**
   * Interfaces to be implemented by the proxy. Held in List to keep the order
   * of registration, to create JDK proxy with specified order of interfaces.
   */
  private ArrayList<Class<?>> interfaces = new ArrayList<>();

  /**
   * List of Advisors. If Advice is added, it will be wrapped
   * in an Advisor before being added to this List.
   */
  private ArrayList<Advisor> advisors = new ArrayList<>();

  /** The InterceptorChainFactory to use. */
  private InterceptorChainFactory interceptorChainFactory = DefaultInterceptorChainFactory.INSTANCE;

  /**
   * No-arg constructor for use as a JavaBean.
   */
  public AdvisedSupport() {
    this.methodCache = new ConcurrentHashMap<>(32);
  }

  /**
   * Create a AdvisedSupport instance with the given parameters.
   *
   * @param interfaces the proxied interfaces
   */
  public AdvisedSupport(Class<?>... interfaces) {
    this();
    setInterfaces(interfaces);
  }

  /**
   * Set the given object as target.
   * Will create a SingletonTargetSource for the object.
   *
   * @see #setTargetSource
   * @see cn.taketoday.aop.target.SingletonTargetSource
   */
  public void setTarget(Object target) {
    setTargetSource(new SingletonTargetSource(target));
  }

  @Override
  public void setTargetSource(@Nullable TargetSource targetSource) {
    this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
  }

  @Override
  public TargetSource getTargetSource() {
    return this.targetSource;
  }

  /**
   * Set the advisor chain factory to use.
   * <p>Default is a {@link DefaultInterceptorChainFactory}.
   *
   * @since 4.0
   */
  public void setInterceptorChainFactory(InterceptorChainFactory interceptorChainFactory) {
    Assert.notNull(interceptorChainFactory, "AdvisorChainFactory is required");
    this.interceptorChainFactory = interceptorChainFactory;
  }

  /**
   * Return the advisor chain factory to use (never {@code null}).
   *
   * @since 4.0
   */
  public InterceptorChainFactory getInterceptorChainFactory() {
    return this.interceptorChainFactory;
  }

  /**
   * Set a target class to be proxied, indicating that the proxy
   * should be cast-able to the given class.
   * <p>Internally, an {@link cn.taketoday.aop.target.EmptyTargetSource}
   * for the given target class will be used. The kind of proxy needed
   * will be determined on actual creation of the proxy.
   * <p>This is a replacement for setting a "targetSource" or "target",
   * for the case where we want a proxy based on a target class
   * (which can be an interface or a concrete class) without having
   * a fully capable TargetSource available.
   *
   * @see #setTargetSource
   * @see #setTarget
   */
  public void setTargetClass(Class<?> targetClass) {
    this.targetSource = EmptyTargetSource.forClass(targetClass);
  }

  @Override
  @Nullable
  public Class<?> getTargetClass() {
    return this.targetSource.getTargetClass();
  }

  @Override
  public void setPreFiltered(boolean preFiltered) {
    this.preFiltered = preFiltered;
  }

  @Override
  public boolean isPreFiltered() {
    return this.preFiltered;
  }

  /**
   * Set the interfaces to be proxied.
   */
  public void setInterfaces(Class<?>... interfaces) {
    Assert.notNull(interfaces, "Interfaces is required");
    this.interfaces.clear();
    for (Class<?> ifc : interfaces) {
      addInterface(ifc);
    }
  }

  /**
   * Add a new proxied interface.
   *
   * @param intf the additional interface to proxy
   */
  public void addInterface(Class<?> intf) {
    Assert.notNull(intf, "Interface is required");
    if (!intf.isInterface()) {
      throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
    }
    if (!this.interfaces.contains(intf)) {
      this.interfaces.add(intf);
      adviceChanged();
    }
  }

  /**
   * Remove a proxied interface.
   * <p>Does nothing if the given interface isn't proxied.
   *
   * @param intf the interface to remove from the proxy
   * @return {@code true} if the interface was removed; {@code false}
   * if the interface was not found and hence could not be removed
   */
  public boolean removeInterface(Class<?> intf) {
    return this.interfaces.remove(intf);
  }

  @Override
  public Class<?>[] getProxiedInterfaces() {
    return ClassUtils.toClassArray(this.interfaces);
  }

  @Override
  public boolean isInterfaceProxied(Class<?> intf) {
    for (Class<?> proxyIntf : this.interfaces) {
      if (intf.isAssignableFrom(proxyIntf)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final Advisor[] getAdvisors() {
    return advisors.toArray(new Advisor[0]);
  }

  @Override
  public int getAdvisorCount() {
    return advisors.size();
  }

  @Override
  public void addAdvisor(Advisor advisor) {
    int pos = this.advisors.size();
    addAdvisor(pos, advisor);
  }

  @Override
  public void addAdvisor(int pos, Advisor advisor) {
    if (advisor instanceof IntroductionAdvisor) {
      validateIntroductionAdvisor((IntroductionAdvisor) advisor);
    }
    addAdvisorInternal(pos, advisor);
  }

  @Override
  public boolean removeAdvisor(Advisor advisor) {
    int index = indexOf(advisor);
    if (index == -1) {
      return false;
    }
    else {
      removeAdvisor(index);
      return true;
    }
  }

  @Override
  public void removeAdvisor(int index) {
    if (isFrozen()) {
      throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
    }
    if (index < 0 || index > this.advisors.size() - 1) {
      throw new AopConfigException(
              "Advisor index " + index + " is out of bounds: " +
                      "This configuration only has " + this.advisors.size() + " advisors.");
    }

    Advisor advisor = this.advisors.remove(index);
    if (advisor instanceof IntroductionAdvisor ia) {
      // We need to remove introduction interfaces.
      for (Class<?> ifc : ia.getInterfaces()) {
        removeInterface(ifc);
      }
    }

    adviceChanged();
  }

  @Override
  public int indexOf(Advisor advisor) {
    Assert.notNull(advisor, "Advisor is required");
    return this.advisors.indexOf(advisor);
  }

  @Override
  public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
    Assert.notNull(a, "Advisor 'a' is required");
    Assert.notNull(b, "Advisor 'b' is required");
    int index = indexOf(a);
    if (index == -1) {
      return false;
    }
    removeAdvisor(index);
    addAdvisor(index, b);
    return true;
  }

  /**
   * Add all of the given advisors to this proxy configuration.
   *
   * @param advisors the advisors to register
   */
  public void addAdvisors(Advisor... advisors) {
    addAdvisors(Arrays.asList(advisors));
  }

  /**
   * Add all of the given advisors to this proxy configuration.
   *
   * @param advisors the advisors to register
   */
  public void addAdvisors(Collection<Advisor> advisors) {
    if (isFrozen()) {
      throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
    }
    if (CollectionUtils.isNotEmpty(advisors)) {
      for (Advisor advisor : advisors) {
        Assert.notNull(advisor, "Advisor is required");
        if (advisor instanceof IntroductionAdvisor) {
          validateIntroductionAdvisor((IntroductionAdvisor) advisor);
        }
        this.advisors.add(advisor);
      }
      adviceChanged();
    }
  }

  private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
    advisor.validateInterfaces();
    // If the advisor passed validation, we can make the change.
    Class<?>[] ifcs = advisor.getInterfaces();
    for (Class<?> ifc : ifcs) {
      addInterface(ifc);
    }
  }

  void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
    Assert.notNull(advisor, "Advisor is required");
    if (isFrozen()) {
      throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
    }
    if (pos > this.advisors.size()) {
      throw new IllegalArgumentException(
              "Illegal position " + pos + " in advisor list with size " + this.advisors.size());
    }
    this.advisors.add(pos, advisor);
    adviceChanged();
  }

  /**
   * Allows uncontrolled access to the {@link List} of {@link Advisor Advisors}.
   * <p>Use with care, and remember to {@link #adviceChanged() fire advice changed events}
   * when making any modifications.
   */
  protected final List<Advisor> getAdvisorsInternal() {
    return this.advisors;
  }

  @Override
  public void addAdvice(Advice advice) throws AopConfigException {
    int pos = this.advisors.size();
    addAdvice(pos, advice);
  }

  /**
   * Cannot add introductions this way unless the advice implements IntroductionInfo.
   */
  @Override
  public void addAdvice(int pos, Advice advice) throws AopConfigException {
    Assert.notNull(advice, "Advice is required");
    if (advice instanceof IntroductionInfo) {
      // We don't need an IntroductionAdvisor for this kind of introduction:
      // It's fully self-describing.
      addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
    }
    else if (advice instanceof DynamicIntroductionAdvice) {
      // We need an IntroductionAdvisor for this kind of introduction.
      throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
    }
    else {
      addAdvisor(pos, new DefaultPointcutAdvisor(advice));
    }
  }

  @Override
  public boolean removeAdvice(Advice advice) throws AopConfigException {
    int index = indexOf(advice);
    if (index == -1) {
      return false;
    }
    else {
      removeAdvisor(index);
      return true;
    }
  }

  @Override
  public int indexOf(Advice advice) {
    Assert.notNull(advice, "Advice is required");
    final ArrayList<Advisor> advisors = this.advisors;
    for (int i = 0; i < advisors.size(); i++) {
      Advisor advisor = advisors.get(i);
      if (advisor.getAdvice() == advice) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Is the given advice included in any advisor within this proxy configuration?
   *
   * @param advice the advice to check inclusion of
   * @return whether this advice instance is included
   */
  public boolean adviceIncluded(@Nullable Advice advice) {
    if (advice != null) {
      for (Advisor advisor : this.advisors) {
        if (advisor.getAdvice() == advice) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Count advices of the given class.
   *
   * @param adviceClass the advice class to check
   * @return the count of the interceptors of this class or subclasses
   */
  public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
    int count = 0;
    if (adviceClass != null) {
      for (Advisor advisor : this.advisors) {
        if (adviceClass.isInstance(advisor.getAdvice())) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
   * for the given method, based on this configuration.
   * <p>
   * ordered {@link MethodInterceptor} array
   * </p>
   *
   * @param method the proxied method
   * @param targetClass the target class
   * @return a array of MethodInterceptors (may also include
   * {@link cn.taketoday.aop.support.RuntimeMethodInterceptor})
   * @see cn.taketoday.aop.support.RuntimeMethodInterceptor
   */
  public MethodInterceptor[] getInterceptors(Method method, @Nullable Class<?> targetClass) {
    MethodCacheKey cacheKey = new MethodCacheKey(method);
    MethodInterceptor[] cached = this.methodCache.get(cacheKey);
    if (cached == null) {
      cached = interceptorChainFactory.getInterceptors(this, method, targetClass);
      this.methodCache.put(cacheKey, cached);
    }
    return cached;
  }

  /**
   * Invoked when advice has changed.
   */
  protected void adviceChanged() {
    this.methodCache.clear();
  }

  /**
   * Call this method on a new instance created by the no-arg constructor
   * to create an independent copy of the configuration from the given object.
   *
   * @param other the AdvisedSupport object to copy configuration from
   */
  protected void copyConfigurationFrom(AdvisedSupport other) {
    copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
  }

  /**
   * Copy the AOP configuration from the given AdvisedSupport object,
   * but allow substitution of a fresh TargetSource and a given interceptor chain.
   *
   * @param other the AdvisedSupport object to take proxy configuration from
   * @param targetSource the new TargetSource
   * @param advisors the Advisors for the chain
   */
  protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
    copyFrom(other);
    this.targetSource = targetSource;
    this.interceptorChainFactory = other.interceptorChainFactory;
    this.interfaces = new ArrayList<>(other.interfaces);
    for (Advisor advisor : advisors) {
      Assert.notNull(advisor, "Advisor is required");
      if (advisor instanceof IntroductionAdvisor) {
        validateIntroductionAdvisor((IntroductionAdvisor) advisor);
      }
      this.advisors.add(advisor);
    }
    adviceChanged();
  }

  /**
   * Build a configuration-only copy of this AdvisedSupport,
   * replacing the TargetSource.
   */
  AdvisedSupport getConfigurationOnlyCopy() {
    AdvisedSupport copy = new AdvisedSupport();
    copy.copyFrom(this);
    copy.advisors = new ArrayList<>(this.advisors);
    copy.interfaces = new ArrayList<>(this.interfaces);
    copy.interceptorChainFactory = this.interceptorChainFactory;
    copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
    return copy;
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    this.methodCache = new ConcurrentHashMap<>(32);
  }

  @Override
  public String toProxyConfigString() {
    return toString();
  }

  /**
   * For debugging/diagnostic use.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
    sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
    sb.append(this.advisors.size()).append(" advisors ");
    sb.append(this.advisors).append("; ");
    sb.append("targetSource [").append(this.targetSource).append("]; ");
    sb.append(super.toString());
    return sb.toString();
  }

  /**
   * Simple wrapper class around a Method. Used as the key when
   * caching methods, for efficient equals and hashCode comparisons.
   */
  static final class MethodCacheKey implements Comparable<MethodCacheKey> {
    private final int hashCode;
    private final Method method;

    public MethodCacheKey(Method method) {
      this.method = method;
      this.hashCode = method.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return (this == other
              || (other instanceof MethodCacheKey && this.method == ((MethodCacheKey) other).method));
    }

    @Override
    public int hashCode() {
      return this.hashCode;
    }

    @Override
    public String toString() {
      return this.method.toString();
    }

    @Override
    public int compareTo(MethodCacheKey other) {
      int result = this.method.getName().compareTo(other.method.getName());
      if (result == 0) {
        result = this.method.toString().compareTo(other.method.toString());
      }
      return result;
    }
  }

}
