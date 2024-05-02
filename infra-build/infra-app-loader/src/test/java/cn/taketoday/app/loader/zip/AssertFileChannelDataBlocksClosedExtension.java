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

package cn.taketoday.app.loader.zip;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.app.loader.ref.DefaultCleanerTracking;
import cn.taketoday.app.loader.zip.FileChannelDataBlock.Tracker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension for {@link AssertFileChannelDataBlocksClosed @TrackFileChannelDataBlock}.
 */
class AssertFileChannelDataBlocksClosedExtension implements BeforeEachCallback, AfterEachCallback {

  private static OpenFilesTracker tracker = new OpenFilesTracker();

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    tracker.clear();
    FileChannelDataBlock.tracker = tracker;
    DefaultCleanerTracking.set(tracker::addedCleanable);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    tracker.assertAllClosed();
    FileChannelDataBlock.tracker = null;
  }

  private static final class OpenFilesTracker implements Tracker {

    private final Set<Path> paths = new LinkedHashSet<>();

    private final List<Cleanable> clean = new ArrayList<>();

    private final List<Closeable> close = new ArrayList<>();

    @Override
    public void openedFileChannel(Path path, FileChannel fileChannel) {
      this.paths.add(path);
    }

    @Override
    public void closedFileChannel(Path path, FileChannel fileChannel) {
      this.paths.remove(path);
    }

    void clear() {
      this.paths.clear();
      this.clean.clear();
    }

    void assertAllClosed() throws IOException {
      for (Closeable closeable : this.close) {
        closeable.close();
      }
      this.clean.forEach(Cleanable::clean);
      assertThat(this.paths).as("open paths").isEmpty();
    }

    private void addedCleanable(Object obj, Cleanable cleanable) {
      if (cleanable != null) {
        this.clean.add(cleanable);
      }
      if (obj instanceof Closeable closeable) {
        this.close.add(closeable);
      }
    }

  }

}
