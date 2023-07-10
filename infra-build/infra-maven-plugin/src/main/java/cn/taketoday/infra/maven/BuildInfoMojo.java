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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.app.loader.tools.BuildPropertiesWriter;
import cn.taketoday.app.loader.tools.BuildPropertiesWriter.NullAdditionalPropertyValueException;
import cn.taketoday.app.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * Generate a {@code build-info.properties} file based on the content of the current
 * {@link MavenProject}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "build-info", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class BuildInfoMojo extends AbstractMojo {

  @Component
  private BuildContext buildContext;

  /**
   * The Maven session.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The location of the generated {@code build-info.properties} file.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/build-info.properties")
  private File outputFile;

  /**
   * The value used for the {@code build.time} property in a form suitable for
   * {@link Instant#parse(CharSequence)}. Defaults to
   * {@code project.build.outputTimestamp} or {@code session.request.startTime} if the
   * former is not set. To disable the {@code build.time} property entirely, use
   * {@code 'off'} or add it to {@code excludeInfoProperties}.
   */
  @Parameter(defaultValue = "${project.build.outputTimestamp}")
  private String time;

  /**
   * Additional properties to store in the {@code build-info.properties} file. Each
   * entry is prefixed by {@code build.} in the generated {@code build-info.properties}.
   */
  @Parameter
  private Map<String, String> additionalProperties;

  /**
   * Properties that should be excluded {@code build-info.properties} file. Can be used
   * to exclude the standard {@code group}, {@code artifact}, {@code name},
   * {@code version} or {@code time} properties as well as items from
   * {@code additionalProperties}.
   */
  @Parameter
  private List<String> excludeInfoProperties;

  /**
   * Skip the execution.
   */
  @Parameter(property = "today-infra.build-info.skip", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (this.skip) {
      getLog().debug("skipping build-info as per configuration.");
      return;
    }
    try {
      ProjectDetails details = getProjectDetails();
      new BuildPropertiesWriter(this.outputFile).writeBuildProperties(details);
      this.buildContext.refresh(this.outputFile);
    }
    catch (NullAdditionalPropertyValueException ex) {
      throw new MojoFailureException("Failed to generate build-info.properties. " + ex.getMessage(), ex);
    }
    catch (Exception ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  private ProjectDetails getProjectDetails() {
    String group = getIfNotExcluded("group", this.project.getGroupId());
    String artifact = getIfNotExcluded("artifact", this.project.getArtifactId());
    String version = getIfNotExcluded("version", this.project.getVersion());
    String name = getIfNotExcluded("name", this.project.getName());
    Instant time = getIfNotExcluded("time", getBuildTime());
    Map<String, String> additionalProperties = applyExclusions(this.additionalProperties);
    return new ProjectDetails(group, artifact, version, name, time, additionalProperties);
  }

  private <T> T getIfNotExcluded(String name, T value) {
    return (this.excludeInfoProperties == null || !this.excludeInfoProperties.contains(name)) ? value : null;
  }

  private Map<String, String> applyExclusions(Map<String, String> source) {
    if (source == null || this.excludeInfoProperties == null) {
      return source;
    }
    Map<String, String> result = new LinkedHashMap<>(source);
    this.excludeInfoProperties.forEach(result::remove);
    return result;
  }

  private Instant getBuildTime() {
    if (this.time == null || this.time.isEmpty()) {
      Date startTime = this.session.getRequest().getStartTime();
      return (startTime != null) ? startTime.toInstant() : Instant.now();
    }
    if ("off".equalsIgnoreCase(this.time)) {
      return null;
    }
    return Instant.parse(this.time);
  }

}
