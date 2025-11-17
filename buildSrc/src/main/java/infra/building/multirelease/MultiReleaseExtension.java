/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.building.multirelease;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import javax.inject.Inject;

/**
 * @author Cedric Champeau
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class MultiReleaseExtension {

  private final TaskContainer tasks;

  private final ObjectFactory objects;

  private final SourceSetContainer sourceSets;

  private final DependencyHandler dependencies;

  private final ConfigurationContainer configurations;

  @Inject
  public MultiReleaseExtension(SourceSetContainer sourceSets,
          ConfigurationContainer configurations, TaskContainer tasks,
          DependencyHandler dependencies, ObjectFactory objectFactory) {
    this.sourceSets = sourceSets;
    this.configurations = configurations;
    this.tasks = tasks;
    this.dependencies = dependencies;
    this.objects = objectFactory;
  }

  public void releaseVersions(int... javaVersions) {
    releaseVersions("src/main/", "src/test/", javaVersions);
  }

  private void releaseVersions(String mainSourceDirectory, String testSourceDirectory, int... javaVersions) {
    for (int javaVersion : javaVersions) {
      addLanguageVersion(javaVersion, mainSourceDirectory, testSourceDirectory);
    }
  }

  private void addLanguageVersion(int javaVersion, String mainSourceDirectory, String testSourceDirectory) {
    String javaN = "java" + javaVersion;

    SourceSet langSourceSet = sourceSets.create(javaN, srcSet -> srcSet.getJava().srcDir(mainSourceDirectory + javaN));
    SourceSet testSourceSet = sourceSets.create(javaN + "Test", srcSet -> srcSet.getJava().srcDir(testSourceDirectory + javaN));
    SourceSet sharedSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet sharedTestSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);

    FileCollection mainClasses = objects.fileCollection().from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs());
    dependencies.add(javaN + "Implementation", mainClasses);

    tasks.named(langSourceSet.getCompileJavaTaskName(), JavaCompile.class, task ->
            task.getOptions().getRelease().set(javaVersion)
    );
    tasks.named(testSourceSet.getCompileJavaTaskName(), JavaCompile.class, task ->
            task.getOptions().getRelease().set(javaVersion)
    );

    TaskProvider<Test> testTask = createTestTask(javaVersion, testSourceSet, sharedTestSourceSet, langSourceSet, sharedSourceSet);
    tasks.named("check", task -> task.dependsOn(testTask));

    configureMultiReleaseJar(javaVersion, langSourceSet);
  }

  private TaskProvider<Test> createTestTask(int javaVersion, SourceSet testSourceSet, SourceSet sharedTestSourceSet, SourceSet langSourceSet, SourceSet sharedSourceSet) {
    Configuration testImplementation = configurations.getByName(testSourceSet.getImplementationConfigurationName());
    testImplementation.extendsFrom(configurations.getByName(sharedTestSourceSet.getImplementationConfigurationName()));
    Configuration testCompileOnly = configurations.getByName(testSourceSet.getCompileOnlyConfigurationName());
    testCompileOnly.extendsFrom(configurations.getByName(sharedTestSourceSet.getCompileOnlyConfigurationName()));
    testCompileOnly.getDependencies().add(dependencies.create(langSourceSet.getOutput().getClassesDirs()));
    testCompileOnly.getDependencies().add(dependencies.create(sharedSourceSet.getOutput().getClassesDirs()));

    Configuration testRuntimeClasspath = configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName());
    // so here's the deal. MRjars are JARs! Which means that to execute tests, we need
    // the JAR on classpath, not just classes + resources as Gradle usually does
    testRuntimeClasspath.getAttributes()
            .attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));

    return tasks.register("java" + javaVersion + "Test", Test.class, test -> {
      test.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);

      ConfigurableFileCollection testClassesDirs = objects.fileCollection();
      testClassesDirs.from(testSourceSet.getOutput());
      testClassesDirs.from(sharedTestSourceSet.getOutput());
      test.setTestClassesDirs(testClassesDirs);
      ConfigurableFileCollection classpath = objects.fileCollection();
      // must put the MRJar first on classpath
      classpath.from(tasks.named("jar"));
      // then we put the specific test sourceset tests, so that we can override
      // the shared versions
      classpath.from(testSourceSet.getOutput());

      // then we add the shared tests
      classpath.from(sharedTestSourceSet.getRuntimeClasspath());
      test.setClasspath(classpath);
    });
  }

  private void configureMultiReleaseJar(int version, SourceSet languageSourceSet) {
    tasks.named("jar", Jar.class, jar -> {
      jar.into("META-INF/versions/" + version, s -> s.from(languageSourceSet.getOutput()));
      Attributes attributes = jar.getManifest().getAttributes();
      attributes.put("Multi-Release", "true");
    });
  }

}
