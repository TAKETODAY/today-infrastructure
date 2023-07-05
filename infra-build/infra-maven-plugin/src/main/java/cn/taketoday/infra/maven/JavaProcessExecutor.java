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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ease the execution of a Java process using Maven's toolchain support.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JavaProcessExecutor {

  private static final int EXIT_CODE_SIGINT = 130;

  private final MavenSession mavenSession;

  private final ToolchainManager toolchainManager;

  private final Consumer<RunProcess> runProcessCustomizer;

  JavaProcessExecutor(MavenSession mavenSession, ToolchainManager toolchainManager) {
    this(mavenSession, toolchainManager, null);
  }

  private JavaProcessExecutor(MavenSession mavenSession, ToolchainManager toolchainManager,
          Consumer<RunProcess> runProcessCustomizer) {
    this.mavenSession = mavenSession;
    this.toolchainManager = toolchainManager;
    this.runProcessCustomizer = runProcessCustomizer;
  }

  JavaProcessExecutor withRunProcessCustomizer(Consumer<RunProcess> customizer) {
    Consumer<RunProcess> combinedCustomizer = (this.runProcessCustomizer != null)
                                              ? this.runProcessCustomizer.andThen(customizer) : customizer;
    return new JavaProcessExecutor(this.mavenSession, this.toolchainManager, combinedCustomizer);
  }

  int run(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
          throws MojoExecutionException {
    RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
    if (this.runProcessCustomizer != null) {
      this.runProcessCustomizer.accept(runProcess);
    }
    try {
      int exitCode = runProcess.run(true, args, environmentVariables);
      if (!hasTerminatedSuccessfully(exitCode)) {
        throw new MojoExecutionException("Process terminated with exit code: " + exitCode);
      }
      return exitCode;
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Process execution failed", ex);
    }
  }

  RunProcess runAsync(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
          throws MojoExecutionException {
    try {
      RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
      runProcess.run(false, args, environmentVariables);
      return runProcess;
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Process execution failed", ex);
    }
  }

  private boolean hasTerminatedSuccessfully(int exitCode) {
    return (exitCode == 0 || exitCode == EXIT_CODE_SIGINT);
  }

  private String getJavaExecutable() {
    Toolchain toolchain = this.toolchainManager.getToolchainFromBuildContext("jdk", this.mavenSession);
    String javaExecutable = (toolchain != null) ? toolchain.findTool("java") : null;
    return (javaExecutable != null) ? javaExecutable : new JavaExecutable().toString();
  }

}
