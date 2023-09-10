/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.codec.multipart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Represents a directory used to store parts larger than
 * {@link DefaultPartHttpMessageReader#setMaxInMemorySize(int)}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class FileStorage {

  private static final Logger logger = LoggerFactory.getLogger(FileStorage.class);

  protected FileStorage() { }

  /**
   * Get the mono of the directory to store files in.
   */
  public abstract Mono<Path> directory();

  /**
   * Create a new {@code FileStorage} from a user-specified path. Creates the
   * path if it does not exist.
   */
  public static FileStorage fromPath(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectory(path);
    }
    return new PathFileStorage(path);
  }

  /**
   * Create a new {@code FileStorage} based on a temporary directory.
   *
   * @param scheduler the scheduler to use for blocking operations
   */
  public static FileStorage tempDirectory(Supplier<Scheduler> scheduler) {
    return new TempFileStorage(scheduler);
  }

  private static final class PathFileStorage extends FileStorage {

    private final Mono<Path> directory;

    public PathFileStorage(Path directory) {
      this.directory = Mono.just(directory);
    }

    @Override
    public Mono<Path> directory() {
      return this.directory;
    }
  }

  private static final class TempFileStorage extends FileStorage {

    private static final String IDENTIFIER = "multipart-";

    private final Supplier<Scheduler> scheduler;

    private volatile Mono<Path> directory = tempDirectory();

    public TempFileStorage(Supplier<Scheduler> scheduler) {
      this.scheduler = scheduler;
    }

    @Override
    public Mono<Path> directory() {
      return this.directory
              .flatMap(this::createNewDirectoryIfDeleted)
              .subscribeOn(this.scheduler.get());
    }

    private Mono<Path> createNewDirectoryIfDeleted(Path directory) {
      if (!Files.exists(directory)) {
        // Some daemons remove temp directories. Let's create a new one.
        Mono<Path> newDirectory = tempDirectory();
        this.directory = newDirectory;
        return newDirectory;
      }
      else {
        return Mono.just(directory);
      }
    }

    private static Mono<Path> tempDirectory() {
      return Mono.fromCallable(() -> {
        Path directory = ApplicationTemp.createDirectory(IDENTIFIER + StringUtils.getUUIDString());
        if (logger.isDebugEnabled()) {
          logger.debug("Created temporary storage directory: {}", directory);
        }
        directory.toFile().deleteOnExit();
        return directory;
      }).cache();
    }
  }

}
