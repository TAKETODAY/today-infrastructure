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
package infra.mock.api.fileupload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * General file manipulation utilities.
 * <p>
 * Facilities are provided in the following areas:
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>listing files and directories by filter and extension
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 * Note that a specific charset should be specified whenever possible.
 * Relying on the platform default means that the code is Locale-dependent.
 * Only use the default if the files are known to always use the platform default.
 * <p>
 * Origin of code: Excalibur, Alexandria, Commons-Utils
 */
public class FileUtils {

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public FileUtils() {
    super();
  }

  //-----------------------------------------------------------------------

  /**
   * Deletes a directory recursively.
   *
   * @param directory directory to delete
   * @throws IOException in case deletion is unsuccessful
   * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
   */
  public static void deleteDirectory(final File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    if (!isSymlink(directory)) {
      cleanDirectory(directory);
    }

    if (!directory.delete()) {
      final String message =
              "Unable to delete directory " + directory + ".";
      throw new IOException(message);
    }
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory directory to clean
   * @throws IOException in case cleaning is unsuccessful
   * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
   */
  public static void cleanDirectory(final File directory) throws IOException {
    if (!directory.exists()) {
      final String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      final String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    final File[] files = directory.listFiles();
    if (files == null) {  // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (File file : files) {
      try {
        forceDelete(file);
      }
      catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  //-----------------------------------------------------------------------

  /**
   * Deletes a file. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>You get exceptions when a file or directory cannot be deleted.
   * (java.io.File methods returns a boolean)</li>
   * </ul>
   *
   * @param file file or directory to delete, must not be {@code null}
   * @throws NullPointerException if the directory is {@code null}
   * @throws FileNotFoundException if the file was not found
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDelete(final File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    }
    else {
      final boolean filePresent = file.exists();
      if (!file.delete()) {
        if (!filePresent) {
          throw new FileNotFoundException("File does not exist: " + file);
        }
        final String message =
                "Unable to delete file: " + file;
        throw new IOException(message);
      }
    }
  }

  /**
   * Schedules a file to be deleted when JVM exits.
   * If file is directory delete it and all sub-directories.
   *
   * @param file file or directory to delete, must not be {@code null}
   * @throws NullPointerException if the file is {@code null}
   * @throws IOException in case deletion is unsuccessful
   */
  public static void forceDeleteOnExit(final File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectoryOnExit(file);
    }
    else {
      file.deleteOnExit();
    }
  }

  /**
   * Schedules a directory recursively for deletion on JVM exit.
   *
   * @param directory directory to delete, must not be {@code null}
   * @throws NullPointerException if the directory is {@code null}
   * @throws IOException in case deletion is unsuccessful
   */
  private static void deleteDirectoryOnExit(final File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    directory.deleteOnExit();
    if (!isSymlink(directory)) {
      cleanDirectoryOnExit(directory);
    }
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory directory to clean, must not be {@code null}
   * @throws NullPointerException if the directory is {@code null}
   * @throws IOException in case cleaning is unsuccessful
   */
  private static void cleanDirectoryOnExit(final File directory) throws IOException {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    File[] files = directory.listFiles();
    if (files == null) {  // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (File file : files) {
      try {
        forceDeleteOnExit(file);
      }
      catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  /**
   * Makes a directory, including any necessary but nonexistent parent
   * directories. If a file already exists with specified name but it is
   * not a directory then an IOException is thrown.
   * If the directory cannot be created (or does not already exist)
   * then an IOException is thrown.
   *
   * @param directory directory to create, must not be {@code null}
   * @throws NullPointerException if the directory is {@code null}
   * @throws IOException if the directory cannot be created or the file already exists but is not a directory
   */
  public static void forceMkdir(final File directory) throws IOException {
    if (directory.exists()) {
      if (!directory.isDirectory()) {
        final String message =
                "File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.";
        throw new IOException(message);
      }
    }
    else {
      if (!directory.mkdirs()) {
        // Double-check that some other thread or process hasn't made
        // the directory in the background
        if (!directory.isDirectory()) {
          final String message =
                  "Unable to create directory " + directory;
          throw new IOException(message);
        }
      }
    }
  }

  /**
   * Makes any necessary but nonexistent parent directories for a given File. If the parent directory cannot be
   * created then an IOException is thrown.
   *
   * @param file file with parent to create, must not be {@code null}
   * @throws NullPointerException if the file is {@code null}
   * @throws IOException if the parent directory cannot be created
   * @since IO 2.5
   */
  public static void forceMkdirParent(final File file) throws IOException {
    final File parent = file.getParentFile();
    if (parent == null) {
      return;
    }
    forceMkdir(parent);
  }

  /**
   * Determines whether the specified file is a Symbolic Link rather than an actual file.
   * <p>
   * Will not return true if there is a Symbolic Link anywhere in the path,
   * only if the specific file is.
   * <p>
   * <b>Note:</b> the current implementation always returns {@code false} if
   * the system is detected as Windows using
   * {@link File#separatorChar} == '\\'
   *
   * @param file the file to check
   * @return true if the file is a Symbolic Link
   * @throws IOException if an IO error occurs while checking the file
   * @since IO 2.0
   */
  public static boolean isSymlink(File file) throws IOException {
    if (file == null) {
      throw new NullPointerException("File is required");
    }
    //FilenameUtils.isSystemWindows()
    if (File.separatorChar == '\\') {
      return false;
    }
    File fileInCanonicalDir = null;
    if (file.getParent() == null) {
      fileInCanonicalDir = file;
    }
    else {
      File canonicalDir = file.getParentFile().getCanonicalFile();
      fileInCanonicalDir = new File(canonicalDir, file.getName());
    }

    if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
      return false;
    }
    else {
      return true;
    }
  }
}
