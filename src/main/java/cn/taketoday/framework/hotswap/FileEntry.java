/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.framework.hotswap;

import java.io.File;
import java.io.Serializable;

import cn.taketoday.context.utils.Assert;

/**
 * The state of a file or directory, capturing the following {@link File} attributes at a point in time.
 * <ul>
 *   <li>File Name (see {@link File#getName()})</li>
 *   <li>Exists - whether the file exists or not (see {@link File#exists()})</li>
 *   <li>Directory - whether the file is a directory or not (see {@link File#isDirectory()})</li>
 *   <li>Last Modified Date/Time (see {@link File#lastModified()})</li>
 *   <li>Length (see {@link File#length()}) - directories treated as zero</li>
 *   <li>Children - contents of a directory (see {@link File#listFiles(java.io.FileFilter)})</li>
 * </ul>
 *
 * <h2>Custom Implementations</h2>
 * <p>
 * If the state of additional {@link File} attributes is required then create a custom
 * {@link FileEntry} with properties for those attributes. Override the
 * {@link #newChildInstance(File)} to return a new instance of the appropriate type.
 * You may also want to override the {@link #refresh(File)} method.
 * </p>
 *
 * @author TODAY 2021/2/18 11:47
 * @see FileAlterationObserver
 */
public class FileEntry implements Serializable {
  private static final long serialVersionUID = 1L;

  static final FileEntry[] EMPTY_ENTRIES = new FileEntry[0];

  private final FileEntry parent;
  private FileEntry[] children;
  private final File file;
  private String name;
  private boolean exists;
  private boolean directory;
  private long lastModified;
  private long length;

  /**
   * Construct a new monitor for a specified {@link File}.
   *
   * @param file
   *         The file being monitored
   */
  public FileEntry(final File file) {
    this(null, file);
  }

  /**
   * Construct a new monitor for a specified {@link File}.
   *
   * @param parent
   *         The parent
   * @param file
   *         The file being monitored
   */
  public FileEntry(final FileEntry parent, final File file) {
    Assert.notNull(file, "File is missing");
    this.file = file;
    this.parent = parent;
    this.name = file.getName();
  }

  /**
   * Refresh the attributes from the {@link File}, indicating
   * whether the file has changed.
   * <p>
   * This implementation refreshes the <code>name</code>, <code>exists</code>,
   * <code>directory</code>, <code>lastModified</code> and <code>length</code>
   * properties.
   * <p>
   * The <code>exists</code>, <code>directory</code>, <code>lastModified</code>
   * and <code>length</code> properties are compared for changes
   *
   * @param file
   *         the file instance to compare to
   *
   * @return {@code true} if the file has changed, otherwise {@code false}
   */
  public boolean refresh(final File file) {

    // cache original values
    final boolean origExists = exists;
    final long origLastModified = lastModified;
    final boolean origDirectory = directory;
    final long origLength = length;

    // refresh the values
    name = file.getName();
    exists = file.exists();
    directory = exists && file.isDirectory();
    lastModified = exists ? file.lastModified() : 0;
    length = exists && !directory ? file.length() : 0;

    // Return if there are changes
    return exists != origExists ||
            lastModified != origLastModified ||
            directory != origDirectory ||
            length != origLength;
  }

  /**
   * Create a new child instance.
   * <p>
   * Custom implementations should override this method to return
   * a new instance of the appropriate type.
   *
   * @param file
   *         The child file
   *
   * @return a new child instance
   */
  public FileEntry newChildInstance(final File file) {
    return new FileEntry(this, file);
  }

  /**
   * Return the parent entry.
   *
   * @return the parent entry
   */
  public FileEntry getParent() {
    return parent;
  }

  /**
   * Return the level
   *
   * @return the level
   */
  public int getLevel() {
    return parent == null ? 0 : parent.getLevel() + 1;
  }

  /**
   * Return the directory's files.
   *
   * @return This directory's files or an empty
   * array if the file is not a directory or the
   * directory is empty
   */
  public FileEntry[] getChildren() {
    return children != null ? children : EMPTY_ENTRIES;
  }

  /**
   * Set the directory's files.
   *
   * @param children
   *         This directory's files, may be null
   */
  public void setChildren(final FileEntry... children) {
    this.children = children;
  }

  /**
   * Return the file being monitored.
   *
   * @return the file being monitored
   */
  public File getFile() {
    return file;
  }

  /**
   * Return the file name.
   *
   * @return the file name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the file name.
   *
   * @param name
   *         the file name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Return the last modified time from the last time it
   * was checked.
   *
   * @return the last modified time
   */
  public long getLastModified() {
    return lastModified;
  }

  /**
   * Return the last modified time from the last time it
   * was checked.
   *
   * @param lastModified
   *         The last modified time
   */
  public void setLastModified(final long lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Return the length.
   *
   * @return the length
   */
  public long getLength() {
    return length;
  }

  /**
   * Set the length.
   *
   * @param length
   *         the length
   */
  public void setLength(final long length) {
    this.length = length;
  }

  /**
   * Indicate whether the file existed the last time it
   * was checked.
   *
   * @return whether the file existed
   */
  public boolean isExists() {
    return exists;
  }

  /**
   * Set whether the file existed the last time it
   * was checked.
   *
   * @param exists
   *         whether the file exists or not
   */
  public void setExists(final boolean exists) {
    this.exists = exists;
  }

  /**
   * Indicate whether the file is a directory or not.
   *
   * @return whether the file is a directory or not
   */
  public boolean isDirectory() {
    return directory;
  }

  /**
   * Set whether the file is a directory or not.
   *
   * @param directory
   *         whether the file is a directory or not
   */
  public void setDirectory(final boolean directory) {
    this.directory = directory;
  }
}
