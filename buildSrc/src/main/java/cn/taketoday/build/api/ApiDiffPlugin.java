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

package cn.taketoday.build.api;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import me.champeau.gradle.japicmp.JapicmpPlugin;
import me.champeau.gradle.japicmp.JapicmpTask;

/**
 * {@link Plugin} that applies the {@code "japicmp-gradle-plugin"}
 * and create tasks for all subprojects named {@code "today-*"}, diffing the public API one by one
 * and creating the reports in {@code "build/reports/api-diff/$OLDVERSION_to_$NEWVERSION/"}.
 * <p>{@code "./gradlew apiDiff -PbaselineVersion=5.1.0.RELEASE"} will output the
 * reports for the API diff between the baseline version and the current one for all modules.
 * You can limit the report to a single module with
 * {@code "./gradlew :today-core:apiDiff -PbaselineVersion=5.1.0.RELEASE"}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApiDiffPlugin implements Plugin<Project> {

  private static final Logger logger = LoggerFactory.getLogger(ApiDiffPlugin.class);

  public static final String TASK_NAME = "apiDiff";

  private static final String BASELINE_VERSION_PROPERTY = "baselineVersion";

  private static final List<String> PACKAGE_INCLUDES = Collections.singletonList("cn.taketoday.*");

  private static final URI INFRA_MILESTONE_REPOSITORY = URI.create("https://repo.spring.io/milestone");

  @Override
  public void apply(Project project) {
    if (project.hasProperty(BASELINE_VERSION_PROPERTY) && project.equals(project.getRootProject())) {
      project.getPluginManager().apply(JapicmpPlugin.class);
      project.getPlugins().withType(JapicmpPlugin.class,
              plugin -> applyApiDiffConventions(project));
    }
  }

  private void applyApiDiffConventions(Project project) {
    String baselineVersion = project.property(BASELINE_VERSION_PROPERTY).toString();
    project.subprojects(subProject -> {
      if (subProject.getName().startsWith("today-")) {
        createApiDiffTask(baselineVersion, subProject);
      }
    });
  }

  private void createApiDiffTask(String baselineVersion, Project project) {
    if (isProjectEligible(project)) {
      // Add Infra Milestone repository for generating diffs against previous milestones
      project.getRootProject()
              .getRepositories()
              .maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(INFRA_MILESTONE_REPOSITORY));
      JapicmpTask apiDiff = project.getTasks().create(TASK_NAME, JapicmpTask.class);
      apiDiff.setDescription("Generates an API diff report with japicmp");
      apiDiff.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);

      apiDiff.getOldClasspath().setFrom(createBaselineConfiguration(baselineVersion, project));
      TaskProvider<Jar> jar = project.getTasks().withType(Jar.class).named("jar");
      apiDiff.getNewArchives().setFrom(project.getLayout().files(jar.get().getArchiveFile().get().getAsFile()));
      apiDiff.getNewClasspath().setFrom(getRuntimeClassPath(project));
      apiDiff.getPackageIncludes().set(PACKAGE_INCLUDES);
      apiDiff.getOnlyModified().set(true);
      apiDiff.getIgnoreMissingClasses().set(true);
      // Ignore Kotlin metadata annotations since they contain
      // illegal HTML characters and fail the report generation
      apiDiff.getAnnotationExcludes().set(Collections.singletonList("@kotlin.Metadata"));

      apiDiff.getHtmlOutputFile().set(getOutputFile(baselineVersion, project));

      apiDiff.dependsOn(project.getTasks().getByName("jar"));
    }
  }

  private boolean isProjectEligible(Project project) {
    return project.getPlugins().hasPlugin(JavaPlugin.class)
            && project.getPlugins().hasPlugin(MavenPublishPlugin.class);
  }

  private Configuration createBaselineConfiguration(String baselineVersion, Project project) {
    String baseline = String.join(":",
            project.getGroup().toString(), project.getName(), baselineVersion);
    Dependency baselineDependency = project.getDependencies().create(baseline + "@jar");
    Configuration baselineConfiguration = project.getRootProject().getConfigurations().detachedConfiguration(baselineDependency);
    try {
      // eagerly resolve the baseline configuration to check whether this is a new Infra module
      baselineConfiguration.resolve();
      return baselineConfiguration;
    }
    catch (GradleException exception) {
      logger.warn("Could not resolve {} - assuming this is a new Infra module.", baseline);
    }
    return project.getRootProject().getConfigurations().detachedConfiguration();
  }

  private Configuration getRuntimeClassPath(Project project) {
    return project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
  }

  private File getOutputFile(String baseLineVersion, Project project) {
    Path outDir = Paths.get(project.getRootProject().getBuildDir().getAbsolutePath(),
            "reports", "api-diff",
            baseLineVersion + "_to_" + project.getRootProject().getVersion());
    return project.file(outDir.resolve(project.getName() + ".html").toString());
  }

}