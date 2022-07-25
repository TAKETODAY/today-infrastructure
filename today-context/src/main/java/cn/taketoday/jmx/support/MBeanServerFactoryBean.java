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

package cn.taketoday.jmx.support;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jmx.MBeanServerNotFoundException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link FactoryBean} that obtains a {@link MBeanServer} reference
 * through the standard JMX 1.2 {@link MBeanServerFactory}
 * API.
 *
 * <p>Exposes the {@code MBeanServer} for bean references.
 *
 * <p>By default, {@code MBeanServerFactoryBean} will always create
 * a new {@code MBeanServer} even if one is already running. To have
 * the {@code MBeanServerFactoryBean} attempt to locate a running
 * {@code MBeanServer} first, set the value of the
 * "locateExistingServerIfPossible" property to "true".
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #setLocateExistingServerIfPossible
 * @see #locateMBeanServer
 * @see MBeanServer
 * @see MBeanServerFactory#findMBeanServer
 * @see MBeanServerFactory#createMBeanServer
 * @see MBeanServerFactory#newMBeanServer
 * @see MBeanServerConnectionFactoryBean
 * @see ConnectorServerFactoryBean
 * @since 4.0
 */
public class MBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean, DisposableBean {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private boolean locateExistingServerIfPossible = false;

  @Nullable
  private String agentId;

  @Nullable
  private String defaultDomain;

  private boolean registerWithFactory = true;

  @Nullable
  private MBeanServer server;

  private boolean newlyRegistered = false;

  /**
   * Set whether or not the {@code MBeanServerFactoryBean} should attempt
   * to locate a running {@code MBeanServer} before creating one.
   * <p>Default is {@code false}.
   */
  public void setLocateExistingServerIfPossible(boolean locateExistingServerIfPossible) {
    this.locateExistingServerIfPossible = locateExistingServerIfPossible;
  }

  /**
   * Set the agent id of the {@code MBeanServer} to locate.
   * <p>Default is none. If specified, this will result in an
   * automatic attempt being made to locate the attendant MBeanServer,
   * and (importantly) if said MBeanServer cannot be located no
   * attempt will be made to create a new MBeanServer (and an
   * MBeanServerNotFoundException will be thrown at resolution time).
   * <p>Specifying the empty String indicates the platform MBeanServer.
   *
   * @see MBeanServerFactory#findMBeanServer(String)
   */
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
   * Set the default domain to be used by the {@code MBeanServer},
   * to be passed to {@code MBeanServerFactory.createMBeanServer()}
   * or {@code MBeanServerFactory.findMBeanServer()}.
   * <p>Default is none.
   *
   * @see MBeanServerFactory#createMBeanServer(String)
   * @see MBeanServerFactory#findMBeanServer(String)
   */
  public void setDefaultDomain(String defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  /**
   * Set whether to register the {@code MBeanServer} with the
   * {@code MBeanServerFactory}, making it available through
   * {@code MBeanServerFactory.findMBeanServer()}.
   * <p>Default is {@code true}.
   *
   * @see MBeanServerFactory#createMBeanServer
   * @see MBeanServerFactory#findMBeanServer
   */
  public void setRegisterWithFactory(boolean registerWithFactory) {
    this.registerWithFactory = registerWithFactory;
  }

  /**
   * Creates the {@code MBeanServer} instance.
   */
  @Override
  public void afterPropertiesSet() throws MBeanServerNotFoundException {
    // Try to locate existing MBeanServer, if desired.
    if (this.locateExistingServerIfPossible || this.agentId != null) {
      try {
        this.server = locateMBeanServer(this.agentId);
      }
      catch (MBeanServerNotFoundException ex) {
        // If agentId was specified, we were only supposed to locate that
        // specific MBeanServer; so let's bail if we can't find it.
        if (this.agentId != null) {
          throw ex;
        }
        logger.debug("No existing MBeanServer found - creating new one");
      }
    }

    // Create a new MBeanServer and register it, if desired.
    if (this.server == null) {
      this.server = createMBeanServer(this.defaultDomain, this.registerWithFactory);
      this.newlyRegistered = this.registerWithFactory;
    }
  }

  /**
   * Attempt to locate an existing {@code MBeanServer}.
   * Called if {@code locateExistingServerIfPossible} is set to {@code true}.
   * <p>The default implementation attempts to find an {@code MBeanServer} using
   * a standard lookup. Subclasses may override to add additional location logic.
   *
   * @param agentId the agent identifier of the MBeanServer to retrieve.
   * If this parameter is {@code null}, all registered MBeanServers are
   * considered.
   * @return the {@code MBeanServer} if found
   * @throws MBeanServerNotFoundException if no {@code MBeanServer} could be found
   * @see #setLocateExistingServerIfPossible
   * @see JmxUtils#locateMBeanServer(String)
   * @see MBeanServerFactory#findMBeanServer(String)
   */
  protected MBeanServer locateMBeanServer(@Nullable String agentId) throws MBeanServerNotFoundException {
    return JmxUtils.locateMBeanServer(agentId);
  }

  /**
   * Create a new {@code MBeanServer} instance and register it with the
   * {@code MBeanServerFactory}, if desired.
   *
   * @param defaultDomain the default domain, or {@code null} if none
   * @param registerWithFactory whether to register the {@code MBeanServer}
   * with the {@code MBeanServerFactory}
   * @see MBeanServerFactory#createMBeanServer
   * @see MBeanServerFactory#newMBeanServer
   */
  protected MBeanServer createMBeanServer(@Nullable String defaultDomain, boolean registerWithFactory) {
    if (registerWithFactory) {
      return MBeanServerFactory.createMBeanServer(defaultDomain);
    }
    else {
      return MBeanServerFactory.newMBeanServer(defaultDomain);
    }
  }

  @Override
  @Nullable
  public MBeanServer getObject() {
    return this.server;
  }

  @Override
  public Class<? extends MBeanServer> getObjectType() {
    return (this.server != null ? this.server.getClass() : MBeanServer.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * Unregisters the {@code MBeanServer} instance, if necessary.
   */
  @Override
  public void destroy() {
    if (this.newlyRegistered) {
      MBeanServerFactory.releaseMBeanServer(this.server);
    }
  }

}
