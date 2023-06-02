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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;

/**
 * Build the project to specified product
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/11 15:06
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class BuildMojo extends AbstractDependencyFilterMojo {

  /**
   * Skip the execution.
   */
  @Parameter(property = "today-infra.build.skip", defaultValue = "false")
  public boolean skip;

  /**
   * The Maven Session Object
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession mavenSession;

  /**
   * Include system scoped dependencies.
   */
  @Parameter(defaultValue = "false")
  public boolean includeSystemScope;

  /**
   * Classifier to add to the packaged archive. If not given, the main artifact will
   * be replaced by the repackaged archive. If given, the classifier will also be used
   * to determine the source archive to repackage: if an artifact with that classifier
   * already exists, it will be used as source and replaced. If no such artifact exists,
   * the main artifact will be used as source and the repackaged archive will be
   * attached as a supplemental artifact with that classifier. Attaching the artifact
   * allows to deploy it alongside to the original one, see <a href=
   * "https://maven.apache.org/plugins/maven-deploy-plugin/examples/deploying-with-classifiers.html"
   * >the Maven documentation for more details</a>.
   */
  @Parameter
  private String classifier;

  /**
   * Directory containing the generated archive.
   */
  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File outputDirectory;

  /**
   * Name of the generated archive.
   */
  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  private String finalName;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    if (skip) {
      log.debug("skipping build-info as per configuration.");
      return;
    }

    if ("pom".equals(project.getPackaging())) {
      log.debug("repackage goal could not be applied to pom project.");
      return;
    }

    Artifact source = getSourceArtifact(this.classifier);
    File target = getTargetFile(this.finalName, this.classifier, this.outputDirectory);

    log.info(LogMessage.format("source: {}, target: {}", source, target));
    executeInternal();
  }

  protected final void executeInternal() throws MojoExecutionException {
    Log log = getLog();
    Set<Artifact> artifacts = this.project.getArtifacts();
    Set<Artifact> includedArtifacts = filterDependencies(artifacts, getAdditionalFilters());
    for (Artifact includedArtifact : includedArtifacts) {
      log.info(LogMessage.format("includedArtifact: {}", includedArtifact));
    }
  }

  private ArtifactsFilter[] getAdditionalFilters() {
    ArrayList<ArtifactsFilter> filters = new ArrayList<>();
    if (!includeSystemScope) {
      filters.add(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
    }

    return filters.toArray(new ArtifactsFilter[0]);
  }

  /**
   * Return the source {@link Artifact} to repackage. If a classifier is specified and
   * an artifact with that classifier exists, it is used. Otherwise, the main artifact
   * is used.
   *
   * @param classifier the artifact classifier
   * @return the source artifact to repackage
   */
  protected Artifact getSourceArtifact(String classifier) {
    Artifact sourceArtifact = getArtifact(classifier);
    return sourceArtifact != null ? sourceArtifact : this.project.getArtifact();
  }

  @Nullable
  private Artifact getArtifact(String classifier) {
    if (classifier != null) {
      for (Artifact attachedArtifact : this.project.getAttachedArtifacts()) {
        if (classifier.equals(attachedArtifact.getClassifier())
                && attachedArtifact.getFile() != null
                && attachedArtifact.getFile().isFile()) {
          return attachedArtifact;
        }
      }
    }
    return null;
  }

  protected File getTargetFile(String finalName, String classifier, File targetDirectory) {
    String classifierSuffix = classifier != null ? classifier.trim() : "";
    if (!classifierSuffix.isEmpty() && !classifierSuffix.startsWith("-")) {
      classifierSuffix = "-" + classifierSuffix;
    }
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs();
    }
    return new File(targetDirectory, finalName + classifierSuffix + "." +
            project.getArtifact().getArtifactHandler().getExtension());
  }

}
