/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.War;

import java.util.concurrent.Callable;

import infra.gradle.tasks.bundling.InfraWar;

/**
 * {@link Action} that is executed in response to the {@link WarPlugin} being applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class WarPluginAction implements PluginApplicationAction {

  private final SinglePublishedArtifact singlePublishedArtifact;

  WarPluginAction(SinglePublishedArtifact singlePublishedArtifact) {
    this.singlePublishedArtifact = singlePublishedArtifact;
  }

  @Override
  public Class<? extends Plugin<? extends Project>> getPluginClass() {
    return WarPlugin.class;
  }

  @Override
  public boolean autoApply(Project project) {
    return false;
  }

  @Override
  public void execute(Project project) {
    classifyWarTask(project);
    TaskProvider<InfraWar> infraWar = configureInfraWarTask(project);
    configureArtifactPublication(infraWar);
  }

  private void classifyWarTask(Project project) {
    project.getTasks()
            .named(WarPlugin.WAR_TASK_NAME, War.class)
            .configure((war) -> war.getArchiveClassifier().convention("plain"));
  }

  private TaskProvider<InfraWar> configureInfraWarTask(Project project) {
    ConfigurationContainer container = project.getConfigurations();
    Configuration developmentOnly = container.getByName(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration testAndDevelopmentOnly = container.getByName(InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration productionRuntimeClasspath = container.getByName(InfraApplicationPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    Configuration runtimeClasspath = container.getByName(mainSourceSet.getRuntimeClasspathConfigurationName());

    Callable<FileCollection> classpath = () -> mainSourceSet.getRuntimeClasspath()
            .minus(providedRuntimeConfiguration(project))
            .minus(developmentOnly.minus(productionRuntimeClasspath))
            .minus(testAndDevelopmentOnly.minus(productionRuntimeClasspath))
            .filter(JarTypeFileSpec.include());
    TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks()
            .named(InfraApplicationPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
    TaskProvider<InfraWar> infraWarProvider = project.getTasks()
            .register(InfraApplicationPlugin.INFRA_WAR_TASK_NAME, InfraWar.class, infraWar -> {
              infraWar.setGroup(BasePlugin.BUILD_GROUP);
              infraWar.setDescription("Assembles an executable war archive containing webapp"
                      + " content, and the main classes and their dependencies.");
              infraWar.providedClasspath(providedRuntimeConfiguration(project));
              infraWar.setClasspath(classpath);
              Provider<String> manifestStartClass = project
                      .provider(() -> (String) infraWar.getManifest().getAttributes().get("Start-Class"));
              infraWar.getMainClass()
                      .convention(resolveMainClassName.flatMap((resolver) -> manifestStartClass.isPresent()
                              ? manifestStartClass : resolveMainClassName.get().readMainClassName()));
              infraWar.getTargetJavaVersion()
                      .set(project.provider(() -> javaPluginExtension(project).getTargetCompatibility()));
              infraWar.resolvedArtifacts(runtimeClasspath.getIncoming().getArtifacts().getResolvedArtifacts());
            });
    infraWarProvider.map(War::getClasspath);
    return infraWarProvider;
  }

  private FileCollection providedRuntimeConfiguration(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();
    return configurations.getByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME);
  }

  private void configureArtifactPublication(TaskProvider<InfraWar> infraWar) {
    this.singlePublishedArtifact.addWarCandidate(infraWar);
  }

  private JavaPluginExtension javaPluginExtension(Project project) {
    return project.getExtensions().getByType(JavaPluginExtension.class);
  }

}
