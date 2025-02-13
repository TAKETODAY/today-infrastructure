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
package infra.mock.api.fileupload.disk;

import java.io.File;

import infra.mock.api.fileupload.FileItem;
import infra.mock.api.fileupload.FileItemFactory;

/**
 * <p>The default {@link FileItemFactory}
 * implementation. This implementation creates
 * {@link FileItem} instances which keep
 * their
 * content either in memory, for smaller items, or in a temporary file on disk,
 * for larger items. The size threshold, above which content will be stored on
 * disk, is configurable, as is the directory in which temporary files will be
 * created.</p>
 *
 * <p>If not otherwise configured, the default configuration values are as
 * follows:</p>
 * <ul>
 *   <li>Size threshold is 10 KiB.</li>
 *   <li>Repository is the system default temp directory, as returned by
 *       {@code System.getProperty("java.io.tmpdir")}.</li>
 * </ul>
 * <p>
 * <b>NOTE</b>: Files are created in the system default temp directory with
 * predictable names. This means that a local attacker with write access to that
 * directory can perform a TOUTOC attack to replace any uploaded file with a
 * file of the attackers choice. The implications of this will depend on how the
 * uploaded file is used but could be significant. When using this
 * implementation in an environment with local, untrusted users,
 * {@link #setRepository(File)} MUST be used to configure a repository location
 * that is not publicly writable. In a Servlet container the location identified
 * by the MockContext attribute {@code infra.mock.api.context.tempdir}
 * may be used.
 * </p>
 *
 * <p>Temporary files, which are created for file items, will be deleted when
 * the associated request is recycled.</p>
 *
 * @since FileUpload 1.1
 */
public class DiskFileItemFactory implements FileItemFactory {

  // ----------------------------------------------------- Manifest constants

  /**
   * The default threshold above which uploads will be stored on disk.
   */
  public static final int DEFAULT_SIZE_THRESHOLD = 10240;

  // ----------------------------------------------------- Instance Variables

  /**
   * The directory in which uploaded files will be stored, if stored on disk.
   */
  private File repository;

  /**
   * The threshold above which uploads will be stored on disk.
   */
  private int sizeThreshold = DEFAULT_SIZE_THRESHOLD;

  /**
   * Default content charset to be used when no explicit charset
   * parameter is provided by the sender.
   */
  private String defaultCharset = DiskFileItem.DEFAULT_CHARSET;

  // ----------------------------------------------------------- Constructors

  /**
   * Constructs an unconfigured instance of this class. The resulting factory
   * may be configured by calling the appropriate setter methods.
   */
  public DiskFileItemFactory() {
    this(DEFAULT_SIZE_THRESHOLD, null);
  }

  /**
   * Constructs a preconfigured instance of this class.
   *
   * @param sizeThreshold The threshold, in bytes, below which items will be
   * retained in memory and above which they will be
   * stored as a file.
   * @param repository The data repository, which is the directory in
   * which files will be created, should the item size
   * exceed the threshold.
   */
  public DiskFileItemFactory(final int sizeThreshold, final File repository) {
    this.sizeThreshold = sizeThreshold;
    this.repository = repository;
  }

  // ------------------------------------------------------------- Properties

  /**
   * Returns the directory used to temporarily store files that are larger
   * than the configured size threshold.
   *
   * @return The directory in which temporary files will be located.
   * @see #setRepository(File)
   */
  public File getRepository() {
    return repository;
  }

  /**
   * Sets the directory used to temporarily store files that are larger
   * than the configured size threshold.
   *
   * @param repository The directory in which temporary files will be located.
   * @see #getRepository()
   */
  public void setRepository(final File repository) {
    this.repository = repository;
  }

  /**
   * Returns the size threshold beyond which files are written directly to
   * disk. The default value is 10240 bytes.
   *
   * @return The size threshold, in bytes.
   * @see #setSizeThreshold(int)
   */
  public int getSizeThreshold() {
    return sizeThreshold;
  }

  /**
   * Sets the size threshold beyond which files are written directly to disk.
   *
   * @param sizeThreshold The size threshold, in bytes.
   * @see #getSizeThreshold()
   */
  public void setSizeThreshold(final int sizeThreshold) {
    this.sizeThreshold = sizeThreshold;
  }

  // --------------------------------------------------------- Public Methods

  /**
   * Create a new {@link DiskFileItem}
   * instance from the supplied parameters and the local factory
   * configuration.
   *
   * @param fieldName The name of the form field.
   * @param contentType The content type of the form field.
   * @param isFormField {@code true} if this is a plain form field;
   * {@code false} otherwise.
   * @param fileName The name of the uploaded file, if any, as supplied
   * by the browser or other client.
   * @return The newly created file item.
   */
  @Override
  public FileItem createItem(final String fieldName, final String contentType,
          final boolean isFormField, final String fileName) {
    final DiskFileItem result = new DiskFileItem(fieldName, contentType,
            isFormField, fileName, sizeThreshold, repository);
    result.setDefaultCharset(defaultCharset);
    return result;
  }

  /**
   * Returns the default charset for use when no explicit charset
   * parameter is provided by the sender.
   *
   * @return the default charset
   */
  public String getDefaultCharset() {
    return defaultCharset;
  }

  /**
   * Sets the default charset for use when no explicit charset
   * parameter is provided by the sender.
   *
   * @param pCharset the default charset
   */
  public void setDefaultCharset(final String pCharset) {
    defaultCharset = pCharset;
  }
}
