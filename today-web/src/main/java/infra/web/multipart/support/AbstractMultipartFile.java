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

package infra.web.multipart.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import infra.web.multipart.MultipartFile;

/**
 * Abstract base class for multipart file implementations. Provides a common structure
 * and behavior for handling multipart files, including caching, transferring, and cleanup.
 * Subclasses must implement abstract methods to handle specific file operations.
 *
 * <p>This class is designed to be extended by concrete implementations that provide
 * the actual logic for interacting with multipart files. It implements the {@link MultipartFile}
 * interface and extends {@link AbstractPart}, offering a foundation for file handling
 * in web applications or frameworks.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Caching of file content to improve performance.</li>
 *   <li>Support for transferring files to a target location.</li>
 *   <li>Cleanup of resources after processing.</li>
 *   <li>Abstract methods for file-specific operations, allowing flexibility for different implementations.</li>
 * </ul>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>The {@link #transferTo(File)} method ensures that the parent directory of the destination
 *       file exists before attempting to save the file.</li>
 *   <li>The {@link #cleanup()} method clears cached bytes and invokes the abstract {@link #deleteInternal()}
 *       method to release resources.</li>
 *   <li>Subclasses should ensure thread safety if the instance is accessed concurrently.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MultipartFile
 * @see AbstractPart
 * @since 3.0 2021/4/18 20:38
 */
public abstract class AbstractMultipartFile extends AbstractPart implements MultipartFile {

  @Override
  public void transferTo(File dest) throws IOException {
    // fix #3 Upload file not found exception
    File parentFile = dest.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    /*
     * The uploaded file is being stored on disk
     * in a temporary location so move it to the
     * desired file.
     */
    if (dest.exists()) {
      Files.delete(dest.toPath());
    }
    saveInternal(dest);
  }

  /**
   * Saves the internal representation of this multipart file to the specified destination.
   * This method is intended to be implemented by subclasses to provide specific
   * behavior for saving the file data to a given location. It may involve writing
   * cached bytes or transferring data from a temporary storage to the target file.
   *
   * <p>This method is typically invoked by higher-level operations such as
   * {@link #transferTo(File)} to handle the actual file-saving logic.
   *
   * @param dest the target file where the internal data should be saved. Must not be null.
   * @throws IOException if an I/O error occurs during the save operation, such as issues
   * with writing to the file or accessing the internal data.
   */
  protected abstract void saveInternal(File dest) throws IOException;

  @Override
  public final boolean isFormField() {
    return false;
  }

  @Override
  public final void cleanup() throws IOException {
    cachedBytes = null;
    deleteInternal();
  }

  protected abstract void deleteInternal() throws IOException;

  @Override
  public String toString() {
    return "%s: '%s'".formatted(getClass().getSimpleName(), getName());
  }

}
