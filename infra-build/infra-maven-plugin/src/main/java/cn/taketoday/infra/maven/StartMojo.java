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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

/**
 * Start a infra application. Contrary to the {@code run} goal, this does not block and
 * allows other goals to operate on the application. This goal is typically used in
 * integration test scenario where the application is started before a test suite and
 * stopped after.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StopMojo
 * @since 4.0
 */
@Mojo(name = "start", requiresProject = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
      requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractRunMojo {

  private static final String ENABLE_MBEAN_PROPERTY = "--app.admin.enabled=true";

  private static final String JMX_NAME_PROPERTY_PREFIX = "--app.admin.jmx-name=";

  /**
   * The JMX name of the automatically deployed MBean managing the lifecycle of the
   * infra application.
   */
  @Parameter(defaultValue = InfraApplicationAdminClient.DEFAULT_OBJECT_NAME)
  private String jmxName;

  /**
   * The port to use to expose the platform MBeanServer.
   */
  @Parameter(defaultValue = "9001")
  private int jmxPort;

  /**
   * The number of milliseconds to wait between each attempt to check if the infra
   * application is ready.
   */
  @Parameter(property = "today-infra.start.wait", defaultValue = "500")
  private long wait;

  /**
   * The maximum number of attempts to check if the infra application is ready.
   * Combined with the "wait" argument, this gives a global timeout value (30 sec by
   * default)
   */
  @Parameter(property = "today-infra.start.maxAttempts", defaultValue = "60")
  private int maxAttempts;

  private final Object lock = new Object();

  @Override
  protected void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
          Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException {
    RunProcess runProcess = processExecutor.runAsync(workingDirectory, args, environmentVariables);
    try {
      waitForInfraApplication();
    }
    catch (MojoExecutionException | MojoFailureException ex) {
      runProcess.kill();
      throw ex;
    }
  }

  @Override
  protected RunArguments resolveApplicationArguments() {
    RunArguments applicationArguments = super.resolveApplicationArguments();
    applicationArguments.getArgs().addLast(ENABLE_MBEAN_PROPERTY);
    applicationArguments.getArgs().addLast(JMX_NAME_PROPERTY_PREFIX + this.jmxName);
    return applicationArguments;
  }

  @Override
  protected RunArguments resolveJvmArguments() {
    RunArguments jvmArguments = super.resolveJvmArguments();
    List<String> remoteJmxArguments = new ArrayList<>();
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote");
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.port=" + this.jmxPort);
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.authenticate=false");
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.ssl=false");
    remoteJmxArguments.add("-Djava.rmi.server.hostname=127.0.0.1");
    jvmArguments.getArgs().addAll(remoteJmxArguments);
    return jvmArguments;
  }

  private void waitForInfraApplication() throws MojoFailureException, MojoExecutionException {
    try {
      getLog().debug("Connecting to local MBeanServer at port " + this.jmxPort);
      try (JMXConnector connector = execute(this.wait, this.maxAttempts, new CreateJmxConnector(this.jmxPort))) {
        if (connector == null) {
          throw new MojoExecutionException("JMX MBean server was not reachable before the configured "
                  + "timeout (" + (this.wait * this.maxAttempts) + "ms");
        }
        getLog().debug("Connected to local MBeanServer at port " + this.jmxPort);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        doWaitForInfraApplication(connection);
      }
    }
    catch (IOException ex) {
      throw new MojoFailureException("Could not contact Infra application via JMX on port " + this.jmxPort
              + ". Please make sure that no other process is using that port", ex);
    }
    catch (Exception ex) {
      throw new MojoExecutionException("Failed to connect to MBean server at port " + this.jmxPort, ex);
    }
  }

  private void doWaitForInfraApplication(MBeanServerConnection connection)
          throws MojoExecutionException, MojoFailureException {
    final InfraApplicationAdminClient client = new InfraApplicationAdminClient(connection, this.jmxName);
    try {
      execute(this.wait, this.maxAttempts, () -> (client.isReady() ? true : null));
    }
    catch (ReflectionException ex) {
      throw new MojoExecutionException("Unable to retrieve 'ready' attribute", ex.getCause());
    }
    catch (Exception ex) {
      throw new MojoFailureException("Could not invoke shutdown operation", ex);
    }
  }

  /**
   * Execute a task, retrying it on failure.
   *
   * @param <T> the result type
   * @param wait the wait time
   * @param maxAttempts the maximum number of attempts
   * @param callback the task to execute (possibly multiple times). The callback should
   * return {@code null} to indicate that another attempt should be made
   * @return the result
   * @throws Exception in case of execution errors
   */
  public <T> T execute(long wait, int maxAttempts, Callable<T> callback) throws Exception {
    getLog().debug("Waiting for infra application to start...");
    for (int i = 0; i < maxAttempts; i++) {
      T result = callback.call();
      if (result != null) {
        return result;
      }
      String message = "Infra application is not ready yet, waiting " + wait + "ms (attempt " + (i + 1) + ")";
      getLog().debug(message);
      synchronized(this.lock) {
        try {
          this.lock.wait(wait);
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted while waiting for Infra app to start.");
        }
      }
    }
    throw new MojoExecutionException(
            "Infra application did not start before the configured timeout (" + (wait * maxAttempts) + "ms");
  }

  private class CreateJmxConnector implements Callable<JMXConnector> {

    private final int port;

    CreateJmxConnector(int port) {
      this.port = port;
    }

    @Override
    public JMXConnector call() throws Exception {
      try {
        return InfraApplicationAdminClient.connect(this.port);
      }
      catch (IOException ex) {
        if (hasCauseWithType(ex, ConnectException.class)) {
          String message = "MBean server at port " + this.port + " is not up yet...";
          getLog().debug(message);
          return null;
        }
        throw ex;
      }
    }

    private boolean hasCauseWithType(Throwable t, Class<? extends Exception> type) {
      return type.isAssignableFrom(t.getClass()) || t.getCause() != null && hasCauseWithType(t.getCause(), type);
    }

  }

}
