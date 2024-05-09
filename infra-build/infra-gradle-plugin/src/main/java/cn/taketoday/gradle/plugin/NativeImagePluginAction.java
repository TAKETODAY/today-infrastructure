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

import org.graalvm.buildtools.gradle.NativeImagePlugin;
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.gradle.tasks.bundling.InfraBuildImage;
import cn.taketoday.gradle.tasks.bundling.InfraJar;

import static cn.taketoday.gradle.plugin.InfraApplicationPlugin.INFRA_BUILD_IMAGE_TASK_NAME;
import static cn.taketoday.gradle.plugin.InfraApplicationPlugin.INFRA_JAR_TASK_NAME;

/**
 * {@link Action} that is executed in response to the {@link NativeImagePlugin} being
 * applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class NativeImagePluginAction implements PluginApplicationAction {

  @Override
  public Class<? extends Plugin<? extends Project>> getPluginClass()
          throws ClassNotFoundException, NoClassDefFoundError {
    return NativeImagePlugin.class;
  }

  @Override
  public void execute(Project project) {
    project.getPlugins().apply(InfraApplicationAotPlugin.class);
    project.getPlugins().withType(JavaPlugin.class).all((plugin) -> {
      JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
      SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
      GraalVMExtension graalVmExtension = configureGraalVmExtension(project);
      configureMainNativeBinaryClasspath(project, sourceSets, graalVmExtension);
      configureTestNativeBinaryClasspath(sourceSets, graalVmExtension);
      copyReachabilityMetadataToInfraJar(project);
      configureInfraBuildImageToProduceANativeImage(project);
      configureJarManifestNativeAttribute(project);
    });
  }

  private void configureMainNativeBinaryClasspath(Project project, SourceSetContainer sourceSets, GraalVMExtension graalVmExtension) {
    FileCollection runtimeClasspath = sourceSets.getByName(InfraApplicationAotPlugin.AOT_SOURCE_SET_NAME).getRuntimeClasspath();
    graalVmExtension.getBinaries().getByName(NativeImagePlugin.NATIVE_MAIN_EXTENSION).classpath(runtimeClasspath);
    Configuration nativeImageClasspath = project.getConfigurations().getByName("nativeImageClasspath");
    nativeImageClasspath.setExtendsFrom(removeDevelopmentOnly(nativeImageClasspath.getExtendsFrom()));
  }

  private Iterable<Configuration> removeDevelopmentOnly(Set<Configuration> configurations) {
    return configurations.stream()
            .filter(this::isNotDevelopmentOnly)
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private boolean isNotDevelopmentOnly(Configuration configuration) {
    return !InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME.equals(configuration.getName())
            && !InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME.equals(configuration.getName());
  }

  private void configureTestNativeBinaryClasspath(SourceSetContainer sourceSets, GraalVMExtension graalVmExtension) {
    FileCollection runtimeClasspath = sourceSets.getByName(InfraApplicationAotPlugin.AOT_TEST_SOURCE_SET_NAME)
            .getRuntimeClasspath();
    graalVmExtension.getBinaries().getByName(NativeImagePlugin.NATIVE_TEST_EXTENSION).classpath(runtimeClasspath);
  }

  private GraalVMExtension configureGraalVmExtension(Project project) {
    GraalVMExtension extension = project.getExtensions().getByType(GraalVMExtension.class);
    extension.getToolchainDetection().set(false);
    return extension;
  }

  private void copyReachabilityMetadataToInfraJar(Project project) {
    project.getTasks().named(INFRA_JAR_TASK_NAME, InfraJar.class)
            .configure(infraJar -> infraJar.from(project.getTasks().named("collectReachabilityMetadata")));
  }

  private void configureInfraBuildImageToProduceANativeImage(Project project) {
    project.getTasks().named(INFRA_BUILD_IMAGE_TASK_NAME, InfraBuildImage.class).configure(infraBuildImage -> {
      infraBuildImage.getBuilder().convention("paketobuildpacks/builder-jammy-tiny:latest");
      infraBuildImage.getEnvironment().put("BP_NATIVE_IMAGE", "true");
    });
  }

  private void configureJarManifestNativeAttribute(Project project) {
    project.getTasks()
            .named(INFRA_JAR_TASK_NAME, InfraJar.class)
            .configure(this::addNativeProcessedAttribute);
  }

  private void addNativeProcessedAttribute(InfraJar infraJar) {
    infraJar.manifest(this::addNativeProcessedAttribute);
  }

  private void addNativeProcessedAttribute(Manifest manifest) {
    manifest.getAttributes().put("Infra-App-Native-Processed", true);
  }

}
