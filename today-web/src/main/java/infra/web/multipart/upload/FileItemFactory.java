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
import org.apache.commons.io.build.AbstractStreamBuilder;
import org.apache.commons.io.file.PathUtils;
import org.jspecify.annotations.Nullable;

import infra.http.HttpHeaders;

/**
 * Creates {@link FileItem} instances.
 * <p>
 * Factories can provide their own custom configuration, over and above that provided by the default file upload implementation.
 * </p>
 *
 * @param <I> The {@link FileItem} type this factory creates.
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface FileItemFactory<I extends FileItem<I>> {

  /**
   * Abstracts building for subclasses.
   *
   * @param <I> the type of {@link FileItem} to build.
   * @param <B> the type of builder subclass.
   */
  abstract class AbstractFileItemBuilder<I extends FileItem<I>, B extends AbstractFileItemBuilder<I, B>> extends AbstractStreamBuilder<I, B> {

    /**
     * Field name.
     */
    private String fieldName;

    /**
     * Content type.
     */
    private String contentType;

    /**
     * Is this a form field.
     */
    private boolean isFormField;

    /**
     * File name.
     */
    private @Nullable String fileName;

    /**
     * File item headers.
     */
    private HttpHeaders itemHeaders = HttpHeaders.empty();

    /**
     * The instance of {@link FileCleaningTracker}, which is responsible for deleting temporary files.
     * <p>
     * May be null, if tracking files is not required.
     * </p>
     */
    private FileCleaningTracker fileCleaningTracker;

    /**
     * Constructs a new instance.
     */
    public AbstractFileItemBuilder() {
      setBufferSize(DiskFileItemFactory.DEFAULT_THRESHOLD);
      setPath(PathUtils.getTempDirectory());
    }

    /**
     * Gets the content type.
     *
     * @return the content type.
     */
    public String getContentType() {
      return contentType;
    }

    /**
     * Gets the field name.
     *
     * @return the field name.
     */
    public String getFieldName() {
      return fieldName;
    }

    /**
     * Gets the file cleaning tracker.
     *
     * @return the file cleaning tracker.
     */
    public FileCleaningTracker getFileCleaningTracker() {
      return fileCleaningTracker;
    }

    /**
     * Gets the field item headers.
     *
     * @return the field item headers.
     */
    public HttpHeaders getItemHeaders() {
      return itemHeaders;
    }

    /**
     * Gets the file name.
     *
     * @return the file name.
     */
    public String getFileName() {
      return fileName;
    }

    /**
     * Tests whether this is a form field.
     *
     * @return whether this is a form field.
     */
    public boolean isFormField() {
      return isFormField;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the content type.
     * @return {@code this} instance.
     */
    public B contentType(final String contentType) {
      this.contentType = contentType;
      return asThis();
    }

    /**
     * Sets the field name.
     *
     * @param fieldName the field name.
     * @return {@code this} instance.
     */
    public B fieldName(final String fieldName) {
      this.fieldName = fieldName;
      return asThis();
    }

    /**
     * Sets the file cleaning tracker.
     *
     * @param fileCleaningTracker the file cleaning tracker.
     * @return {@code this} instance.
     */
    public B fileCleaningTracker(final FileCleaningTracker fileCleaningTracker) {
      this.fileCleaningTracker = fileCleaningTracker;
      return asThis();
    }

    /**
     * Sets the file item headers.
     *
     * @param fileItemHeaders the item headers.
     * @return {@code this} instance.
     */
    public B fileItemHeaders(final @Nullable HttpHeaders fileItemHeaders) {
      this.itemHeaders = fileItemHeaders != null ? fileItemHeaders : HttpHeaders.empty();
      return asThis();
    }

    /**
     * Sets the file name.
     *
     * @param fileName the file name.
     * @return {@code this} instance.
     */
    public B fileName(final @Nullable String fileName) {
      this.fileName = fileName;
      return asThis();
    }

    /**
     * Sets whether this is a form field.
     *
     * @param isFormField whether this is a form field.
     * @return {@code this} instance.
     */
    public B formField(final boolean isFormField) {
      this.isFormField = isFormField;
      return asThis();
    }

  }

  /**
   * Creates a new AbstractFileItemBuilder.
   *
   * @param <B> The type of AbstractFileItemBuilder.
   * @return a new AbstractFileItemBuilder.
   */
  <B extends AbstractFileItemBuilder<I, B>> AbstractFileItemBuilder<I, B> fileItemBuilder();

}
