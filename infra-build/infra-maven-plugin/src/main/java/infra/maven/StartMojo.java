/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import infra.app.loader.tools.RunProcess;

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
  @Parameter(property = "infra.start.wait", defaultValue = "500")
  private long wait;

  /**
   * The maximum number of attempts to check if the infra application is ready.
   * Combined with the "wait" argument, this gives a global timeout value (30 sec by
   * default)
   */
  @Parameter(property = "infra.start.maxAttempts", defaultValue = "60")
  private int maxAttempts;

  @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
  private File buildDirectory;

  private final Object lock = new Object();

  private final String password = UUID.randomUUID().toString();

  /**
   * Flag to include the test classpath when running.
   */
  @Parameter(property = "infra.run.useTestClasspath", defaultValue = "false")
  private Boolean useTestClasspath;

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
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.rmi.port=" + this.jmxPort);
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.local.only=true");
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.host=127.0.0.1");
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.authenticate=true");
    Path directory = this.buildDirectory.toPath().toAbsolutePath();
    Path passwordFile = directory.resolve(InfraApplicationAdminClient.JMX_PASSWORD_FILE);
    writeWithOwnerOnlyPermissions(passwordFile, InfraApplicationAdminClient.JMX_USER + " " + this.password);
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.password.file=" + passwordFile);
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.password.toHashes=false");
    Path accessFile = directory.resolve("infra.jmxremote.access");
    writeWithOwnerOnlyPermissions(accessFile, InfraApplicationAdminClient.JMX_USER + " readwrite");
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.access.file=" + accessFile);
    remoteJmxArguments.add("-Dcom.sun.management.jmxremote.ssl=false");
    remoteJmxArguments.add("-Djava.rmi.server.hostname=127.0.0.1");
    jvmArguments.getArgs().addAll(remoteJmxArguments);
    return jvmArguments;
  }

  private void writeWithOwnerOnlyPermissions(Path file, String content) {
    try {
      Path directory = Files.createDirectories(file.getParent());
      Files.deleteIfExists(file);
      if (Files.getFileAttributeView(directory, PosixFileAttributeView.class) != null) {
        Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
      }
      else {
        Files.createFile(file);
        AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (view == null) {
          throw new IOException("Unable to set owner-only permissions for " + file);
        }
        AclEntry ownerOnly = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(view.getOwner())
                .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
        view.setAcl(List.of(ownerOnly));
      }
      Files.write(file, List.of(content), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
    }
    catch (IOException ex) {
      throw new UncheckedIOException("Unable to write JMX configuration file " + file, ex);
    }
  }

  private void waitForInfraApplication() throws MojoFailureException, MojoExecutionException {
    try {
      getLog().debug("Connecting to local MBeanServer at port " + this.jmxPort);
      try (JMXConnector connector = execute(this.wait, this.maxAttempts,
              new CreateJmxConnector(this.jmxPort, this.password))) {
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

  @Override
  protected boolean isUseTestClasspath() {
    return this.useTestClasspath;
  }

  private class CreateJmxConnector implements Callable<JMXConnector> {

    private final int port;

    private final String password;

    CreateJmxConnector(int port, String password) {
      this.port = port;
      this.password = password;
    }

    @Override
    public JMXConnector call() throws Exception {
      try {
        return InfraApplicationAdminClient.connect(this.port, this.password);
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
      return type.isInstance(t) || t.getCause() != null && hasCauseWithType(t.getCause(), type);
    }

  }

}
