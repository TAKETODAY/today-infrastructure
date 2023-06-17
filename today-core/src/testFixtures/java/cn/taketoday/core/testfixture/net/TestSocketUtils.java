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

package cn.taketoday.core.testfixture.net;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

/**
 * Simple utility for finding available TCP ports on {@code localhost} for use in
 * integration testing scenarios.
 *
 * <p>{@code SocketUtils} was removed from the public API in {@code spring-core}
 * in Spring Framework 6.0 and reintroduced as {@code TestSocketUtils}, which is
 * made available to all tests in Spring Framework's test suite as a Gradle
 * <em>test fixture</em>.
 *
 * <p>{@code SocketUtils} was introduced in Spring Framework 4.0, primarily to
 * assist in writing integration tests which start an external server on an
 * available random port. However, these utilities make no guarantee about the
 * subsequent availability of a given port and are therefore unreliable. Instead
 * of using {@code TestSocketUtils} to find an available local port for a server,
 * it is recommended that you rely on a server's ability to start on a random port
 * that it selects or is assigned by the operating system. To interact with that
 * server, you should query the server for the port it is currently using.
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 6.0
 */
public abstract class TestSocketUtils {

  /**
   * The minimum value for port ranges used when finding an available TCP port.
   */
  private static final int PORT_RANGE_MIN = 1024;

  /**
   * The maximum value for port ranges used when finding an available TCP port.
   */
  private static final int PORT_RANGE_MAX = 65535;

  private static final int PORT_RANGE = PORT_RANGE_MAX - PORT_RANGE_MIN;

  private static final int MAX_ATTEMPTS = 1_000;

  private static final Random random = new Random(System.nanoTime());

  /**
   * Find an available TCP port randomly selected from the range [1024, 65535].
   *
   * @return an available TCP port number
   * @throws IllegalStateException if no available port could be found
   */
  public static int findAvailableTcpPort() {
    int candidatePort;
    int searchCounter = 0;
    do {
      if (searchCounter > MAX_ATTEMPTS) {
        throw new IllegalStateException(String.format(
                "Could not find an available TCP port in the range [%d, %d] after %d attempts",
                PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
      }
      candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE + 1);
      searchCounter++;
    }
    while (!isPortAvailable(candidatePort));

    return candidatePort;
  }

  /**
   * Determine if the specified TCP port is currently available on {@code localhost}.
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
