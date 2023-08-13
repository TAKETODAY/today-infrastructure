/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.aot;

import java.io.IOException;
import java.nio.file.Path;

import cn.taketoday.aot.generate.FileSystemGeneratedFiles;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.nativex.FileNativeConfigurationWriter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileSystemUtils;

/**
 * Abstract base class for filesystem-based ahead-of-time (AOT) processing.
 *
 * <p>Concrete implementations should override {@link #doProcess()} that kicks
 * off the optimization of the target, usually an application.
 *
 * @param <T> the type of the processing result
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FileSystemGeneratedFiles
 * @see FileNativeConfigurationWriter
 * @see cn.taketoday.context.aot.ContextAotProcessor
 * @see cn.taketoday.test.context.aot.TestAotProcessor
 * @since 4.0
 */
public abstract class AbstractAotProcessor<T> {

  /**
   * The name of a system property that is made available when the processor
   * runs.
   *
   * @see #doProcess()
   */
  private static final String AOT_PROCESSING = "infra.aot.processing";

  private final Settings settings;

  /**
   * Create a new processor instance with the supplied {@linkplain Settings settings}.
   *
   * @see Settings#builder()
   */
  protected AbstractAotProcessor(Settings settings) {
    this.settings = settings;
  }

  /**
   * Get the {@linkplain Settings settings} for this AOT processor.
   */
  protected Settings getSettings() {
    return this.settings;
  }

  /**
   * Run AOT processing.
   *
   * @return the result of the processing.
   */
  public final T process() {
    try {
      System.setProperty(AOT_PROCESSING, "true");
      return doProcess();
    }
    finally {
      System.clearProperty(AOT_PROCESSING);
    }
  }

  protected abstract T doProcess();

  /**
   * Delete the source, resource, and class output directories.
   */
  protected void deleteExistingOutput() {
    deleteExistingOutput(getSettings().getSourceOutput(),
        getSettings().getResourceOutput(), getSettings().getClassOutput());
  }

  private void deleteExistingOutput(Path... paths) {
    for (Path path : paths) {
      try {
        FileSystemUtils.deleteRecursively(path);
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to delete existing output in '" + path + "'");
      }
    }
  }

  protected FileSystemGeneratedFiles createFileSystemGeneratedFiles() {
    return new FileSystemGeneratedFiles(this::getRoot);
  }

  private Path getRoot(Kind kind) {
    return switch (kind) {
      case CLASS -> getSettings().getClassOutput();
      case SOURCE -> getSettings().getSourceOutput();
      case RESOURCE -> getSettings().getResourceOutput();
    };
  }

  protected void writeHints(RuntimeHints hints) {
    FileNativeConfigurationWriter writer = new FileNativeConfigurationWriter(
        getSettings().getResourceOutput(), getSettings().getGroupId(), getSettings().getArtifactId());
    writer.write(hints);
  }

  /**
   * Common settings for AOT processors.
   */
  public static final class Settings {

    private final Path sourceOutput;

    private final Path resourceOutput;

    private final Path classOutput;

    @Nullable
    private final String groupId;

    @Nullable
    private final String artifactId;

    private Settings(Path sourceOutput, Path resourceOutput,
        Path classOutput, @Nullable String groupId, @Nullable String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.classOutput = classOutput;
      this.sourceOutput = sourceOutput;
      this.resourceOutput = resourceOutput;
    }

    /**
     * Create a new {@link Builder} for {@link Settings}.
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * Get the output directory for generated sources.
     */
    public Path getSourceOutput() {
      return this.sourceOutput;
    }

    /**
     * Get the output directory for generated resources.
     */
    public Path getResourceOutput() {
      return this.resourceOutput;
    }

    /**
     * Get the output directory for generated classes.
     */
    public Path getClassOutput() {
      return this.classOutput;
    }

    /**
     * Get the group ID of the application.
     */
    @Nullable
    public String getGroupId() {
      return this.groupId;
    }

    /**
     * Get the artifact ID of the application.
     */
    @Nullable
    public String getArtifactId() {
      return this.artifactId;
    }

    /**
     * Fluent builder API for {@link Settings}.
     */
    public static final class Builder {

      @Nullable
      private Path sourceOutput;

      @Nullable
      private Path resourceOutput;

      @Nullable
      private Path classOutput;

      @Nullable
      private String groupId;

      @Nullable
      private String artifactId;

      private Builder() {
        // internal constructor
      }

      /**
       * Set the output directory for generated sources.
       *
       * @param sourceOutput the location of generated sources
       * @return this builder for method chaining
       */
      public Builder sourceOutput(Path sourceOutput) {
        this.sourceOutput = sourceOutput;
        return this;
      }

      /**
       * Set the output directory for generated resources.
       *
       * @param resourceOutput the location of generated resources
       * @return this builder for method chaining
       */
      public Builder resourceOutput(Path resourceOutput) {
        this.resourceOutput = resourceOutput;
        return this;
      }

      /**
       * Set the output directory for generated classes.
       *
       * @param classOutput the location of generated classes
       * @return this builder for method chaining
       */
      public Builder classOutput(Path classOutput) {
        this.classOutput = classOutput;
        return this;
      }

      /**
       * Set the group ID of the application.
       *
       * @param groupId the group ID of the application, used to locate
       * {@code native-image.properties}
       * @return this builder for method chaining
       */
      public Builder groupId(String groupId) {
        this.groupId = groupId;
        return this;
      }

      /**
       * Set the artifact ID of the application.
       *
       * @param artifactId the artifact ID of the application, used to locate
       * {@code native-image.properties}
       * @return this builder for method chaining
       */
      public Builder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
      }

      /**
       * Build the {@link Settings} configured in this {@code Builder}.
       */
      public Settings build() {
        Assert.notNull(this.sourceOutput, "'sourceOutput' must not be null");
        Assert.notNull(this.resourceOutput, "'resourceOutput' must not be null");
        Assert.notNull(this.classOutput, "'classOutput' must not be null");
        Assert.hasText(this.groupId, "'groupId' must not be null or empty");
        Assert.hasText(this.artifactId, "'artifactId' must not be null or empty");
        return new Settings(this.sourceOutput, this.resourceOutput, this.classOutput,
            this.groupId, this.artifactId);
      }

    }

  }

}
