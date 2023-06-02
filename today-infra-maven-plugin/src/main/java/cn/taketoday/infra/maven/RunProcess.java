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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import sun.misc.Signal;

/**
 * Utility used to run a process.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/10 20:54
 */
public class RunProcess {
  private static final Signal SIG_INT = new Signal("INT");

  private static final long JUST_ENDED_LIMIT = 500;

  private final File workingDirectory;

  private final String[] command;

  private volatile Process process;

  private volatile long endTime;

  /**
   * Creates new {@link RunProcess} instance for the specified command.
   *
   * @param command the program to execute and its arguments
   */
  public RunProcess(String... command) {
    this(null, command);
  }

  /**
   * Creates new {@link RunProcess} instance for the specified working directory and
   * command.
   *
   * @param workingDirectory the working directory of the child process or {@code null}
   * to run in the working directory of the current Java process
   * @param command the program to execute and its arguments
   */
  public RunProcess(File workingDirectory, String... command) {
    this.workingDirectory = workingDirectory;
    this.command = command;
  }

  public int run(boolean waitForProcess, String... args) throws IOException {
    return run(waitForProcess, Arrays.asList(args), Collections.emptyMap());
  }

  public int run(boolean waitForProcess, Collection<String> args, Map<String, String> environmentVariables)
          throws IOException {
    ProcessBuilder builder = new ProcessBuilder(this.command);
    builder.directory(this.workingDirectory);
    builder.command().addAll(args);
    builder.environment().putAll(environmentVariables);
    builder.redirectErrorStream(true);
    builder.inheritIO();
    try {
      Process process = builder.start();
      this.process = process;
      attachSignalHandler(this::handleSigInt);
      if (waitForProcess) {
        try {
          return process.waitFor();
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return 1;
        }
      }
      return 5;
    }
    finally {
      if (waitForProcess) {
        this.endTime = System.currentTimeMillis();
        this.process = null;
      }
    }
  }

  /**
   * Return the running process.
   *
   * @return the process or {@code null}
   */
  public Process getRunningProcess() {
    return this.process;
  }

  /**
   * Return if the process was stopped.
   *
   * @return {@code true} if stopped
   */
  public boolean handleSigInt() {
    if (allowChildToHandleSigInt()) {
      return true;
    }
    return doKill();
  }

  private boolean allowChildToHandleSigInt() {
    Process process = this.process;
    if (process == null) {
      return true;
    }
    long end = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < end) {
      if (!process.isAlive()) {
        return true;
      }
      try {
        Thread.sleep(500);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  /**
   * Kill this process.
   */
  public void kill() {
    doKill();
  }

  private boolean doKill() {
    // destroy the running process
    Process process = this.process;
    if (process != null) {
      try {
        process.destroy();
        process.waitFor();
        this.process = null;
        return true;
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    return false;
  }

  public boolean hasJustEnded() {
    return System.currentTimeMillis() < (this.endTime + JUST_ENDED_LIMIT);
  }

  /**
   * Handle {@literal INT} signals by calling the specified {@link Runnable}.
   *
   * @param runnable the runnable to call on SIGINT.
   */
  public static void attachSignalHandler(Runnable runnable) {
    Signal.handle(SIG_INT, (signal) -> runnable.run());
  }

}
