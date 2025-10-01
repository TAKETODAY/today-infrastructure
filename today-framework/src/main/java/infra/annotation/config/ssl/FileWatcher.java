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

package infra.annotation.config.ssl;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Watches files and directories and triggers a callback on change.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class FileWatcher {

  private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

  private final Duration quietPeriod;

  private final Object lock = new Object();

  @Nullable
  private WatcherThread thread;

  /**
   * Create a new {@link FileWatcher} instance.
   *
   * @param quietPeriod the duration that no file changes should occur before triggering
   * actions
   */
  FileWatcher(Duration quietPeriod) {
    Assert.notNull(quietPeriod, "QuietPeriod is required");
    this.quietPeriod = quietPeriod;
  }

  /**
   * Watch the given files or directories for changes.
   *
   * @param paths the files or directories to watch
   * @param action the action to take when changes are detected
   */
  void watch(Set<Path> paths, Runnable action) {
    Assert.notNull(paths, "Paths is required");
    Assert.notNull(action, "Action is required");
    if (paths.isEmpty()) {
      return;
    }
    synchronized(this.lock) {
      try {
        if (this.thread == null) {
          this.thread = new WatcherThread();
          this.thread.start();
        }
        Set<Path> registrationPaths = new HashSet<>();
        for (Path path : paths) {
          registrationPaths.addAll(getRegistrationPaths(path));
        }
        this.thread.register(new Registration(registrationPaths, action));
      }
      catch (IOException ex) {
        throw new UncheckedIOException("Failed to register paths for watching: " + paths, ex);
      }
    }
  }

  public void destroy() throws IOException {
    synchronized(this.lock) {
      if (this.thread != null) {
        this.thread.close();
        this.thread.interrupt();
        try {
          this.thread.join();
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        this.thread = null;
      }
    }
  }

  /**
   * Retrieves all {@link Path Paths} that should be registered for the specified
   * {@link Path}. If the path is a symlink, changes to the symlink should be monitored,
   * not just the file it points to. For example, for the given {@code keystore.jks}
   * path in the following directory structure:<pre>
   * .
   * ├── ..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f
   * │   ├── keystore.jks
   * ├── ..data -> ..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f
   * ├── keystore.jks -> ..data/keystore.jks
   * </pre> the resulting paths would include:
   * <ul>
   * <li><b>keystore.jks</b></li>
   * <li><b>..data/keystore.jks</b></li>
   * <li><b>..data</b></li>
   * <li><b>..a72e81ff-f0e1-41d8-a19b-068d3d1d4e2f/keystore.jks</b></li>
   * </ul>
   *
   * @param path the path
   * @return all possible {@link Path} instances to be registered
   * @throws IOException if an I/O error occurs
   */
  private static Set<Path> getRegistrationPaths(Path path) throws IOException {
    path = path.toAbsolutePath();
    Set<Path> result = new HashSet<>();
    result.add(path);
    Path parent = path.getParent();
    if (parent != null && Files.isSymbolicLink(parent)) {
      result.add(parent);
      Path target = parent.resolveSibling(Files.readSymbolicLink(parent));
      result.addAll(getRegistrationPaths(target.resolve(path.getFileName())));
    }
    else if (Files.isSymbolicLink(path)) {
      Path target = path.resolveSibling(Files.readSymbolicLink(path));
      result.addAll(getRegistrationPaths(target));
    }
    return result;
  }

  /**
   * The watcher thread used to check for changes.
   */
  private class WatcherThread extends Thread implements Closeable {

    private final WatchService watchService = FileSystems.getDefault().newWatchService();

    private final Map<WatchKey, List<Registration>> registrations = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    WatcherThread() throws IOException {
      setName("ssl-bundle-watcher");
      setDaemon(true);
      setUncaughtExceptionHandler(this::onThreadException);
    }

    private void onThreadException(Thread thread, Throwable throwable) {
      logger.error("Uncaught exception in file watcher thread", throwable);
    }

    void register(Registration registration) throws IOException {
      Set<Path> directories = new HashSet<>();
      for (Path path : registration.paths()) {
        if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
          throw new IOException("'%s' is neither a file nor a directory".formatted(path));
        }
        Path directory = Files.isDirectory(path) ? path : path.getParent();
        directories.add(directory);
      }
      for (Path directory : directories) {
        WatchKey watchKey = register(directory);
        this.registrations.computeIfAbsent(watchKey, (key) -> new CopyOnWriteArrayList<>()).add(registration);
      }
    }

    private WatchKey register(Path directory) throws IOException {
      logger.debug("Registering '{}'", directory);
      return directory.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
    }

    @Override
    public void run() {
      logger.debug("Watch thread started");
      Set<Runnable> actions = new HashSet<>();
      long timeout = FileWatcher.this.quietPeriod.toMillis();
      while (this.running) {
        try {
          WatchKey key = this.watchService.poll(timeout, TimeUnit.MILLISECONDS);
          if (key == null) {
            actions.forEach(this::runSafely);
            actions.clear();
          }
          else {
            accumulate(key, actions);
            key.reset();
          }
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        catch (ClosedWatchServiceException ex) {
          logger.debug("File watcher has been closed");
          this.running = false;
        }
      }
      logger.debug("Watch thread stopped");
    }

    private void runSafely(Runnable action) {
      try {
        action.run();
      }
      catch (Throwable ex) {
        logger.error("Unexpected SSL reload error", ex);
      }
    }

    @SuppressWarnings("NullAway")
    private void accumulate(WatchKey key, Set<Runnable> actions) {
      List<Registration> registrations = this.registrations.get(key);
      Path directory = (Path) key.watchable();
      for (WatchEvent<?> event : key.pollEvents()) {
        Path file = directory.resolve((Path) event.context());
        for (Registration registration : registrations) {
          if (registration.manages(file)) {
            actions.add(registration.action());
          }
        }
      }
    }

    @Override
    public void close() throws IOException {
      this.running = false;
      this.watchService.close();
    }

  }

  /**
   * An individual watch registration.
   */
  private record Registration(Set<Path> paths, Runnable action) {

    Registration {
      paths = paths.stream().map(Path::toAbsolutePath).collect(Collectors.toSet());
    }

    boolean manages(Path file) {
      Path absolutePath = file.toAbsolutePath();
      return this.paths.contains(absolutePath) || isInDirectories(absolutePath);
    }

    private boolean isInDirectories(Path file) {
      return this.paths.stream().filter(Files::isDirectory).anyMatch(file::startsWith);
    }
  }

}
