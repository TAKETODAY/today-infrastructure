/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.undertow;

import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;

/**
 * A {@link HttpHandlerFactory} for an {@link AccessLogHandler}.
 *
 * @author Andy Wilkinson
 */
class AccessLogHttpHandlerFactory implements HttpHandlerFactory {

  private final File directory;

  @Nullable
  private final String pattern;

  @Nullable
  private final String prefix;

  @Nullable
  private final String suffix;

  private final boolean rotate;

  AccessLogHttpHandlerFactory(File directory, @Nullable String pattern, @Nullable String prefix, @Nullable String suffix, boolean rotate) {
    this.directory = directory;
    this.pattern = pattern;
    this.prefix = prefix;
    this.suffix = suffix;
    this.rotate = rotate;
  }

  @Override
  public HttpHandler getHandler(HttpHandler next) {
    try {
      createAccessLogDirectoryIfNecessary();
      XnioWorker worker = createWorker();
      String baseName = (this.prefix != null) ? this.prefix : "access_log.";
      String formatString = (this.pattern != null) ? this.pattern : "common";
      return new ClosableAccessLogHandler(next, worker,
              new DefaultAccessLogReceiver(worker, this.directory, baseName, this.suffix, this.rotate),
              formatString);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to create AccessLogHandler", ex);
    }
  }

  private void createAccessLogDirectoryIfNecessary() {
    Assert.state(this.directory != null, "Access log directory is not set");
    if (!this.directory.isDirectory() && !this.directory.mkdirs()) {
      throw new IllegalStateException("Failed to create access log directory '" + this.directory + "'");
    }
  }

  private XnioWorker createWorker() throws IOException {
    Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
    return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
  }

  /**
   * {@link Closeable} variant of {@link AccessLogHandler}.
   */
  private static class ClosableAccessLogHandler extends AccessLogHandler implements Closeable {

    private final DefaultAccessLogReceiver accessLogReceiver;

    private final XnioWorker worker;

    ClosableAccessLogHandler(HttpHandler next, XnioWorker worker, DefaultAccessLogReceiver accessLogReceiver,
                             String formatString) {
      super(next, accessLogReceiver, formatString, Undertow.class.getClassLoader());
      this.worker = worker;
      this.accessLogReceiver = accessLogReceiver;
    }

    @Override
    public void close() throws IOException {
      try {
        this.accessLogReceiver.close();
        this.worker.shutdown();
        this.worker.awaitTermination(30, TimeUnit.SECONDS);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

  }

}
