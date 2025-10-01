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

package infra.http.client.support;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.lang.Assert;

/**
 * {@link FactoryBean} that creates a {@link Proxy java.net.Proxy}.
 *
 * @author Arjen Poutsma
 * @see FactoryBean
 * @see Proxy
 * @since 4.0
 */
public class NetworkProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

  private Proxy.Type type = Proxy.Type.HTTP;

  @Nullable
  private String hostname;

  private int port = -1;

  @Nullable
  private Proxy proxy;

  /**
   * Set the proxy type.
   * <p>Defaults to {@link Proxy.Type#HTTP}.
   */
  public void setType(Proxy.Type type) {
    this.type = type;
  }

  /**
   * Set the proxy host name.
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Set the proxy port.
   */
  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public void afterPropertiesSet() throws IllegalArgumentException {
    Assert.notNull(this.type, "Property 'type' is required");
    Assert.notNull(this.hostname, "Property 'hostname' is required");
    if (this.port < 0 || this.port > 65535) {
      throw new IllegalArgumentException("Property 'port' value out of range: " + this.port);
    }

    SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
    this.proxy = new Proxy(this.type, socketAddress);
  }

  @Override
  @Nullable
  public Proxy getObject() {
    return this.proxy;
  }

  @Override
  public Class<?> getObjectType() {
    return Proxy.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
