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

package cn.taketoday.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.gradle.tasks.aot.AbstractAot;
import cn.taketoday.gradle.tasks.aot.ProcessAot;
import cn.taketoday.gradle.tasks.aot.ProcessTestAot;

import static cn.taketoday.gradle.plugin.InfraApplicationPlugin.dependenciesCoordinates;

/**
 * Gradle plugin for Infra AOT.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraApplicationAotPlugin implements Plugin<Project> {

  /**
   * Name of the main {@code aot} {@link SourceSet source set}.
   */
  public static final String AOT_SOURCE_SET_NAME = "aot";

  /**
   * Name of the {@code aotTest} {@link SourceSet source set}.
   */
  public static final String AOT_TEST_SOURCE_SET_NAME = "aotTest";

  /**
   * Name of the default {@link ProcessAot} task.
   */
  public static final String PROCESS_AOT_TASK_NAME = "processAot";

  /**
   * Name of the default {@link ProcessAot} task.
   */
  public static final String PROCESS_TEST_AOT_TASK_NAME = "processTestAot";

  @Override
  public void apply(Project project) {
    PluginContainer plugins = project.getPlugins();
    plugins.withType(JavaPlugin.class).all((javaPlugin) -> {
      JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
      SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
      SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
      SourceSet aotSourceSet = configureSourceSet(project, AOT_SOURCE_SET_NAME, mainSourceSet);
      SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
      SourceSet aotTestSourceSet = configureSourceSet(project, AOT_TEST_SOURCE_SET_NAME, testSourceSet);
      plugins.withType(InfraApplicationPlugin.class).all(plugin -> {
        registerProcessAotTask(project, aotSourceSet, mainSourceSet);
        registerProcessTestAotTask(project, mainSourceSet, aotTestSourceSet, testSourceSet);
      });
    });
  }

  private SourceSet configureSourceSet(Project project, String newSourceSetName, SourceSet existingSourceSet) {
    JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
    SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
    return sourceSets.create(newSourceSetName, (sourceSet) -> {
      existingSourceSet.setRuntimeClasspath(existingSourceSet.getRuntimeClasspath().plus(sourceSet.getOutput()));
      project.getConfigurations()
              .getByName(sourceSet.getCompileClasspathConfigurationName())
              .attributes((attributes) -> {
                configureClassesAndResourcesLibraryElementsAttribute(project, attributes);
                configureJavaRuntimeUsageAttribute(project, attributes);
              });
    });
  }

  private void configureClassesAndResourcesLibraryElementsAttribute(Project project, AttributeContainer attributes) {
    LibraryElements classesAndResources = project.getObjects()
            .named(LibraryElements.class, LibraryElements.CLASSES_AND_RESOURCES);
    attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, classesAndResources);
  }

  private void configureJavaRuntimeUsageAttribute(Project project, AttributeContainer attributes) {
    Usage javaRuntime = project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME);
    attributes.attribute(Usage.USAGE_ATTRIBUTE, javaRuntime);
  }

  private void registerProcessAotTask(Project project, SourceSet aotSourceSet, SourceSet mainSourceSet) {
    TaskProvider<ResolveMainClassName> resolveMainClassName = project.getTasks().named(InfraApplicationPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class);
    Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_AOT_TASK_NAME, mainSourceSet,
            Set.of(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME,
                    InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME));
    project.getDependencies().add(aotClasspath.getName(), project.files(mainSourceSet.getOutput()));
    if (!project.hasProperty("skipAOTDependency")) {
      addAOTDependency(project, aotClasspath);
    }
    Configuration compileClasspath = project.getConfigurations()
            .getByName(aotSourceSet.getCompileClasspathConfigurationName());
    compileClasspath.extendsFrom(aotClasspath);
    Provider<Directory> resourcesOutput = project.getLayout()
            .getBuildDirectory()
            .dir("generated/%sResources".formatted(aotSourceSet.getName()));
    TaskProvider<ProcessAot> processAot = project.getTasks().register(PROCESS_AOT_TASK_NAME, ProcessAot.class, (task) -> {
      configureAotTask(project, aotSourceSet, task, resourcesOutput);
      task.getApplicationMainClass().set(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
      task.setClasspath(aotClasspath);
    });
    aotSourceSet.getJava().srcDir(processAot.map(ProcessAot::getSourcesOutput));
    aotSourceSet.getResources().srcDir(resourcesOutput);
    ConfigurableFileCollection classesOutputFiles = project.files(processAot.map(ProcessAot::getClassesOutput));
    mainSourceSet.setRuntimeClasspath(mainSourceSet.getRuntimeClasspath().plus(classesOutputFiles));
    project.getDependencies().add(aotSourceSet.getImplementationConfigurationName(), classesOutputFiles);
    configureDependsOn(project, aotSourceSet, processAot);
  }

  private void configureAotTask(Project project, SourceSet sourceSet, AbstractAot task, Provider<Directory> resourcesOutput) {
    task.setGroup(BasePlugin.BUILD_GROUP);
    task.getSourcesOutput()
            .set(project.getLayout().getBuildDirectory().dir("generated/%sSources".formatted(sourceSet.getName())));
    task.getResourcesOutput().set(resourcesOutput);
    task.getClassesOutput()
            .set(project.getLayout().getBuildDirectory().dir("generated/%sClasses".formatted(sourceSet.getName())));
    task.getGroupId().set(project.provider(() -> String.valueOf(project.getGroup())));
    task.getArtifactId().set(project.provider(project::getName));
    configureToolchainConvention(project, task);
  }

  private void configureToolchainConvention(Project project, AbstractAot aotTask) {
    JavaToolchainSpec toolchain = project.getExtensions().getByType(JavaPluginExtension.class).getToolchain();
    JavaToolchainService toolchainService = project.getExtensions().getByType(JavaToolchainService.class);
    aotTask.getJavaLauncher().convention(toolchainService.launcherFor(toolchain));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Configuration createAotProcessingClasspath(Project project, String taskName, SourceSet inputSourceSet,
          Set<String> developmentOnlyConfigurationNames) {
    Configuration base = project.getConfigurations()
            .getByName(inputSourceSet.getRuntimeClasspathConfigurationName());
    return project.getConfigurations().create(taskName + "Classpath", (classpath) -> {
      classpath.setCanBeConsumed(false);
      if (!classpath.isCanBeResolved()) {
        throw new IllegalStateException("Unexpected");
      }
      classpath.setCanBeResolved(true);
      classpath.setDescription("Classpath of the %s task.".formatted(taskName));
      removeDevelopmentOnly(base.getExtendsFrom(), developmentOnlyConfigurationNames)
              .forEach(classpath::extendsFrom);
      classpath.attributes((attributes) -> {
        ProviderFactory providers = project.getProviders();
        AttributeContainer baseAttributes = base.getAttributes();
        for (Attribute attribute : baseAttributes.keySet()) {
          attributes.attributeProvider(attribute,
                  providers.provider(() -> baseAttributes.getAttribute(attribute)));
        }
      });
    });
  }

  private Stream<Configuration> removeDevelopmentOnly(Set<Configuration> configurations,
          Set<String> developmentOnlyConfigurationNames) {
    return configurations.stream().filter(
            configuration -> !developmentOnlyConfigurationNames.contains(configuration.getName()));
  }

  private void configureDependsOn(Project project, SourceSet aotSourceSet,
          TaskProvider<? extends AbstractAot> processAot) {
    project.getTasks()
            .named(aotSourceSet.getProcessResourcesTaskName())
            .configure((processResources) -> processResources.dependsOn(processAot));
  }

  private void registerProcessTestAotTask(Project project, SourceSet mainSourceSet, SourceSet aotTestSourceSet, SourceSet testSourceSet) {
    Configuration aotClasspath = createAotProcessingClasspath(project, PROCESS_TEST_AOT_TASK_NAME, testSourceSet,
            Set.of(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME));
    addJUnitPlatformLauncherDependency(project, aotClasspath);
    Configuration compileClasspath = project.getConfigurations()
            .getByName(aotTestSourceSet.getCompileClasspathConfigurationName());
    compileClasspath.extendsFrom(aotClasspath);
    Provider<Directory> resourcesOutput = project.getLayout()
            .getBuildDirectory()
            .dir("generated/%sResources".formatted(aotTestSourceSet.getName()));
    TaskProvider<ProcessTestAot> processTestAot = project.getTasks()
            .register(PROCESS_TEST_AOT_TASK_NAME, ProcessTestAot.class, (task) -> {
              configureAotTask(project, aotTestSourceSet, task, resourcesOutput);
              task.setClasspath(aotClasspath);
              task.setClasspathRoots(testSourceSet.getOutput());
            });
    aotTestSourceSet.getJava().srcDir(processTestAot.map(ProcessTestAot::getSourcesOutput));
    aotTestSourceSet.getResources().srcDir(resourcesOutput);
    project.getDependencies().add(aotClasspath.getName(), project.files(mainSourceSet.getOutput()));
    project.getDependencies().add(aotClasspath.getName(), project.files(testSourceSet.getOutput()));
    ConfigurableFileCollection classesOutputFiles = project
            .files(processTestAot.map(ProcessTestAot::getClassesOutput));
    testSourceSet.setRuntimeClasspath(testSourceSet.getRuntimeClasspath().plus(classesOutputFiles));
    project.getDependencies().add(aotTestSourceSet.getImplementationConfigurationName(), classesOutputFiles);
    configureDependsOn(project, aotTestSourceSet, processTestAot);
  }

  private void addJUnitPlatformLauncherDependency(Project project, Configuration configuration) {
    DependencyHandler dependencyHandler = project.getDependencies();
    Dependency infraDependencies = dependencyHandler.create(dependencyHandler.platform(dependenciesCoordinates(project)));
    DependencySet dependencies = configuration.getDependencies();
    dependencies.add(infraDependencies);
    dependencies.add(dependencyHandler.create("cn.taketoday:today-core-aot"));
    dependencies.add(dependencyHandler.create("org.junit.platform:junit-platform-launcher"));
  }

  /**
   * @since 5.0
   */
  private void addAOTDependency(Project project, Configuration configuration) {
    DependencyHandler dependencyHandler = project.getDependencies();
    Dependency infraDependencies = dependencyHandler.create(dependencyHandler.platform(dependenciesCoordinates(project)));
    DependencySet dependencies = configuration.getDependencies();
    dependencies.add(infraDependencies);
    dependencies.add(dependencyHandler.create("cn.taketoday:today-core-aot"));
  }

}
