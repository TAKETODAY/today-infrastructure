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

package cn.taketoday.build.hint;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.testing.Test;

import java.util.Collections;

/**
 * {@link Plugin} that configures the {@code RuntimeHints} Java agent to test tasks.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RuntimeHintsAgentPlugin implements Plugin<Project> {

  public static final String RUNTIMEHINTS_TEST_TASK = "runtimeHintsTest";
  private static final String EXTENSION_NAME = "runtimeHintsAgent";
  private static final String CONFIGURATION_NAME = "testRuntimeHintsAgentJar";

  @Override
  public void apply(Project project) {

    project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
      RuntimeHintsAgentExtension agentExtension = createRuntimeHintsAgentExtension(project);
      Test agentTest = project.getTasks().create(RUNTIMEHINTS_TEST_TASK, Test.class, test -> {
        test.useJUnitPlatform(options -> {
          options.includeTags("RuntimeHintsTests");
        });
        test.include("**/*Tests.class", "**/*Test.class");
        test.systemProperty("java.awt.headless", "true");
        test.systemProperty("org.graalvm.nativeimage.imagecode", "runtime");
        test.getJvmArgumentProviders().add(createRuntimeHintsAgentArgumentProvider(project, agentExtension));
      });
      project.getTasks().getByName("check", task -> task.dependsOn(agentTest));
      project.getDependencies().add(CONFIGURATION_NAME, project.project(":today-core-test"));
    });
  }

  private static RuntimeHintsAgentExtension createRuntimeHintsAgentExtension(Project project) {
    RuntimeHintsAgentExtension agentExtension = project.getExtensions().create(EXTENSION_NAME, RuntimeHintsAgentExtension.class);
    agentExtension.getIncludedPackages().convention(Collections.singleton("cn.taketoday"));
    agentExtension.getExcludedPackages().convention(Collections.emptySet());
    return agentExtension;
  }

  private static RuntimeHintsAgentArgumentProvider createRuntimeHintsAgentArgumentProvider(
          Project project, RuntimeHintsAgentExtension agentExtension) {
    RuntimeHintsAgentArgumentProvider agentArgumentProvider = project.getObjects().newInstance(RuntimeHintsAgentArgumentProvider.class);
    agentArgumentProvider.getAgentJar().from(createRuntimeHintsAgentConfiguration(project));
    agentArgumentProvider.getIncludedPackages().set(agentExtension.getIncludedPackages());
    agentArgumentProvider.getExcludedPackages().set(agentExtension.getExcludedPackages());
    return agentArgumentProvider;
  }

  private static Configuration createRuntimeHintsAgentConfiguration(Project project) {
    return project.getConfigurations().create(CONFIGURATION_NAME, configuration -> {
      configuration.setCanBeConsumed(false);
      configuration.setTransitive(false); // Only the built artifact is required
      configuration.attributes(attributes -> {
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, Integer.valueOf(JavaVersion.current().getMajorVersion()));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
      });
    });
  }
}
