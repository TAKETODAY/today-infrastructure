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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import cn.taketoday.jmx.MBeanTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MBeanServerFactoryBean}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 */
class MBeanServerFactoryBeanTests {

  @BeforeEach
  @AfterEach
  void resetMBeanServers() throws Exception {
    MBeanTestUtils.resetMBeanServers();
  }

  @Test
  void defaultValues() {
    MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
    bean.afterPropertiesSet();
    try {
      MBeanServer server = bean.getObject();
      assertThat(server).as("The MBeanServer should not be null").isNotNull();
    }
    finally {
      bean.destroy();
    }
  }

  @Test
  void defaultDomain() {
    MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
    bean.setDefaultDomain("foo");
    bean.afterPropertiesSet();
    try {
      MBeanServer server = bean.getObject();
      assertThat(server.getDefaultDomain()).as("The default domain should be foo").isEqualTo("foo");
    }
    finally {
      bean.destroy();
    }
  }

  @Test
  void locateExistingServerIfPossibleWithExistingServer() {
    MBeanServer server = MBeanServerFactory.createMBeanServer();
    try {
      MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
      bean.setLocateExistingServerIfPossible(true);
      bean.afterPropertiesSet();
      try {
        MBeanServer otherServer = bean.getObject();
        assertThat(otherServer).as("Existing MBeanServer not located").isSameAs(server);
      }
      finally {
        bean.destroy();
      }
    }
    finally {
      MBeanServerFactory.releaseMBeanServer(server);
    }
  }

  @Test
  void locateExistingServerIfPossibleWithFallbackToPlatformServer() {
    MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
    bean.setLocateExistingServerIfPossible(true);
    bean.afterPropertiesSet();
    try {
      assertThat(bean.getObject()).isSameAs(ManagementFactory.getPlatformMBeanServer());
    }
    finally {
      bean.destroy();
    }
  }

  @Test
  void withEmptyAgentIdAndFallbackToPlatformServer() {
    MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
    bean.setAgentId("");
    bean.afterPropertiesSet();
    try {
      assertThat(bean.getObject()).isSameAs(ManagementFactory.getPlatformMBeanServer());
    }
    finally {
      bean.destroy();
    }
  }

  @Test
  void createMBeanServer() throws Exception {
    assertCreation(true, "The server should be available in the list");
  }

  @Test
  void newMBeanServer() throws Exception {
    assertCreation(false, "The server should not be available in the list");
  }

  private void assertCreation(boolean referenceShouldExist, String failMsg) {
    MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
    bean.setRegisterWithFactory(referenceShouldExist);
    bean.afterPropertiesSet();
    try {
      MBeanServer server = bean.getObject();
      List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
      assertThat(hasInstance(servers, server)).as(failMsg).isEqualTo(referenceShouldExist);
    }
    finally {
      bean.destroy();
    }
  }

  private boolean hasInstance(List<MBeanServer> servers, MBeanServer server) {
    return servers.stream().anyMatch(current -> current == server);
  }

}
