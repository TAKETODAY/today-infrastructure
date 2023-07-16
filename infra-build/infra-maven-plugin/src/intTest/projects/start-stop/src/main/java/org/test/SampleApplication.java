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

package org.test;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This sample app simulates the JMX Mbean that is exposed by the Infra application.
 */
public class SampleApplication {

  private static final Object lock = new Object();

  public static void main(String[] args) throws Exception {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName(
            "cn.taketoday.app:type=Admin,name=InfraApplication");
    SpringApplicationAdmin mbean = new SpringApplicationAdmin();
    mbs.registerMBean(mbean, name);

    // Flag the app as ready
    mbean.ready = true;

    int waitAttempts = 0;
    while (!mbean.shutdownInvoked) {
      if (waitAttempts > 30) {
        throw new IllegalStateException(
                "Shutdown should have been invoked by now");
      }
      synchronized(lock) {
        lock.wait(250);
      }
      waitAttempts++;
    }
  }

  public interface SpringApplicationAdminMXBean {

    boolean isReady();

    void shutdown();

  }

  static class SpringApplicationAdmin implements SpringApplicationAdminMXBean {

    private boolean ready;

    private boolean shutdownInvoked;

    @Override
    public boolean isReady() {
      System.out.println("isReady: " + this.ready);
      return this.ready;
    }

    @Override
    public void shutdown() {
      this.shutdownInvoked = true;
      System.out.println("Shutdown requested");
    }

  }

}
