/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.lang.Assert;

/**
 * Convenience superclass for configuration used in creating proxies,
 * to ensure that all proxy creators have consistent properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:23
 * @see AdvisedSupport
 * @since 3.0
 */
public class ProxyConfig implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  boolean opaque = false;
  boolean exposeProxy = false;
  private boolean frozen = false;
  private boolean optimize = false;
  private boolean proxyTargetClass = false;

  /**
   * Set whether to proxy the target class directly, instead of just proxying
   * specific interfaces. Default is "false".
   * <p>Set this to "true" to force proxying for the TargetSource's exposed
   * target class. If that target class is an interface, a JDK proxy will be
   * created for the given interface. If that target class is any other class,
   * a Standard proxy will be created for the given class.
   * <p>Note: Depending on the configuration of the concrete proxy factory,
   * the proxy-target-class behavior will also be applied if no interfaces
   * have been specified (and no interface auto-detection is activated).
   *
   * @see cn.taketoday.aop.TargetSource#getTargetClass()
   */
  public void setProxyTargetClass(boolean proxyTargetClass) {
    this.proxyTargetClass = proxyTargetClass;
  }

  /**
   * Return whether to proxy the target class directly as well as any interfaces.
   */
  public boolean isProxyTargetClass() {
    return this.proxyTargetClass;
  }

  /**
   * Set whether proxies should perform aggressive optimizations.
   * The exact meaning of "aggressive optimizations" will differ
   * between proxies, but there is usually some tradeoff.
   * Default is "false".
   * <p>With current proxy options, this flag effectively
   * enforces CGLIB proxies (similar to {@link #setProxyTargetClass})
   * but without any class validation checks (for final methods etc).
   */
  public void setOptimize(boolean optimize) {
    this.optimize = optimize;
  }

  /**
   * Return whether proxies should perform aggressive optimizations.
   */
  public boolean isOptimize() {
    return this.optimize;
  }

  /**
   * Set whether proxies created by this configuration should be prevented
   * from being cast to {@link Advised} to query proxy status.
   * <p>Default is "false", meaning that any AOP proxy can be cast to
   * {@link Advised}.
   */
  public void setOpaque(boolean opaque) {
    this.opaque = opaque;
  }

  /**
   * Return whether proxies created by this configuration should be
   * prevented from being cast to {@link Advised}.
   */
  public boolean isOpaque() {
    return this.opaque;
  }

  /**
   * Set whether the proxy should be exposed by the AOP framework as a
   * ThreadLocal for retrieval via the AopContext class. This is useful
   * if an advised object needs to call another advised method on itself.
   * (If it uses {@code this}, the invocation will not be advised).
   * <p>Default is "false", in order to avoid unnecessary extra interception.
   * This means that no guarantees are provided that AopContext access will
   * work consistently within any method of the advised object.
   */
  public void setExposeProxy(boolean exposeProxy) {
    this.exposeProxy = exposeProxy;
  }

  /**
   * Return whether the AOP proxy will expose the AOP proxy for
   * each invocation.
   */
  public boolean isExposeProxy() {
    return this.exposeProxy;
  }

  /**
   * Set whether this config should be frozen.
   * <p>When a config is frozen, no advice changes can be made. This is
   * useful for optimization, and useful when we don't want callers to
   * be able to manipulate configuration after casting to Advised.
   */
  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
  }

  /**
   * Return whether the config is frozen, and no advice changes can be made.
   */
  public boolean isFrozen() {
    return this.frozen;
  }

  /**
   * Copy configuration from the other config object.
   *
   * @param other object to copy configuration from
   */
  public void copyFrom(ProxyConfig other) {
    Assert.notNull(other, "Other ProxyConfig object is required");
    this.frozen = other.frozen;
    this.opaque = other.opaque;
    this.optimize = other.optimize;
    this.exposeProxy = other.exposeProxy;
    this.proxyTargetClass = other.proxyTargetClass;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
    sb.append("optimize=").append(this.optimize).append("; ");
    sb.append("opaque=").append(this.opaque).append("; ");
    sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
    sb.append("frozen=").append(this.frozen);
    return sb.toString();
  }

}
