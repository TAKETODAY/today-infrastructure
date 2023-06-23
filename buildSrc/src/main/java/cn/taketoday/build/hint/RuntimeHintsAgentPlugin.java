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

package cn.taketoday.build.hint;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

/**
 * {@link Plugin} that configures the {@code RuntimeHints} Java agent to test tasks.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
public class RuntimeHintsAgentPlugin implements Plugin<Project> {

  public static final String RUNTIMEHINTS_TEST_TASK = "runtimeHintsTest";
  private static final String EXTENSION_NAME = "runtimeHintsAgent";

  @Override
  public void apply(Project project) {

    project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
      RuntimeHintsAgentExtension agentExtension = project.getExtensions().create(EXTENSION_NAME,
              RuntimeHintsAgentExtension.class, project.getObjects());
      Test agentTest = project.getTasks().create(RUNTIMEHINTS_TEST_TASK, Test.class, test -> {
        test.useJUnitPlatform(options -> {
          options.includeTags("RuntimeHintsTests");
        });
        test.include("**/*Tests.class", "**/*Test.class");
        test.systemProperty("java.awt.headless", "true");
        test.systemProperty("org.graalvm.nativeimage.imagecode", "runtime");
      });
      project.afterEvaluate(p -> {
        Jar jar = project.getRootProject().project("today-core-test").getTasks().withType(Jar.class).named("jar").get();
        agentTest.jvmArgs("-javaagent:" + jar.getArchiveFile().get().getAsFile() + "=" + agentExtension.asJavaAgentArgument());
      });
      project.getTasks().getByName("check", task -> task.dependsOn(agentTest));
    });
  }
}
