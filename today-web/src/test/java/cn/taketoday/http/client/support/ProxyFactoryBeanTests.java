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

package cn.taketoday.http.client.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
public class ProxyFactoryBeanTests {

  NetworkProxyFactoryBean factoryBean;

  @BeforeEach
  public void setUp() {
    factoryBean = new NetworkProxyFactoryBean();
  }

  @Test
  public void noType() {
    factoryBean.setType(null);
    assertThatIllegalArgumentException().isThrownBy(
            factoryBean::afterPropertiesSet);
  }

  @Test
  public void noHostname() {
    factoryBean.setHostname("");
    assertThatIllegalArgumentException().isThrownBy(
            factoryBean::afterPropertiesSet);
  }

  @Test
  public void noPort() {
    factoryBean.setHostname("example.com");
    assertThatIllegalArgumentException().isThrownBy(
            factoryBean::afterPropertiesSet);
  }

  @Test
  public void normal() {
    Proxy.Type type = Proxy.Type.HTTP;
    factoryBean.setType(type);
    String hostname = "example.com";
    factoryBean.setHostname(hostname);
    int port = 8080;
    factoryBean.setPort(port);
    factoryBean.afterPropertiesSet();

    Proxy result = factoryBean.getObject();

    assertThat(result.type()).isEqualTo(type);
    InetSocketAddress address = (InetSocketAddress) result.address();
    assertThat(address.getHostName()).isEqualTo(hostname);
    assertThat(address.getPort()).isEqualTo(port);
  }

}
