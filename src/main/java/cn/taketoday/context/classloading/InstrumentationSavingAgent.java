/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.classloading;

import java.lang.instrument.Instrumentation;

import cn.taketoday.lang.Nullable;

/**
 * Java agent that saves the {@link Instrumentation} interface from the JVM
 * for later use.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public final class InstrumentationSavingAgent {

  @Nullable
  private static volatile Instrumentation instrumentation;

  private InstrumentationSavingAgent() { }

  /**
   * Save the {@link Instrumentation} interface exposed by the JVM.
   */
  public static void premain(String agentArgs, Instrumentation inst) {
    instrumentation = inst;
  }

  /**
   * Save the {@link Instrumentation} interface exposed by the JVM.
   * This method is required to dynamically load this Agent with the Attach API.
   */
  public static void agentmain(String agentArgs, Instrumentation inst) {
    instrumentation = inst;
  }

  /**
   * Return the {@link Instrumentation} interface exposed by the JVM.
   * <p>Note that this agent class will typically not be available in the classpath
   * unless the agent is actually specified on JVM startup. If you intend to do
   * conditional checking with respect to agent availability, consider using
   * {@link InstrumentationLoadTimeWeaver#getInstrumentation()}
   * instead - which will work without the agent class in the classpath as well.
   *
   * @return the {@code Instrumentation} instance previously saved when
   * the {@link #premain} or {@link #agentmain} methods was called by the JVM;
   * will be {@code null} if this class was not used as Java agent when this
   * JVM was started or it wasn't installed as agent using the Attach API.
   * @see InstrumentationLoadTimeWeaver#getInstrumentation()
   */
  @Nullable
  public static Instrumentation getInstrumentation() {
    return instrumentation;
  }

}
