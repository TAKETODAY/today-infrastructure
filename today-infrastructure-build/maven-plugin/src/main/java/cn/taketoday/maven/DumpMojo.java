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
package cn.taketoday.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import static java.lang.String.format;

/**
 * <p>
 * Request a dump over TCP/IP from a JaCoCo agent running in
 * <code>tcpserver</code> mode.
 * </p>
 *
 * <p>
 * Note concerning parallel builds: While the dump goal as such is thread safe,
 * it has to be considered that TCP/IP server ports of the agents are a shared
 * resource.
 * </p>
 *
 * @since 0.6.4
 */
@Mojo(name = "dump", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class DumpMojo extends AbstractJacocoMojo {

  /**
   * Path to the output file for execution data.
   */
  @Parameter(property = "jacoco.destFile", defaultValue = "${project.build.directory}/jacoco.exec")
  private File destFile;

  /**
   * If set to true and the execution data file already exists, coverage data
   * is appended to the existing file. If set to false, an existing execution
   * data file will be replaced.
   */
  @Parameter(property = "jacoco.append", defaultValue = "true")
  private boolean append;

  /**
   * Sets whether execution data should be downloaded from the remote host.
   */
  @Parameter(property = "jacoco.dump", defaultValue = "true")
  private boolean dump;

  /**
   * Sets whether a reset command should be sent after the execution data has
   * been dumped.
   */
  @Parameter(property = "jacoco.reset", defaultValue = "false")
  private boolean reset;

  /**
   * IP address or hostname to connect to.
   */
  @Parameter(property = "jacoco.address")
  private String address;

  /**
   * Port number to connect to. If multiple JaCoCo agents should run on the
   * same machine, different ports have to be specified for the agents.
   */
  @Parameter(property = "jacoco.port", defaultValue = "6300")
  private int port;

  /**
   * Number of retries which the goal will attempt to establish a connection.
   * This can be used to wait until the target JVM is successfully launched.
   */
  @Parameter(property = "jacoco.retryCount", defaultValue = "10")
  private int retryCount;

  @Override
  public void executeMojo() throws MojoExecutionException {
    final ExecDumpClient client = new ExecDumpClient() {
      @Override
      protected void onConnecting(final InetAddress address,
              final int port) {
        getLog().info(format("Connecting to %s:%s", address,
                Integer.valueOf(port)));
      }

      @Override
      protected void onConnectionFailure(final IOException exception) {
        getLog().info(exception.getMessage());
      }
    };
    client.setDump(dump);
    client.setReset(reset);
    client.setRetryCount(retryCount);

    try {
      final ExecFileLoader loader = client.dump(address, port);
      if (dump) {
        getLog().info(format("Dumping execution data to %s",
                destFile.getAbsolutePath()));
        loader.save(destFile, append);
      }
    }
    catch (final IOException e) {
      throw new MojoExecutionException("Unable to dump coverage data", e);
    }
  }

}
