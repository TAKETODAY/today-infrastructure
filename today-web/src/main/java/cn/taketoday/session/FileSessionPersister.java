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

package cn.taketoday.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.session.config.SessionProperties;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Concrete implementation of the <b>SessionPersister</b> interface that utilizes
 * a file per saved Session in a configured directory. Sessions that are
 * saved are still subject to being expired based on inactivity.
 *
 * @author Craig R. McClanahan
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
   * The directory in which Sessions are stored.
   */
  @Nullable
  private File directory;

  @Nullable
  private ApplicationTemp applicationTemp;

  private final SessionRepository repository;

  /**
   * Creates FileSessionPersister with given SessionRepository
   *
   * @param repository used to create session
   * @see #findById(String)
   */
  public FileSessionPersister(SessionRepository repository) {
    Assert.notNull(repository, "SessionRepository is required");
    this.repository = repository;
  }

  /**
   * Set the store directory for this SessionPersister.
   *
   * @param directory The store directory
   */
  public void setDirectory(@Nullable File directory) {
    this.directory = directory;
  }

  /**
   * Set the app temp provider for this SessionPersister.
   *
   * @param applicationTemp The app temp provider
   */
  public void setApplicationTemp(@Nullable ApplicationTemp applicationTemp) {
    this.applicationTemp = applicationTemp;
  }

  /**
   * Remove the Session with the specified session identifier from
   * this SessionPersister, if present.  If no such Session is present,
   * this method takes no action.
   *
   * @param id Session identifier of the Session to be removed
   */
  @Override
  public void remove(String id) throws IOException {
    File file = sessionFile(id);
    if (log.isDebugEnabled()) {
      log.debug("Removing Session [{}] at file [{}]", id, file.getAbsolutePath());
    }

    if (file.exists() && !file.delete()) {
      throw new IOException("Unable to delete file [%s] which is no longer required".formatted(file));
    }
  }

  @Override
  public boolean contains(String id) {
    File file = sessionFile(id);
    return file.exists();
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
   */
  @Override
  public String[] keys() {
    // Acquire the list of files in our storage directory
    String[] files = directory().list();
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
  @Nullable
  @Override
  public WebSession findById(String id) throws ClassNotFoundException, IOException {
    // Open an input stream to the specified pathname, if any
    File file = sessionFile(id);
    if (!file.exists()) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Loading Session [{}] from file [{}]", id, file.getAbsolutePath());
    }

    try (var ois = new ObjectInputStream(new FileInputStream(file))) {
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
  public void persist(WebSession session) throws IOException {
    // Open an output stream to the specified pathname, if any
    File file = sessionFile(session.getId());

    if (log.isDebugEnabled()) {
      log.debug("Saving Session [{}] to file [{}]", session.getId(), file.getAbsolutePath());
    }

    try (var oos = new ObjectOutputStream(new FileOutputStream(file))) {
      if (session instanceof SerializableSession serialized) {
        serialized.writeObjectData(oos);
      }
      else {
        oos.writeObject(session);
      }
    }
  }

  /**
   * Return a File object representing the pathname to our
   * session persistence directory, if any.  The directory will be
   * created if it does not already exist.
   */
  private File directory() {
    File directory = this.directory;
    if (directory == null) {
      directory = SessionProperties.getValidStoreDir(applicationTemp, null, true);
      this.directory = directory;
    }
    return directory;
  }

  /**
   * Return a File object representing the pathname to our
   * session persistence file, if any.
   *
   * @param id The ID of the Session to be retrieved. This is
   * used in the file naming.
   */
  private File sessionFile(String id) {
    String filename = id + FILE_EXT;
    return new File(directory(), filename);
  }

}
