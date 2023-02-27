/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import cn.taketoday.core.ConfigurableObjectInputStream;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/27 21:43
 */
public class FileSessionPersister implements SessionPersister {
  private static final Logger log = LoggerFactory.getLogger(FileSessionPersister.class);

  /**
   * The extension to use for serialized session filenames.
   */
  private static final String FILE_EXT = ".session";

  /**
   * The pathname of the directory in which Sessions are stored.
   * This may be an absolute pathname, or a relative path that is
   * resolved against the temporary work directory for this application.
   */
  @Nullable
  private String directory = ".";

  /**
   * A File representing the directory in which Sessions are stored.
   */
  @Nullable
  private File directoryFile = null;

  private File tempDirectory = initDefaultTemp();

  private File initDefaultTemp() {
    try {
      return Files.createTempDirectory("sessions").toFile();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final SessionRepository repository;

  /**
   * Name to register for this Store, used for logging.
   */
  private static final String storeName = "fileStore";

  /**
   * Name to register for the background thread.
   */
  private static final String threadName = "FileStore";

  public FileSessionPersister(SessionRepository repository) {
    Assert.notNull(repository, "SessionRepository is required");
    this.repository = repository;
  }

  public void setTempDirectory(@Nullable File tempDirectory) {
    this.tempDirectory = tempDirectory == null ? initDefaultTemp() : tempDirectory;
  }

  /**
   * Remove the Session with the specified session identifier from
   * this Store, if present.  If no such Session is present, this method
   * takes no action.
   *
   * @param id Session identifier of the Session to be removed
   */
  @Override
  public void remove(String id) throws IOException {
    File file = file(id);
    if (file == null) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("Removing Session [{}] at file [{}]", id, file.getAbsolutePath());
    }

    if (file.exists() && !file.delete()) {
      throw new IOException("Unable to delete file [" + file + "] which is no longer required");
    }
  }

  /**
   * Remove all of the Sessions in this Store.
   *
   * @throws IOException if an input/output error occurs
   */
  @Override
  public void clear() throws IOException {
    String[] keys = keys();
    for (String key : keys) {
      remove(key);
    }
  }

  /**
   * Return an array containing the session identifiers of all Sessions
   * currently saved in this Store.  If there are no such Sessions, a
   * zero-length array is returned.
   *
   * @throws IOException if an input/output error occurred
   */
  @Override
  public String[] keys() throws IOException {
    // Acquire the list of files in our storage directory
    File dir = directory();
    if (dir == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }

    String[] files = dir.list();
    if (ObjectUtils.isEmpty(files)) {
      return Constant.EMPTY_STRING_ARRAY;
    }

    // Build and return the list of session identifiers
    ArrayList<String> list = new ArrayList<>();
    int n = FILE_EXT.length();
    for (String file : files) {
      if (file.endsWith(FILE_EXT)) {
        list.add(file.substring(0, file.length() - n));
      }
    }
    return StringUtils.toStringArray(list);
  }

  /**
   * Load and return the WebSession associated with the specified session
   * identifier from this Store, without removing it.  If there is no
   * such stored WebSession, return <code>null</code>.
   *
   * @param id Session identifier of the session to load
   * @throws ClassNotFoundException if a deserialization error occurs
   * @throws IOException if an input/output error occurs
   */
  @Override
  public WebSession load(String id) throws ClassNotFoundException, IOException {
    // Open an input stream to the specified pathname, if any
    File file = file(id);
    if (file == null || !file.exists()) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Loading Session [{}] from file [{}]", id, file.getAbsolutePath());
    }

    try (FileInputStream fis = new FileInputStream(file.getAbsolutePath());
            ObjectInputStream ois = getObjectInputStream(fis)) {
      WebSession session = repository.createSession(id);
      if (session instanceof SerializableSession serialized) {
        serialized.readObjectData(ois);
      }
      else if (ois.readObject() instanceof WebSession ret) {
        return new MapSession(ret);
      }
      return session;
    }
    catch (FileNotFoundException e) {
      log.debug("No persisted data file found");
      return null;
    }
  }

  /**
   * Save the specified Session into this Store.  Any previously saved
   * information for the associated session identifier is replaced.
   *
   * @param session Session to be saved
   * @throws IOException if an input/output error occurs
   */
  @Override
  public void save(WebSession session) throws IOException {
    // Open an output stream to the specified pathname, if any
    File file = file(session.getId());
    if (file == null) {
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("Saving Session [{}] to file [{}]", session.getId(), file.getAbsolutePath());
    }

    try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos))) {
      if (session instanceof SerializableSession serialized) {
        serialized.writeObjectData(oos);
      }
      else {
        oos.writeObject(session);
      }
    }
  }

  /**
   * Create the object input stream to use to read a session from the store.
   * Sub-classes <b>must</b> have set the thread context class loader before
   * calling this method.
   *
   * @param is The input stream provided by the sub-class that will provide
   * the data for a session
   * @return An appropriately configured ObjectInputStream from which the
   * session can be read.
   * @throws IOException if a problem occurs creating the ObjectInputStream
   */
  protected ObjectInputStream getObjectInputStream(InputStream is) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(is);
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    return new ConfigurableObjectInputStream(bis, classLoader);
  }

  /**
   * Return a File object representing the pathname to our
   * session persistence directory, if any.  The directory will be
   * created if it does not already exist.
   */
  @Nullable
  private File directory() throws IOException {
    if (this.directory == null) {
      return null;
    }
    if (this.directoryFile != null) {
      // NOTE:  Race condition is harmless, so do not synchronize
      return this.directoryFile;
    }
    File file = new File(this.directory);
    if (!file.isAbsolute()) {
      file = new File(tempDirectory, this.directory);
    }
    if (!file.exists() || !file.isDirectory()) {
      if (!file.delete() && file.exists()) {
        throw new IOException("Unable to delete file [" +
                file + "] which is preventing the creation of the session storage location");
      }
      if (!file.mkdirs() && !file.isDirectory()) {
        throw new IOException("Unable to create directory [" +
                file + "] for the storage of session data");
      }
    }
    this.directoryFile = file;
    return file;
  }

  /**
   * Return a File object representing the pathname to our
   * session persistence file, if any.
   *
   * @param id The ID of the Session to be retrieved. This is
   * used in the file naming.
   */
  @Nullable
  private File file(String id) throws IOException {
    File storageDir = directory();
    if (storageDir == null) {
      return null;
    }

    String filename = id + FILE_EXT;
    File file = new File(storageDir, filename);
    File canonicalFile = file.getCanonicalFile();

    // Check the file is within the storage directory
    if (!canonicalFile.toPath().startsWith(storageDir.getCanonicalFile().toPath())) {
      log.warn("Invalid persistence file [{}] for session ID [{}]", file.getPath(), id);
      return null;
    }

    return canonicalFile;
  }
}
