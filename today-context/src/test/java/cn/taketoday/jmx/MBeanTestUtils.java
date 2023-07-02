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
   * Reset the {@link MBeanServerFactory} to a known consistent state. This involves
   * {@linkplain #releaseMBeanServer(MBeanServer) releasing} all currently registered
   * MBeanServers.
   */
  public static synchronized void resetMBeanServers() throws Exception {
    for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
      releaseMBeanServer(server);
    }
  }

  /**
   * Attempt to release the supplied {@link MBeanServer}.
   * <p>Ignores any {@link IllegalArgumentException} thrown by
   * {@link MBeanServerFactory#releaseMBeanServer(MBeanServer)} whose error
   * message contains the text "not in list".
   */
  public static synchronized void releaseMBeanServer(MBeanServer server) {
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
