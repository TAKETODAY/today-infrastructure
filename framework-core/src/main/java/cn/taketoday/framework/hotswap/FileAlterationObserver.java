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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.Assert;

/**
 * FileAlterationObserver represents the state of files below a root directory,
 * checking the file system and notifying listeners of create, change or
 * delete events.
 * <p>
 * To use this implementation:
 * <ul>
 *   <li>Create {@link FileAlterationListener} implementation(s) that process
 *      the file/directory create, change and delete events</li>
 *   <li>Register the listener(s) with a {@link FileAlterationObserver} for
 *       the appropriate directory.</li>
 *   <li>Either register the observer(s) with a {@link FileAlterationMonitor} or
 *       run manually.</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * Create a {@link FileAlterationObserver} for the directory and register the listeners:
 * <pre>
 *      File directory = new File(new File("."), "src");
 *      FileAlterationObserver observer = new FileAlterationObserver(directory);
 *      observer.addListener(...);
 *      observer.addListener(...);
 * </pre>
 * To manually observe a directory, initialize the observer and invoked the
 * {@link #checkAndNotify()} method as required:
 * <pre>
 *      // initialize
 *      observer.init();
 *      ...
 *      // invoke as required
 *      observer.checkAndNotify();
 *      ...
 *      observer.checkAndNotify();
 *      ...
 *      // finished
 *      observer.finish();
 * </pre>
 * Alternatively, register the observer(s) with a {@link FileAlterationMonitor},
 * which creates a new thread, invoking the observer at the specified interval:
 * <pre>
 *      long interval = ...
 *      FileAlterationMonitor monitor = new FileAlterationMonitor(interval);
 *      monitor.addObserver(observer);
 *      monitor.start();
 *      ...
 *      monitor.stop();
 * </pre>
 *
 * <h2>File Filters</h2>
 * This implementation can monitor portions of the file system
 * by using {@link FileFilter}s to observe only the files and/or directories
 * that are of interest. This makes it more efficient and reduces the
 * noise from <i>unwanted</i> file system events.
 * <p>
 * <a href="https://commons.apache.org/io/">Commons IO</a> has a good range of
 * useful, ready made
 * <a href="../filefilter/package-summary.html">File Filter</a>
 * implementations for this purpose.
 * <p>
 * For example, to only observe 1) visible directories and 2) files with a ".java" suffix
 * in a root directory called "src" you could set up a {@link FileAlterationObserver} in the following
 * way:
 * <pre>
 *      // Create a FileFilter
 *      IOFileFilter directories = FileFilterUtils.and(
 *                                      FileFilterUtils.directoryFileFilter(),
 *                                      HiddenFileFilter.VISIBLE);
 *      IOFileFilter files       = FileFilterUtils.and(
 *                                      FileFilterUtils.fileFileFilter(),
 *                                      FileFilterUtils.suffixFileFilter(".java"));
 *      IOFileFilter filter = FileFilterUtils.or(directories, files);
 *
 *      // Create the File system observer and register File Listeners
 *      FileAlterationObserver observer = new FileAlterationObserver(new File("src"), filter);
 *      observer.addListener(...);
 *      observer.addListener(...);
 * </pre>
 *
 * <h2>FileEntry</h2>
 * {@link FileEntry} represents the state of a file or directory, capturing
 * {@link File} attributes at a point in time. Custom implementations of
 * {@link FileEntry} can be used to capture additional properties that the
 * basic implementation does not support. The {@link FileEntry#refresh(File)}
 * method is used to determine if a file or directory has changed since the last
 * check and stores the current state of the {@link File}'s properties.
 *
 * @author TODAY 2021/2/18 11:44
 * @see FileAlterationListener
 * @see FileAlterationMonitor
 */
public class FileAlterationObserver implements Serializable, Comparator<File> {
  private static final long serialVersionUID = 1L;

  private final FileEntry rootEntry;
  private final FileFilter fileFilter;
  private final boolean caseSensitive;
  private final List<FileAlterationListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Constructs an observer for the specified directory.
   *
   * @param directoryName
   *         the name of the directory to observe
   */
  public FileAlterationObserver(final String directoryName) {
    this(new File(directoryName));
  }

  /**
   * Constructs an observer for the specified directory and file filter.
   *
   * @param directoryName
   *         the name of the directory to observe
   * @param fileFilter
   *         The file filter or null if none
   */
  public FileAlterationObserver(final String directoryName, final FileFilter fileFilter) {
    this(new File(directoryName), fileFilter);
  }

  /**
   * Construct an observer for the specified directory, file filter and
   * file comparator.
   *
   * @param directoryName
   *         the name of the directory to observe
   * @param fileFilter
   *         The file filter or null if none
   * @param caseSensitivity
   *         what case sensitivity to use comparing file names, null means system sensitive
   */
  public FileAlterationObserver(final String directoryName, final FileFilter fileFilter,
                                final boolean caseSensitivity) {
    this(new File(directoryName), fileFilter, caseSensitivity);
  }

  /**
   * Constructs an observer for the specified directory.
   *
   * @param directory
   *         the directory to observe
   */
  public FileAlterationObserver(final File directory) {
    this(directory, null);
  }

  /**
   * Constructs an observer for the specified directory and file filter.
   *
   * @param directory
   *         the directory to observe
   * @param fileFilter
   *         The file filter or null if none
   */
  public FileAlterationObserver(final File directory, final FileFilter fileFilter) {
    this(directory, fileFilter, '\\' != File.separatorChar);
  }

  /**
   * Constructs an observer for the specified directory, file filter and
   * file comparator.
   *
   * @param directory
   *         the directory to observe
   * @param fileFilter
   *         The file filter or null if none
   * @param caseSensitivity
   *         what case sensitivity to use comparing file names, null means system sensitive
   */
  public FileAlterationObserver(final File directory, final FileFilter fileFilter, final boolean caseSensitivity) {
    this(new FileEntry(directory), fileFilter, caseSensitivity);
  }

  /**
   * Constructs an observer for the specified directory, file filter and
   * file comparator.
   *
   * @param rootEntry
   *         the root directory to observe
   * @param fileFilter
   *         The file filter or null if none
   * @param caseSensitivity
   *         what case sensitivity to use comparing file names, null means system sensitive
   */
  protected FileAlterationObserver(final FileEntry rootEntry, final FileFilter fileFilter,
                                   final boolean caseSensitivity) {

    Assert.notNull(rootEntry, "Root entry is missing");
    Assert.notNull(rootEntry.getFile(), "Root directory is missing");

    this.rootEntry = rootEntry;
    this.fileFilter = fileFilter;
    this.caseSensitive = caseSensitivity;
  }

  /**
   * Returns the directory being observed.
   *
   * @return the directory being observed
   */
  public File getDirectory() {
    return rootEntry.getFile();
  }

  /**
   * Returns the fileFilter.
   *
   * @return the fileFilter
   *
   * @since 2.1
   */
  public FileFilter getFileFilter() {
    return fileFilter;
  }

  /**
   * Adds a file system listener.
   *
   * @param listener
   *         The file system listener
   */
  public void addListener(final FileAlterationListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Removes a file system listener.
   *
   * @param listener
   *         The file system listener
   */
  public void removeListener(final FileAlterationListener listener) {
    if (listener != null) {
      while (listeners.remove(listener)) {
        // empty
      }
    }
  }

  /**
   * Returns the set of registered file system listeners.
   *
   * @return The file system listeners
   */
  public Iterable<FileAlterationListener> getListeners() {
    return listeners;
  }

  /**
   * Initializes the observer.
   *
   * @throws Exception
   *         if an error occurs
   */
  public void initialize() throws Exception {
    rootEntry.refresh(rootEntry.getFile());
    final FileEntry[] children = doListFiles(rootEntry.getFile(), rootEntry);
    rootEntry.setChildren(children);
  }

  /**
   * Final processing.
   *
   * @throws Exception
   *         if an error occurs
   */
  public void destroy() throws Exception {
    // noop
  }

  /**
   * Checks whether the file and its children have been created, modified or deleted.
   */
  public void checkAndNotify() {

    /* fire onStart() */
    for (final FileAlterationListener listener : listeners) {
      listener.onStart(this);
    }

    /* fire directory/file events */
    final FileEntry rootEntry = this.rootEntry;
    final File rootFile = rootEntry.getFile();
    if (rootFile.exists()) {
      checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile));
    }
    else if (rootEntry.isExists()) {
      checkAndNotify(rootEntry, rootEntry.getChildren(), Constant.EMPTY_FILE_ARRAY);
    }
//    else {
    // Didn't exist and still doesn't
//    }

    /* fire onStop() */
    for (final FileAlterationListener listener : listeners) {
      listener.onStop(this);
    }
  }

  /**
   * Compares two file lists for files which have been created, modified or deleted.
   *
   * @param parent
   *         The parent entry
   * @param previous
   *         The original list of files
   * @param files
   *         The current list of files
   */
  private void checkAndNotify(final FileEntry parent, final FileEntry[] previous, final File[] files) {
    int c = 0;
    final int fileLength = files.length;
    final FileEntry[] current = fileLength > 0 ? new FileEntry[fileLength] : FileEntry.EMPTY_ENTRIES;
    for (final FileEntry entry : previous) {
      final File entryFile = entry.getFile();
      while (c < fileLength && compare(entryFile, files[c]) > 0) {
        current[c] = createFileEntry(parent, files[c]);
        doCreate(current[c]);
        c++;
      }
      if (c < fileLength && compare(entryFile, files[c]) == 0) {
        doMatch(entry, files[c]);
        checkAndNotify(entry, entry.getChildren(), listFiles(files[c]));
        current[c] = entry;
        c++;
      }
      else {
        checkAndNotify(entry, entry.getChildren(), FileUtils.EMPTY_FILE_ARRAY);
        doDelete(entry);
      }
    }
    for (; c < fileLength; c++) {
      current[c] = createFileEntry(parent, files[c]);
      doCreate(current[c]);
    }
    parent.setChildren(current);
  }

  @Override
  public int compare(File entryFile, File file) {
    final String name1 = entryFile.getName();
    final String name2 = file.getName();
    return caseSensitive ? name1.compareTo(name2) : name1.compareToIgnoreCase(name2);
  }

  /**
   * Creates a new file entry for the specified file.
   *
   * @param parent
   *         The parent file entry
   * @param file
   *         The file to create an entry for
   *
   * @return A new file entry
   */
  private FileEntry createFileEntry(final FileEntry parent, final File file) {
    final FileEntry entry = parent.newChildInstance(file);
    entry.refresh(file);
    final FileEntry[] children = doListFiles(file, entry);
    entry.setChildren(children);
    return entry;
  }

  /**
   * Lists the files
   *
   * @param file
   *         The file to list files for
   * @param entry
   *         the parent entry
   *
   * @return The child files
   */
  private FileEntry[] doListFiles(final File file, final FileEntry entry) {
    final File[] files = listFiles(file);
    final int length = files.length;
    final FileEntry[] children = length > 0 ? new FileEntry[length] : FileEntry.EMPTY_ENTRIES;
    for (int i = 0; i < length; i++) {
      children[i] = createFileEntry(entry, files[i]);
    }
    return children;
  }

  /**
   * Fires directory/file created events to the registered listeners.
   *
   * @param entry
   *         The file entry
   */
  private void doCreate(final FileEntry entry) {
    for (final FileAlterationListener listener : listeners) {
      if (entry.isDirectory()) {
        listener.onDirectoryCreate(entry.getFile());
      }
      else {
        listener.onFileCreate(entry.getFile());
      }
    }
    for (final FileEntry aChildren : entry.getChildren()) {
      doCreate(aChildren);
    }
  }

  /**
   * Fires directory/file change events to the registered listeners.
   *
   * @param entry
   *         The previous file system entry
   * @param file
   *         The current file
   */
  private void doMatch(final FileEntry entry, final File file) {
    if (entry.refresh(file)) {
      for (final FileAlterationListener listener : listeners) {
        if (entry.isDirectory()) {
          listener.onDirectoryChange(file);
        }
        else {
          listener.onFileChange(file);
        }
      }
    }
  }

  /**
   * Fires directory/file delete events to the registered listeners.
   *
   * @param entry
   *         The file entry
   */
  private void doDelete(final FileEntry entry) {
    for (final FileAlterationListener listener : listeners) {
      if (entry.isDirectory()) {
        listener.onDirectoryDelete(entry.getFile());
      }
      else {
        listener.onFileDelete(entry.getFile());
      }
    }
  }

  /**
   * Lists the contents of a directory
   *
   * @param file
   *         The file to list the contents of
   *
   * @return the directory contents or a zero length array if
   * the empty or the file is not a directory
   */
  private File[] listFiles(final File file) {
    File[] children = null;
    if (file.isDirectory()) {
      children = fileFilter == null ? file.listFiles() : file.listFiles(fileFilter);
    }
    if (children == null) {
      children = FileUtils.EMPTY_FILE_ARRAY;
    }
    if (children.length > 1) {
      Arrays.sort(children, this);
    }
    return children;
  }

  /**
   * Returns a String representation of this observer.
   *
   * @return a String representation of this observer
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("[file='");
    builder.append(getDirectory().getPath());
    builder.append('\'');
    if (fileFilter != null) {
      builder.append(", ");
      builder.append(fileFilter.toString());
    }
    builder.append(", listeners=");
    builder.append(listeners.size());
    builder.append("]");
    return builder.toString();
  }

}
