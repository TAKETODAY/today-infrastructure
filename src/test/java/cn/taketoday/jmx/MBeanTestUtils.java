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

package cn.taketoday.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * Utilities for MBean tests.
 *
 * @author Phillip Webb
 */
public class MBeanTestUtils {

  /**
   * Resets MBeanServerFactory and ManagementFactory to a known consistent state.
   * <p>This involves releasing all currently registered MBeanServers and resetting
   * the platformMBeanServer to null.
   */
  public static synchronized void resetMBeanServers() throws Exception {
    for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
      try {
        MBeanServerFactory.releaseMBeanServer(server);
      }
      catch (IllegalArgumentException ex) {
        if (!ex.getMessage().contains("not in list")) {
          throw ex;
        }
      }
    }
  }

}
