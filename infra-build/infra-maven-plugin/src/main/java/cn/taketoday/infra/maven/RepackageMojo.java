/*
 * Copyright 2012 - 2023 the original author or authors.
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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import cn.taketoday.app.loader.tools.DefaultLaunchScript;
import cn.taketoday.app.loader.tools.LaunchScript;
import cn.taketoday.app.loader.tools.LayoutFactory;
import cn.taketoday.app.loader.tools.Libraries;
import cn.taketoday.app.loader.tools.Repackager;

/**
 * Repackage existing JAR and WAR archives so that they can be executed from the command
 * line using {@literal java -jar}. With <code>layout=NONE</code> can also be used simply
 * to package a JAR with nested dependencies (and no main class, so not executable).
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Björn Lindström
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
      requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RepackageMojo extends AbstractPackagerMojo {

  private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");

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

  /**
   * Skip the execution.
   */
  @Parameter(property = "infra.repackage.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Classifier to add to the repackaged archive. If not given, the main artifact will
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
   * Attach the repackaged archive to be installed into your local Maven repository or
   * deployed to a remote repository. If no classifier has been configured, it will
   * replace the normal jar. If a {@code classifier} has been configured such that the
   * normal jar and the repackaged jar are different, it will be attached alongside the
   * normal jar. When the property is set to {@code false}, the repackaged archive will
   * not be installed or deployed.
   */
  @Parameter(defaultValue = "true")
  private boolean attach = true;

  /**
   * A list of the libraries that must be unpacked from fat jars in order to run.
   * Specify each library as a {@code <dependency>} with a {@code <groupId>} and a
   * {@code <artifactId>} and they will be unpacked at runtime.
   */
  @Parameter
  private List<Dependency> requiresUnpack;

  /**
   * Make a fully executable jar for *nix machines by prepending a launch script to the
   * jar.
   * <p>
   * Currently, some tools do not accept this format so you may not always be able to
   * use this technique. For example, {@code jar -xf} may silently fail to extract a jar
   * or war that has been made fully-executable. It is recommended that you only enable
   * this option if you intend to execute it directly, rather than running it with
   * {@code java -jar} or deploying it to a servlet container.
   */
  @Parameter(defaultValue = "false")
  private boolean executable;

  /**
   * The embedded launch script to prepend to the front of the jar if it is fully
   * executable. If not specified the 'Infra' default script will be used.
   */
  @Parameter
  private File embeddedLaunchScript;

  /**
   * Properties that should be expanded in the embedded launch script.
   */
  @Parameter
  private Properties embeddedLaunchScriptProperties;

  /**
   * Timestamp for reproducible output archive entries, either formatted as ISO 8601
   * (<code>yyyy-MM-dd'T'HH:mm:ssXXX</code>) or an {@code int} representing seconds
   * since the epoch.
   */
  @Parameter(defaultValue = "${project.build.outputTimestamp}")
  private String outputTimestamp;

  /**
   * The type of archive (which corresponds to how the dependencies are laid out inside
   * it). Possible values are {@code JAR}, {@code WAR}, {@code ZIP}, {@code DIR},
   * {@code NONE}. Defaults to a guess based on the archive type.
   */
  @Parameter(property = "infra.repackage.layout")
  private LayoutType layout;

  /**
   * The layout factory that will be used to create the executable archive if no
   * explicit layout is set. Alternative layouts implementations can be provided by 3rd
   * parties.
   */
  @Parameter
  private LayoutFactory layoutFactory;

  /**
   * Return the type of archive that should be packaged by this MOJO.
   *
   * @return the value of the {@code layout} parameter, or {@code null} if the parameter
   * is not provided
   */
  @Override
  protected LayoutType getLayout() {
    return this.layout;
  }

  /**
   * Return the layout factory that will be used to determine the
   * {@link AbstractPackagerMojo.LayoutType} if no explicit layout is set.
   *
   * @return the value of the {@code layoutFactory} parameter, or {@code null} if the
   * parameter is not provided
   */
  @Override
  protected LayoutFactory getLayoutFactory() {
    return this.layoutFactory;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (this.project.getPackaging().equals("pom")) {
      getLog().debug("repackage goal could not be applied to pom project.");
      return;
    }
    if (this.skip) {
      getLog().debug("skipping repackaging as per configuration.");
      return;
    }
    repackage();
  }

  private void repackage() throws MojoExecutionException {
    Artifact source = getSourceArtifact(this.classifier);
    File target = getTargetFile(this.finalName, this.classifier, this.outputDirectory);
    Repackager repackager = getRepackager(source.getFile());
    Libraries libraries = getLibraries(this.requiresUnpack);
    try {
      LaunchScript launchScript = getLaunchScript();
      repackager.repackage(target, libraries, launchScript, parseOutputTimestamp());
    }
    catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
    updateArtifact(source, target, repackager.getBackupFile());
  }

  private FileTime parseOutputTimestamp() {
    // Maven ignores a single-character timestamp as it is "useful to override a full
    // value during pom inheritance"
    if (this.outputTimestamp == null || this.outputTimestamp.length() < 2) {
      return null;
    }
    return FileTime.from(getOutputTimestampEpochSeconds(), TimeUnit.SECONDS);
  }

  private long getOutputTimestampEpochSeconds() {
    try {
      return Long.parseLong(this.outputTimestamp);
    }
    catch (NumberFormatException ex) {
      return OffsetDateTime.parse(this.outputTimestamp).toInstant().getEpochSecond();
    }
  }

  private Repackager getRepackager(File source) {
    return getConfiguredPackager(() -> new Repackager(source));
  }

  private LaunchScript getLaunchScript() throws IOException {
    if (this.executable || this.embeddedLaunchScript != null) {
      return new DefaultLaunchScript(this.embeddedLaunchScript, buildLaunchScriptProperties());
    }
    return null;
  }

  private Properties buildLaunchScriptProperties() {
    Properties properties = new Properties();
    if (this.embeddedLaunchScriptProperties != null) {
      properties.putAll(this.embeddedLaunchScriptProperties);
    }
    putIfMissing(properties, "initInfoProvides", this.project.getArtifactId());
    putIfMissing(properties, "initInfoShortDescription", this.project.getName(), this.project.getArtifactId());
    putIfMissing(properties, "initInfoDescription", removeLineBreaks(this.project.getDescription()),
            this.project.getName(), this.project.getArtifactId());
    return properties;
  }

  private String removeLineBreaks(String description) {
    return (description != null) ? WHITE_SPACE_PATTERN.matcher(description).replaceAll(" ") : null;
  }

  private void putIfMissing(Properties properties, String key, String... valueCandidates) {
    if (!properties.containsKey(key)) {
      for (String candidate : valueCandidates) {
        if (candidate != null && !candidate.isEmpty()) {
          properties.put(key, candidate);
          return;
        }
      }
    }
  }

  private void updateArtifact(Artifact source, File target, File original) {
    if (this.attach) {
      attachArtifact(source, target, original);
    }
    else if (source.getFile().equals(target) && original.exists()) {
      String artifactId = (this.classifier != null)
                          ? "artifact with classifier " + this.classifier
                          : "main artifact";
      getLog().info(String.format("Updating %s %s to %s", artifactId, source.getFile(), original));
      source.setFile(original);
    }
    else if (this.classifier != null) {
      getLog().info("Creating repackaged archive " + target + " with classifier " + this.classifier);
    }
  }

  private void attachArtifact(Artifact source, File target, File original) {
    if (this.classifier != null && !source.getFile().equals(target)) {
      getLog().info("Attaching repackaged archive " + target + " with classifier " + this.classifier);
      this.projectHelper.attachArtifact(this.project, this.project.getPackaging(), this.classifier, target);
    }
    else {
      String artifactId = (this.classifier != null)
                          ? "artifact with classifier " + this.classifier
                          : "main artifact";
      getLog().info(String.format("Replacing %s %s with repackaged archive, adding nested dependencies in APP-INF/.",
              artifactId, source.getFile()));
      getLog().info("The original artifact has been renamed to " + original);
      source.setFile(target);
    }
  }

}
