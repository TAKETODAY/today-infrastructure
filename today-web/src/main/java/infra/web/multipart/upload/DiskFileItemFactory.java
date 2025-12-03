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
package infra.web.multipart.upload;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.build.AbstractOrigin;
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.file.PathUtils;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * The default {@link FileItemFactory} implementation.
 * <p>
 * This implementation creates {@link FileItem} instances which keep their content either in memory, for smaller items, or in a temporary file on disk, for
 * larger items. The size threshold, above which content will be stored on disk, is configurable, as is the directory in which temporary files will be created.
 * </p>
 * <p>
 * If not otherwise configured, the default configuration values are as follows:
 * </p>
 * <ul>
 * <li>Size threshold is 10 KB.</li>
 * <li>Repository is the system default temporary directory, as returned by {@code System.getProperty("java.io.tmpdir")}.</li>
 * </ul>
 * <p><em>State model</em>: The created instances of {@link DiskFileItem} are subject to a carefully designed state model,
 * which is also controlled by the threshold. Therefore, it is strongly recommended to set the threshold explicitly, using
 * {@link Builder#setThreshold(int)}. Details
 * on the state model can be found {@link DiskFileItem here}.</p>
 * <p>
 * <strong>NOTE</strong>: Files are created in the system default temporary directory with predictable names. This means that a local attacker with write access
 * to that directory can perform a TOUTOC attack to replace any uploaded file with a file of the attackers choice. The implications of this will depend on how
 * the uploaded file is used, but could be significant. When using this implementation in an environment with local, untrusted users,
 * {@link Builder#setPath(Path)} MUST be used to configure a repository location that is not publicly writable. In a Servlet container the location identified
 * by the ServletContext attribute {@code javax.servlet.context.tempdir} may be used.
 * </p>
 * <p>
 * Temporary files, which are created for file items, should be deleted later on. The best way to do this is using a {@link FileCleaningTracker}, which you can
 * set on the {@link DiskFileItemFactory}. However, if you do use such a tracker, then you must consider the following: Temporary files are automatically
 * deleted as soon as they are no longer needed. (More precisely, when the corresponding instance of {@link java.io.File} is garbage collected.) This is done by
 * the so-called reaper thread, which is started and stopped automatically by the {@link FileCleaningTracker} when there are files to be tracked. It might make
 * sense to terminate that thread, for example, if your web application ends. See the section on "Resource cleanup" in the users guide of Commons FileUpload.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Builder
 * @see Builder#get()
 * @since 5.0
 */
public final class DiskFileItemFactory implements FileItemFactory {

  /**
   * Builds a new {@link DiskFileItemFactory} instance.
   * <p>
   * For example:
   * </p>
   *
   * <pre>{@code
   * DiskFileItemFactory factory = DiskFileItemFactory.builder().setPath(path).setBufferSize(DEFAULT_THRESHOLD).get();
   * }
   * </pre>
   */
  public static class Builder extends AbstractStreamBuilder<DiskFileItemFactory, Builder> {

    /**
     * The instance of {@link FileCleaningTracker}, which is responsible for deleting temporary files.
     * <p>
     * May be null, if tracking files is not required.
     * </p>
     */
    private FileCleaningTracker fileCleaningTracker;

    /**
     * The threshold. We do maintain this separate from the {@link #getBufferSize()},
     * because the parent class might change the value in {@link #setBufferSize(int)}.
     */
    private int threshold;

    /**
     * Constructs a new instance.
     */
    public Builder() {
      setBufferSize(DEFAULT_THRESHOLD);
      setPath(PathUtils.getTempDirectory());
      setCharset(DiskFileItem.DEFAULT_CHARSET);
      setCharsetDefault(DiskFileItem.DEFAULT_CHARSET);
    }

    /**
     * Constructs a new instance.
     * <p>
     * This builder use the aspects Path and buffer size.
     * </p>
     * <p>
     * You must provide an origin that can be converted to a Reader by this builder, otherwise, this call will throw an
     * {@link UnsupportedOperationException}.
     * </p>
     *
     * @return a new instance.
     * @throws UnsupportedOperationException if the origin cannot provide a Path.
     * @see AbstractOrigin#getReader(Charset)
     */
    @Override
    public DiskFileItemFactory get() {
      return new DiskFileItemFactory(this);
    }

    /**
     * Equivalent to {@link #getThreshold()}.
     *
     * @return The threshold, which is being used.
     * @see #getThreshold()
     * @deprecated Since 2.0.0, use {@link #getThreshold()} instead.
     */
    public int getBufferSize() {
      return getThreshold();
    }

    /**
     * Returns the threshold.
     *
     * @return The threshold.
     */
    public int getThreshold() {
      return threshold;
    }

    /**
     * Equivalent to {@link #setThreshold(int)}.
     *
     * @param bufferSize The threshold, which is being used.
     * @return This builder.
     * @see #setThreshold(int)
     * @deprecated Since 2.0.0, use {@link #setThreshold(int)} instead.
     */
    @Override
    public Builder setBufferSize(final int bufferSize) {
      return setThreshold(bufferSize);
    }

    /**
     * Sets the tracker, which is responsible for deleting temporary files.
     *
     * @param fileCleaningTracker Callback to track files created, or null (default) to disable tracking.
     * @return {@code this} instance.
     */
    public Builder setFileCleaningTracker(final FileCleaningTracker fileCleaningTracker) {
      this.fileCleaningTracker = fileCleaningTracker;
      return this;
    }

    /**
     * Sets the threshold. The uploaded data is typically kept in memory, until
     * a certain number of bytes (the threshold) is reached. At this point, the
     * incoming data is transferred to a temporary file, and the in-memory data
     * is removed.
     *
     * The threshold will also control the <em>state model</em> of the created
     * instances of {@link DiskFileItem}. Details on the state model can be
     * found {@link DiskFileItem here}.
     *
     * @param threshold The threshold, which is being used.
     * @return This builder.
     */
    public Builder setThreshold(final int threshold) {
      this.threshold = threshold;
      return this;
    }
  }

  /**
   * The default threshold in bytes above which uploads will be stored on disk.
   */
  public static final int DEFAULT_THRESHOLD = 10_240;

  /**
   * Constructs a new {@link Builder}.
   *
   * @return a new {@link Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The directory in which uploaded files will be stored, if stored on disk.
   */
  private final Path repository;

  /**
   * The threshold above which uploads will be stored on disk.
   */
  private final int threshold;

  /**
   * The instance of {@link FileCleaningTracker}, which is responsible for deleting temporary files.
   * <p>
   * May be null, if tracking files is not required.
   * </p>
   */
  private final FileCleaningTracker fileCleaningTracker;

  /**
   * Default content Charset to be used when no explicit Charset parameter is provided by the sender.
   */
  private final Charset charsetDefault;

  /**
   * Constructs a preconfigured instance of this class.
   *
   * @param repository The data repository, which is the directory in which files will be created, should the item size exceed the threshold.
   * @param threshold The threshold, in bytes, below which items will be retained in memory and above which they will be stored as a file.
   * @param charsetDefault Sets the default charset for use when no explicit charset parameter is provided by the sender.
   * @param fileCleaningTracker Callback to track files created, or null (default) to disable tracking.
   */
  private DiskFileItemFactory(final Builder builder) {
    this.threshold = builder.threshold;
    this.repository = builder.getPath();
    this.charsetDefault = builder.getCharset();
    this.fileCleaningTracker = builder.fileCleaningTracker;
  }

  @SuppressWarnings("unchecked")
  @Override
  public DiskFileItem.Builder fileItemBuilder() {
    return DiskFileItem.builder()
            .setThreshold(threshold)
            .setCharset(charsetDefault)
            .fileCleaningTracker(fileCleaningTracker)
            .setPath(repository);
  }

  /**
   * Gets the default charset for use when no explicit charset parameter is provided by the sender.
   *
   * @return the default charset
   */
  public Charset getCharsetDefault() {
    return charsetDefault;
  }

  /**
   * Gets the tracker, which is responsible for deleting temporary files.
   *
   * @return An instance of {@link FileCleaningTracker}, or null (default), if temporary files aren't tracked.
   */
  public FileCleaningTracker getFileCleaningTracker() {
    return fileCleaningTracker;
  }

  /**
   * Gets the directory used to temporarily store files that are larger than the configured size threshold.
   *
   * @return The directory in which temporary files will be located.
   */
  public Path getRepository() {
    return repository;
  }

  /**
   * Gets the size threshold beyond which files are written directly to disk. The default value is {@value #DEFAULT_THRESHOLD} bytes.
   *
   * @return The size threshold in bytes.
   */
  public int getThreshold() {
    return threshold;
  }
}
