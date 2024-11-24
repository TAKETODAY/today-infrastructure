/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aop.framework;

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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.aop.Advisor;
import infra.aop.DefaultInterceptorChainFactory;
import infra.aop.DynamicIntroductionAdvice;
import infra.aop.InterceptorChainFactory;
import infra.aop.IntroductionAdvisor;
import infra.aop.IntroductionInfo;
import infra.aop.Pointcut;
import infra.aop.PointcutAdvisor;
import infra.aop.TargetSource;
import infra.aop.support.DefaultIntroductionAdvisor;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.aop.support.RuntimeMethodInterceptor;
import infra.aop.target.EmptyTargetSource;
import infra.aop.target.SingletonTargetSource;

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

  /** The InterceptorChainFactory to use. */
  private InterceptorChainFactory interceptorChainFactory = DefaultInterceptorChainFactory.INSTANCE;

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

  /**
   * List of minimal {@link AdvisorKeyEntry} instances,
   * to be assigned to the {@link #advisors} field on reduction.
   *
   * @see #reduceToAdvisorKey
   * @since 4.0
   */
  ArrayList<Advisor> advisorKey = this.advisors;

  /** Cache with Method as key and advisor chain List as value. */
  @Nullable
  private transient ConcurrentHashMap<MethodCacheKey, MethodInterceptor[]> methodCache;

  /**
   * Cache with shared interceptors which are not method-specific.
   *
   * @since 4.0
   */
  @Nullable
  private transient volatile MethodInterceptor[] cachedInterceptors;

  /**
   * Optional field for {@link AopProxy} implementations to store metadata in.
   * Used for {@link JdkDynamicAopProxy.ProxiedInterfacesCache}.
   *
   * @see JdkDynamicAopProxy#JdkDynamicAopProxy(AdvisedSupport)
   * @since 4.0
   */
  @Nullable
  transient volatile Object proxyMetadataCache;

  /**
   * No-arg constructor for use as a JavaBean.
   */
  public AdvisedSupport() { }

  /**
   * Create a AdvisedSupport instance with the given parameters.
   *
   * @param interfaces the proxied interfaces
   */
  public AdvisedSupport(Class<?>... interfaces) {
    setInterfaces(interfaces);
  }

  /**
   * Set the given object as target.
   * Will create a SingletonTargetSource for the object.
   *
   * @see #setTargetSource
   * @see SingletonTargetSource
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
   * <p>Internally, an {@link EmptyTargetSource}
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
   * @param ifc the additional interface to proxy
   */
  public void addInterface(Class<?> ifc) {
    Assert.notNull(ifc, "Interface is required");
    if (!ifc.isInterface()) {
      throw new IllegalArgumentException("[%s] is not an interface".formatted(ifc.getName()));
    }
    if (!this.interfaces.contains(ifc)) {
      this.interfaces.add(ifc);
      adviceChanged();
    }
  }

  /**
   * Remove a proxied interface.
   * <p>Does nothing if the given interface isn't proxied.
   *
   * @param ifc the interface to remove from the proxy
   * @return {@code true} if the interface was removed; {@code false}
   * if the interface was not found and hence could not be removed
   */
  public boolean removeInterface(Class<?> ifc) {
    return this.interfaces.remove(ifc);
  }

  @Override
  public Class<?>[] getProxiedInterfaces() {
    return ClassUtils.toClassArray(this.interfaces);
  }

  @Override
  public boolean isInterfaceProxied(Class<?> ifc) {
    for (Class<?> proxyIntf : this.interfaces) {
      if (ifc.isAssignableFrom(proxyIntf)) {
        return true;
      }
    }
    return false;
  }

  boolean hasUserSuppliedInterfaces() {
    for (Class<?> ifc : this.interfaces) {
      if (!StandardProxy.class.isAssignableFrom(ifc) && !isAdvisorIntroducedInterface(ifc)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAdvisorIntroducedInterface(Class<?> ifc) {
    for (Advisor advisor : this.advisors) {
      if (advisor instanceof IntroductionAdvisor ia) {
        for (Class<?> introducedInterface : ia.getInterfaces()) {
          if (introducedInterface == ifc) {
            return true;
          }
        }
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
   * Determine an array of {@link org.aopalliance.intercept.MethodInterceptor} objects
   * for the given method, based on this configuration.
   * <p>
   * ordered {@link MethodInterceptor} array
   * </p>
   *
   * @param method the proxied method
   * @param targetClass the target class
   * @return a array of MethodInterceptors (may also include
   * {@link RuntimeMethodInterceptor})
   * @see RuntimeMethodInterceptor
   */
  public MethodInterceptor[] getInterceptors(Method method, @Nullable Class<?> targetClass) {
    if (methodCache == null) {
      // Shared cache since there are no method-specific advisors (see below).
      MethodInterceptor[] cachedInterceptors = this.cachedInterceptors;
      if (cachedInterceptors == null) {
        cachedInterceptors = interceptorChainFactory.getInterceptors(this, method, targetClass);
        this.cachedInterceptors = cachedInterceptors;
      }
      return cachedInterceptors;
    }

    MethodCacheKey cacheKey = new MethodCacheKey(method);
    MethodInterceptor[] cached = methodCache.get(cacheKey);
    if (cached == null) {
      synchronized(this) {
        cached = methodCache.get(cacheKey);
        if (cached == null) {
          cached = interceptorChainFactory.getInterceptors(this, method, targetClass);
          methodCache.putIfAbsent(cacheKey, cached);
        }
      }
    }
    return cached;
  }

  /**
   * Invoked when advice has changed.
   */
  protected void adviceChanged() {
    this.methodCache = null;
    this.cachedInterceptors = null;
    this.proxyMetadataCache = null;

    // Initialize method cache if necessary; otherwise,
    // cachedInterceptors is going to be shared (see above).
    for (Advisor advisor : this.advisors) {
      if (advisor instanceof PointcutAdvisor) {
        this.methodCache = new ConcurrentHashMap<>();
        break;
      }
    }
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
    copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
    copy.preFiltered = this.preFiltered;
    copy.interceptorChainFactory = this.interceptorChainFactory;
    copy.interfaces = new ArrayList<>(this.interfaces);
    copy.advisors = new ArrayList<>(this.advisors);
    copy.advisorKey = new ArrayList<>(this.advisors.size());
    for (Advisor advisor : this.advisors) {
      copy.advisorKey.add(new AdvisorKeyEntry(advisor));
    }
    copy.methodCache = this.methodCache;
    copy.cachedInterceptors = this.cachedInterceptors;
    copy.proxyMetadataCache = this.proxyMetadataCache;
    return copy;
  }

  void reduceToAdvisorKey() {
    this.advisors = this.advisorKey;
    this.methodCache = null;
    this.cachedInterceptors = null;
    this.proxyMetadataCache = null;
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize method cache if necessary.
    adviceChanged();
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

  /**
   * Stub for an Advisor instance that is just needed for key purposes,
   * allowing for efficient equals and hashCode comparisons against the
   * advice class and the pointcut.
   *
   * @see #getConfigurationOnlyCopy()
   * @see #advisorKey
   * @since 4.0
   */
  private static class AdvisorKeyEntry implements Advisor {

    private final Class<?> adviceType;

    @Nullable
    private final String classFilterKey;

    @Nullable
    private final String methodMatcherKey;

    public AdvisorKeyEntry(Advisor advisor) {
      this.adviceType = advisor.getAdvice().getClass();
      if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
        Pointcut pointcut = pointcutAdvisor.getPointcut();
        this.classFilterKey = pointcut.getClassFilter().toString();
        this.methodMatcherKey = pointcut.getMethodMatcher().toString();
      }
      else {
        this.classFilterKey = null;
        this.methodMatcherKey = null;
      }
    }

    @Override
    public Advice getAdvice() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
      return this == other ||
              (other instanceof AdvisorKeyEntry otherEntry
                      && this.adviceType == otherEntry.adviceType
                      && Objects.equals(this.classFilterKey, otherEntry.classFilterKey)
                      && Objects.equals(this.methodMatcherKey, otherEntry.methodMatcherKey));
    }

    @Override
    public int hashCode() {
      return this.adviceType.hashCode();
    }

  }

}
