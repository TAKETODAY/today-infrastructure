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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import infra.gradle.dsl.InfraApplicationExtension;
import infra.gradle.tasks.bundling.InfraJar;
import infra.gradle.tasks.bundling.InfraWar;
import infra.lang.Version;
import infra.util.ObjectUtils;
import infra.util.PropertyPlaceholderHandler;

/**
 * Gradle plugin for Infra.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Danny Hyun
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraApplicationPlugin implements Plugin<Project> {

  private static final String INFRA_VERSION = Version.instance.implementationVersion();

  /**
   * The name of the {@link Configuration} that contains Infra archives.
   */
  public static final String INFRA_ARCHIVES_CONFIGURATION_NAME = "infraArchives";

  /**
   * The name of the default {@link InfraJar} task.
   */
  public static final String INFRA_JAR_TASK_NAME = "infraJar";

  /**
   * The name of the default {@link InfraWar} task.
   */
  public static final String INFRA_WAR_TASK_NAME = "infraWar";

  static final String INFRA_RUN_TASK_NAME = "infraRun";

  static final String INFRA_TEST_RUN_TASK_NAME = "infraTestRun";

  /**
   * The name of the {@code developmentOnly} configuration.
   */
  public static final String DEVELOPMENT_ONLY_CONFIGURATION_NAME = "developmentOnly";

  /**
   * The name of the {@code testAndDevelopmentOnly} configuration.
   */
  public static final String TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME = "testAndDevelopmentOnly";

  /**
   * The name of the {@code productionRuntimeClasspath} configuration.
   */
  public static final String PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME = "productionRuntimeClasspath";

  /**
   * The name of the {@link ResolveMainClassName} task used to resolve a main class from
   * the output of the {@code main} source set.
   */
  public static final String RESOLVE_MAIN_CLASS_NAME_TASK_NAME = "resolveMainClassName";

  /**
   * The name of the {@link ResolveMainClassName} task used to resolve a main class from
   * the output of the {@code test} source set then, if needed, the output of the
   * {@code main} source set.
   */
  public static final String RESOLVE_TEST_MAIN_CLASS_NAME_TASK_NAME = "resolveTestMainClassName";

  /**
   * The coordinates {@code (group:name:version)} of the
   * {@code infra-dependencies} bom.
   */
  public static final String BOM_COORDINATES = "cn.taketoday:infra-dependencies:" + INFRA_VERSION;

  @Override
  public void apply(Project project) {
    createExtension(project);
    Configuration infraArchives = createInfraArchivesConfiguration(project);
    registerPluginActions(project, infraArchives);
  }

  public static String dependenciesCoordinates(Project project) {
    Object property = project.findProperty("infra.dependencies");
    if (property != null) {
      var handler = PropertyPlaceholderHandler.shared(true);
      return handler.replacePlaceholders(property.toString(),
              placeholderName -> ObjectUtils.toString(project.findProperty(placeholderName)));
    }
    return BOM_COORDINATES;
  }

  private void createExtension(Project project) {
    project.getExtensions().create("infraApplication", InfraApplicationExtension.class, project);
  }

  private Configuration createInfraArchivesConfiguration(Project project) {
    Configuration infraArchives = project.getConfigurations().create(INFRA_ARCHIVES_CONFIGURATION_NAME);
    infraArchives.setDescription("Configuration for Infra archive artifacts.");
    infraArchives.setCanBeResolved(false);
    return infraArchives;
  }

  private void registerPluginActions(Project project, Configuration infraArchives) {
    var singlePublishedArtifact = new SinglePublishedArtifact(infraArchives, project.getArtifacts());
    List<PluginApplicationAction> actions = Arrays.asList(new JavaPluginAction(singlePublishedArtifact),
            new WarPluginAction(singlePublishedArtifact), new DependencyManagementPluginAction(),
            new ApplicationPluginAction(), new NativeImagePluginAction());
    for (PluginApplicationAction action : actions) {
      withPluginClassOfAction(action, pluginClass -> {
        if (action.autoApply(project)) {
          project.getPlugins().apply(pluginClass);
          action.execute(project);
        }
        else {
          project.getPlugins().withType(pluginClass, plugin -> action.execute(project));
        }
      });
    }
  }

  private void withPluginClassOfAction(PluginApplicationAction action,
          Consumer<Class<? extends Plugin<? extends Project>>> consumer) {
    Class<? extends Plugin<? extends Project>> pluginClass;
    try {
      pluginClass = action.getPluginClass();
    }
    catch (Throwable ex) {
      // Plugin class unavailable.
      return;
    }
    consumer.accept(pluginClass);
  }

}
