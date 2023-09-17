/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;
import java.util.Map;

import cn.taketoday.app.loader.tools.RunProcess;

/**
 * Run an application in place.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "run", requiresProject = true, defaultPhase = LifecyclePhase.VALIDATE,
      requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends AbstractRunMojo {

  /**
   * Whether the JVM's launch should be optimized.
   */
  @Parameter(property = "infra.run.optimizedLaunch", defaultValue = "true")
  private boolean optimizedLaunch;

  /**
   * Flag to include the test classpath when running.
   */
  @Parameter(property = "infra.run.useTestClasspath", defaultValue = "false")
  private Boolean useTestClasspath;

  @Override
  protected RunArguments resolveJvmArguments() {
    RunArguments jvmArguments = super.resolveJvmArguments();
    if (this.optimizedLaunch) {
      jvmArguments.getArgs().addFirst("-XX:TieredStopAtLevel=1");
    }
    return jvmArguments;
  }

  @Override
  protected void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
          Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException {
    processExecutor.withRunProcessCustomizer(runProcess ->
                    Runtime.getRuntime().addShutdownHook(new Thread(new RunProcessKiller(runProcess))))
            .run(workingDirectory, args, environmentVariables);
  }

  @Override
  protected boolean isUseTestClasspath() {
    return this.useTestClasspath;
  }

  private static final class RunProcessKiller implements Runnable {

    private final RunProcess runProcess;

    private RunProcessKiller(RunProcess runProcess) {
      this.runProcess = runProcess;
    }

    @Override
    public void run() {
      this.runProcess.kill();
    }

  }

}
