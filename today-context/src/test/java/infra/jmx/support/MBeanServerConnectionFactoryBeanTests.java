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

package infra.jmx.support;

import org.junit.jupiter.api.Test;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import infra.aop.support.AopUtils;
import infra.core.testfixture.net.TestSocketUtils;
import infra.jmx.AbstractMBeanServerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MBeanServerConnectionFactoryBean}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class MBeanServerConnectionFactoryBeanTests extends AbstractMBeanServerTests {

  private final String serviceUrl = "service:jmx:jmxmp://localhost:" + TestSocketUtils.findAvailableTcpPort();

  @Test
  void noServiceUrl() throws Exception {
    MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
    assertThatIllegalArgumentException()
            .isThrownBy(bean::afterPropertiesSet)
            .withMessage("Property 'serviceUrl' is required");
  }

  @Test
  void validConnection() throws Exception {
    JMXConnectorServer connectorServer = startConnectorServer();

    try {
      MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
      bean.setServiceUrl(this.serviceUrl);
      bean.afterPropertiesSet();

      try {
        MBeanServerConnection connection = bean.getObject();
        assertThat(connection).as("Connection should not be null").isNotNull();

        // perform simple MBean count test
        assertThat(connection.getMBeanCount()).as("MBean count should be the same").isEqualTo(getServer().getMBeanCount());
      }
      finally {
        bean.destroy();
      }
    }
    finally {
      connectorServer.stop();
    }
  }

  @Test
  void lazyConnection() throws Exception {
    MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
    bean.setServiceUrl(this.serviceUrl);
    bean.setConnectOnStartup(false);
    bean.afterPropertiesSet();

    MBeanServerConnection connection = bean.getObject();
    assertThat(AopUtils.isAopProxy(connection)).isTrue();

    JMXConnectorServer connector = null;
    try {
      connector = startConnectorServer();
      assertThat(connection.getMBeanCount()).as("Incorrect MBean count").isEqualTo(getServer().getMBeanCount());
    }
    finally {
      bean.destroy();
      if (connector != null) {
        connector.stop();
      }
    }
  }

  @Test
  void lazyConnectionAndNoAccess() throws Exception {
    MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
    bean.setServiceUrl(this.serviceUrl);
    bean.setConnectOnStartup(false);
    bean.afterPropertiesSet();

    MBeanServerConnection connection = bean.getObject();
    assertThat(AopUtils.isAopProxy(connection)).isTrue();
    bean.destroy();
  }

  private JMXConnectorServer startConnectorServer() throws Exception {
    JMXServiceURL jmxServiceUrl = new JMXServiceURL(this.serviceUrl);
    JMXConnectorServer connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceUrl, null, getServer());
    connectorServer.start();
    return connectorServer;
  }

}
