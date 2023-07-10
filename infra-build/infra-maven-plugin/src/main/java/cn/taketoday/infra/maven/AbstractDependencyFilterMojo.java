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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A base mojo filtering the dependencies of the project.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * Collection of artifact definitions to include. The {@link Include} element defines
   * mandatory {@code groupId} and {@code artifactId} properties and an optional
   * mandatory {@code groupId} and {@code artifactId} properties and an optional
   * {@code classifier} property.
   */
  @Parameter(property = "infra.includes")
  private List<Include> includes;

  /**
   * Collection of artifact definitions to exclude. The {@link Exclude} element defines
   * mandatory {@code groupId} and {@code artifactId} properties and an optional
   * {@code classifier} property.
   */
  @Parameter(property = "infra.excludes")
  private List<Exclude> excludes;

  /**
   * Comma separated list of groupId names to exclude (exact match).
   */
  @Parameter(property = "infra.excludeGroupIds", defaultValue = "")
  private String excludeGroupIds;

  protected void setExcludes(List<Exclude> excludes) {
    this.excludes = excludes;
  }

  protected void setIncludes(List<Include> includes) {
    this.includes = includes;
  }

  protected void setExcludeGroupIds(String excludeGroupIds) {
    this.excludeGroupIds = excludeGroupIds;
  }

  protected List<URL> getDependencyURLs(ArtifactsFilter... additionalFilters) throws MojoExecutionException {
    Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), additionalFilters);
    List<URL> urls = new ArrayList<>();
    for (Artifact artifact : artifacts) {
      if (artifact.getFile() != null) {
        urls.add(toURL(artifact.getFile()));
      }
    }
    return urls;
  }

  protected final Set<Artifact> filterDependencies(Set<Artifact> dependencies, ArtifactsFilter... additionalFilters)
          throws MojoExecutionException {
    try {
      Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
      filtered.retainAll(getFilters(additionalFilters).filter(dependencies));
      return filtered;
    }
    catch (ArtifactFilterException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  protected URL toURL(File file) {
    try {
      return file.toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException("Invalid URL for " + file, ex);
    }
  }

  /**
   * Return artifact filters configured for this MOJO.
   *
   * @param additionalFilters optional additional filters to apply
   * @return the filters
   */
  private FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
    FilterArtifacts filters = new FilterArtifacts();
    for (ArtifactsFilter additionalFilter : additionalFilters) {
      filters.addFilter(additionalFilter);
    }
    filters.addFilter(new MatchingGroupIdFilter(cleanFilterConfig(this.excludeGroupIds)));
    if (this.includes != null && !this.includes.isEmpty()) {
      filters.addFilter(new IncludeFilter(this.includes));
    }
    if (this.excludes != null && !this.excludes.isEmpty()) {
      filters.addFilter(new ExcludeFilter(this.excludes));
    }
    filters.addFilter(new JarTypeFilter());
    return filters;
  }

  private String cleanFilterConfig(String content) {
    if (content == null || content.trim().isEmpty()) {
      return "";
    }
    StringBuilder cleaned = new StringBuilder();
    StringTokenizer tokenizer = new StringTokenizer(content, ",");
    while (tokenizer.hasMoreElements()) {
      cleaned.append(tokenizer.nextToken().trim());
      if (tokenizer.hasMoreElements()) {
        cleaned.append(",");
      }
    }
    return cleaned.toString();
  }

  /**
   * {@link ArtifactFilter} to exclude test scope dependencies.
   */
  protected static class ExcludeTestScopeArtifactFilter extends AbstractArtifactFeatureFilter {

    ExcludeTestScopeArtifactFilter() {
      super("", Artifact.SCOPE_TEST);
    }

    @Override
    protected String getArtifactFeature(Artifact artifact) {
      return artifact.getScope();
    }

  }

  /**
   * {@link ArtifactFilter} that only include runtime scopes.
   */
  protected static class RuntimeArtifactFilter implements ArtifactFilter {

    private static final Collection<String> SCOPES = List.of(Artifact.SCOPE_COMPILE,
            Artifact.SCOPE_COMPILE_PLUS_RUNTIME, Artifact.SCOPE_RUNTIME);

    @Override
    public boolean include(Artifact artifact) {
      String scope = artifact.getScope();
      return !artifact.isOptional() && (scope == null || SCOPES.contains(scope));
    }

  }
}
