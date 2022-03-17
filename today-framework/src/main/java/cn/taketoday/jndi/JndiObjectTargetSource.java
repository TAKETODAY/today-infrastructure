/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.jndi;

import javax.naming.NamingException;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactoryBean;
import cn.taketoday.lang.Nullable;

/**
 * AOP {@link cn.taketoday.aop.TargetSource} that provides
 * configurable JNDI lookups for {@code getTarget()} calls.
 *
 * <p>Can be used as alternative to {@link JndiObjectFactoryBean}, to allow for
 * relocating a JNDI object lazily or for each operation (see "lookupOnStartup"
 * and "cache" properties). This is particularly useful during development, as it
 * allows for hot restarting of the JNDI server (for example, a remote JMS server).
 *
 * <p>Example:
 *
 * <pre class="code">
 * &lt;bean id="queueConnectionFactoryTarget" class="cn.taketoday.jndi.JndiObjectTargetSource"&gt;
 *   &lt;property name="jndiName" value="JmsQueueConnectionFactory"/&gt;
 *   &lt;property name="lookupOnStartup" value="false"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="queueConnectionFactory" class="cn.taketoday.aop.framework.ProxyFactoryBean"&gt;
 *   &lt;property name="proxyInterfaces" value="jakarta.jms.QueueConnectionFactory"/&gt;
 *   &lt;property name="targetSource" ref="queueConnectionFactoryTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * A {@code createQueueConnection} call on the "queueConnectionFactory" proxy will
 * cause a lazy JNDI lookup for "JmsQueueConnectionFactory" and a subsequent delegating
 * call to the retrieved QueueConnectionFactory's {@code createQueueConnection}.
 *
 * <p><b>Alternatively, use a {@link JndiObjectFactoryBean} with a "proxyInterface".</b>
 * "lookupOnStartup" and "cache" can then be specified on the JndiObjectFactoryBean,
 * creating a JndiObjectTargetSource underneath (instead of defining separate
 * ProxyFactoryBean and JndiObjectTargetSource beans).
 *
 * @author Juergen Hoeller
 * @see #setLookupOnStartup
 * @see #setCache
 * @see ProxyFactoryBean#setTargetSource
 * @see JndiObjectFactoryBean#setProxyInterface
 * @since 4.0
 */
public class JndiObjectTargetSource extends JndiObjectLocator implements TargetSource {

  private boolean lookupOnStartup = true;

  private boolean cache = true;

  @Nullable
  private Object cachedObject;

  @Nullable
  private Class<?> targetClass;

  /**
   * Set whether to look up the JNDI object on startup. Default is "true".
   * <p>Can be turned off to allow for late availability of the JNDI object.
   * In this case, the JNDI object will be fetched on first access.
   *
   * @see #setCache
   */
  public void setLookupOnStartup(boolean lookupOnStartup) {
    this.lookupOnStartup = lookupOnStartup;
  }

  /**
   * Set whether to cache the JNDI object once it has been located.
   * Default is "true".
   * <p>Can be turned off to allow for hot redeployment of JNDI objects.
   * In this case, the JNDI object will be fetched for each invocation.
   *
   * @see #setLookupOnStartup
   */
  public void setCache(boolean cache) {
    this.cache = cache;
  }

  @Override
  public void afterPropertiesSet() throws NamingException {
    super.afterPropertiesSet();
    if (this.lookupOnStartup) {
      Object object = lookup();
      if (this.cache) {
        this.cachedObject = object;
      }
      else {
        this.targetClass = object.getClass();
      }
    }
  }

  @Override
  @Nullable
  public Class<?> getTargetClass() {
    if (this.cachedObject != null) {
      return this.cachedObject.getClass();
    }
    else if (this.targetClass != null) {
      return this.targetClass;
    }
    else {
      return getExpectedType();
    }
  }

  @Override
  public boolean isStatic() {
    return (this.cachedObject != null);
  }

  @Override
  @Nullable
  public Object getTarget() {
    try {
      if (this.lookupOnStartup || !this.cache) {
        return (this.cachedObject != null ? this.cachedObject : lookup());
      }
      else {
        synchronized(this) {
          if (this.cachedObject == null) {
            this.cachedObject = lookup();
          }
          return this.cachedObject;
        }
      }
    }
    catch (NamingException ex) {
      throw new JndiLookupFailureException("JndiObjectTargetSource failed to obtain new target object", ex);
    }
  }

  @Override
  public void releaseTarget(Object target) { }

}
