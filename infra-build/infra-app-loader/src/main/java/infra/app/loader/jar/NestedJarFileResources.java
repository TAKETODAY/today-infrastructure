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

package infra.app.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Inflater;

import infra.app.loader.ref.Cleaner;
import infra.app.loader.zip.ZipContent;
import infra.app.loader.zip.ZipContent.Kind;

/**
 * Resources created managed and cleaned by a {@link NestedJarFile} instance and suitable
 * for registration with a {@link Cleaner}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class NestedJarFileResources implements Runnable {

  private static final int INFLATER_CACHE_LIMIT = 20;

  private ZipContent zipContent;

  private ZipContent zipContentForManifest;

  private final Set<InputStream> inputStreams = Collections.newSetFromMap(new WeakHashMap<>());

  private Deque<Inflater> inflaterCache = new ArrayDeque<>();

  /**
   * Create a new {@link NestedJarFileResources} instance.
   *
   * @param file the source zip file
   * @param nestedEntryName the nested entry or {@code null}
   * @throws IOException on I/O error
   */
  NestedJarFileResources(File file, String nestedEntryName) throws IOException {
    this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
    this.zipContentForManifest = (this.zipContent.getKind() != Kind.NESTED_DIRECTORY) ? null
            : ZipContent.open(file.toPath());
  }

  /**
   * Return the underling {@link ZipContent}.
   *
   * @return the zip content
   */
  ZipContent zipContent() {
    return this.zipContent;
  }

  /**
   * Return the underlying {@link ZipContent} that should be used to load manifest
   * content.
   *
   * @return the zip content to use when loading the manifest
   */
  ZipContent zipContentForManifest() {
    return (this.zipContentForManifest != null) ? this.zipContentForManifest : this.zipContent;
  }

  /**
   * Add a managed input stream resource.
   *
   * @param inputStream the input stream
   */
  void addInputStream(InputStream inputStream) {
    synchronized(this.inputStreams) {
      this.inputStreams.add(inputStream);
    }
  }

  /**
   * Remove a managed input stream resource.
   *
   * @param inputStream the input stream
   */
  void removeInputStream(InputStream inputStream) {
    synchronized(this.inputStreams) {
      this.inputStreams.remove(inputStream);
    }
  }

  /**
   * Create a {@link Runnable} action to cleanup the given inflater.
   *
   * @param inflater the inflater to cleanup
   * @return the cleanup action
   */
  Runnable createInflatorCleanupAction(Inflater inflater) {
    return () -> endOrCacheInflater(inflater);
  }

  /**
   * Get previously used {@link Inflater} from the cache, or create a new one.
   *
   * @return a usable {@link Inflater}
   */
  Inflater getOrCreateInflater() {
    Deque<Inflater> inflaterCache = this.inflaterCache;
    if (inflaterCache != null) {
      synchronized(inflaterCache) {
        Inflater inflater = this.inflaterCache.poll();
        if (inflater != null) {
          return inflater;
        }
      }
    }
    return new Inflater(true);
  }

  /**
   * Either release the given {@link Inflater} by calling {@link Inflater#end()} or add
   * it to the cache for later reuse.
   *
   * @param inflater the inflater to end or cache
   */
  private void endOrCacheInflater(Inflater inflater) {
    Deque<Inflater> inflaterCache = this.inflaterCache;
    if (inflaterCache != null) {
      synchronized(inflaterCache) {
        if (this.inflaterCache == inflaterCache && inflaterCache.size() < INFLATER_CACHE_LIMIT) {
          inflater.reset();
          this.inflaterCache.add(inflater);
          return;
        }
      }
    }
    inflater.end();
  }

  /**
   * Called by the {@link Cleaner} to free resources.
   *
   * @see Runnable#run()
   */
  @Override
  public void run() {
    releaseAll();
  }

  private void releaseAll() {
    IOException exceptionChain = null;
    exceptionChain = releaseInflators(exceptionChain);
    exceptionChain = releaseInputStreams(exceptionChain);
    exceptionChain = releaseZipContent(exceptionChain);
    exceptionChain = releaseZipContentForManifest(exceptionChain);
    if (exceptionChain != null) {
      throw new UncheckedIOException(exceptionChain);
    }
  }

  private IOException releaseInflators(IOException exceptionChain) {
    Deque<Inflater> inflaterCache = this.inflaterCache;
    if (inflaterCache != null) {
      try {
        synchronized(inflaterCache) {
          inflaterCache.forEach(Inflater::end);
        }
      }
      finally {
        this.inflaterCache = null;
      }
    }
    return exceptionChain;
  }

  private IOException releaseInputStreams(IOException exceptionChain) {
    synchronized(this.inputStreams) {
      for (InputStream inputStream : List.copyOf(this.inputStreams)) {
        try {
          inputStream.close();
        }
        catch (IOException ex) {
          exceptionChain = addToExceptionChain(exceptionChain, ex);
        }
      }
      this.inputStreams.clear();
    }
    return exceptionChain;
  }

  private IOException releaseZipContent(IOException exceptionChain) {
    ZipContent zipContent = this.zipContent;
    if (zipContent != null) {
      try {
        zipContent.close();
      }
      catch (IOException ex) {
        exceptionChain = addToExceptionChain(exceptionChain, ex);
      }
      finally {
        this.zipContent = null;
      }
    }
    return exceptionChain;
  }

  private IOException releaseZipContentForManifest(IOException exceptionChain) {
    ZipContent zipContentForManifest = this.zipContentForManifest;
    if (zipContentForManifest != null) {
      try {
        zipContentForManifest.close();
      }
      catch (IOException ex) {
        exceptionChain = addToExceptionChain(exceptionChain, ex);
      }
      finally {
        this.zipContentForManifest = null;
      }
    }
    return exceptionChain;
  }

  private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
    if (exceptionChain != null) {
      exceptionChain.addSuppressed(ex);
      return exceptionChain;
    }
    return ex;
  }

}
