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

package cn.taketoday.context.testfixture;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import cn.taketoday.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 23:04
 */
public class TestSocketUtils {

  /**
   * The default minimum value for port ranges used when finding an available
   * socket port.
   */
  private static final int PORT_RANGE_MIN = 1024;

  /**
   * The default maximum value for port ranges used when finding an available
   * socket port.
   */
  private static final int PORT_RANGE_MAX = 65535;

  private static final Random random = new Random(System.nanoTime());

  /**
   * Find an available TCP port randomly selected from the range
   * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
   *
   * @return an available TCP port number
   * @throws IllegalStateException if no available port could be found
   */
  public static int findAvailableTcpPort() {
    return findAvailablePort(PORT_RANGE_MIN, PORT_RANGE_MAX);
  }

  /**
   * Find an available port for this {@code SocketType}, randomly selected
   * from the range [{@code minPort}, {@code maxPort}].
   *
   * @param minPort the minimum port number
   * @param maxPort the maximum port number
   * @return an available port number for this socket type
   * @throws IllegalStateException if no available port could be found
   */
  private static int findAvailablePort(int minPort, int maxPort) {
    Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
    Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
    Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

    int portRange = maxPort - minPort;
    int candidatePort;
    int searchCounter = 0;
    do {
      if (searchCounter > portRange) {
        throw new IllegalStateException(String.format(
                "Could not find an available TCP port in the range [%d, %d] after %d attempts",
                minPort, maxPort, searchCounter));
      }
      candidatePort = findRandomPort(minPort, maxPort);
      searchCounter++;
    }
    while (!isPortAvailable(candidatePort));

    return candidatePort;
  }

  /**
   * Find a pseudo-random port number within the range
   * [{@code minPort}, {@code maxPort}].
   *
   * @param minPort the minimum port number
   * @param maxPort the maximum port number
   * @return a random port number within the specified range
   */
  private static int findRandomPort(int minPort, int maxPort) {
    int portRange = maxPort - minPort;
    return minPort + random.nextInt(portRange + 1);
  }

  /**
   * Determine if the specified port for this {@code SocketType} is
   * currently available on {@code localhost}.
   */
  private static boolean isPortAvailable(int port) {
    try {
      ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
              port, 1, InetAddress.getByName("localhost"));
      serverSocket.close();
      return true;
    }
    catch (Exception ex) {
      return false;
    }
  }

}
