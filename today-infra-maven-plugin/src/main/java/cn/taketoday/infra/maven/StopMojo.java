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

package cn.taketoday.infra.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

/**
 * Stop an application that has been started by the "start" goal. Typically invoked once a
 * test suite has completed.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "stop", requiresProject = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMojo extends AbstractMojo {

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The JMX name of the automatically deployed MBean managing the lifecycle of the
   * application.
   */
  @Parameter(defaultValue = InfraApplicationAdminClient.DEFAULT_OBJECT_NAME)
  private String jmxName;

  /**
   * The port to use to look up the platform MBeanServer.
   */
  @Parameter(defaultValue = "9001")
  private int jmxPort;

  /**
   * Skip the execution.
   */
  @Parameter(property = "today-infra.stop.skip", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (this.skip) {
      getLog().debug("skipping stop as per configuration.");
      return;
    }
    getLog().info("Stopping application...");
    try (JMXConnector connector = InfraApplicationAdminClient.connect(this.jmxPort)) {
      MBeanServerConnection connection = connector.getMBeanServerConnection();
      stop(connection);
    }
    catch (IOException ex) {
      // The response won't be received as the server has died - ignoring
      getLog().debug("Service is not reachable anymore (" + ex.getMessage() + ")");
    }
  }

  private void stop(MBeanServerConnection connection) throws IOException, MojoExecutionException {
    try {
      new InfraApplicationAdminClient(connection, this.jmxName).stop();
    }
    catch (InstanceNotFoundException ex) {
      throw new MojoExecutionException(
              "Infra application lifecycle JMX bean not found. Could not stop application gracefully", ex);
    }
  }

}
