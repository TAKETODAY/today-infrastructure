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
package cn.taketoday.scripting.support;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ScriptSource} implementation
 * based on Framework's {@link cn.taketoday.core.io.Resource}
 * abstraction. Loads the script text from the underlying Resource's
 * {@link cn.taketoday.core.io.Resource#getFile() File} or
 * {@link cn.taketoday.core.io.Resource#getInputStream() InputStream},
 * and tracks the last-modified timestamp of the file (if possible).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see cn.taketoday.core.io.Resource#getInputStream()
 * @see cn.taketoday.core.io.Resource#getFile()
 * @see cn.taketoday.core.io.ResourceLoader
 * @since 4.0
 */
public class ResourceScriptSource implements ScriptSource {
  private static final Logger log = LoggerFactory.getLogger(ResourceScriptSource.class);

  private EncodedResource resource;

  private long lastModified = -1;

  private final Object lastModifiedMonitor = new Object();

  /**
   * Create a new ResourceScriptSource for the given resource.
   *
   * @param resource the EncodedResource to load the script from
   */
  public ResourceScriptSource(EncodedResource resource) {
    Assert.notNull(resource, "Resource must not be null");
    this.resource = resource;
  }

  /**
   * Create a new ResourceScriptSource for the given resource.
   *
   * @param resource the Resource to load the script from (using UTF-8 encoding)
   */
  public ResourceScriptSource(Resource resource) {
    Assert.notNull(resource, "Resource must not be null");
    this.resource = new EncodedResource(resource, StandardCharsets.UTF_8);
  }

  /**
   * Return the {@link cn.taketoday.core.io.Resource} to load the
   * script from.
   */
  public final Resource getResource() {
    return this.resource.getResource();
  }

  /**
   * Set the encoding used for reading the script resource.
   * <p>The default value for regular Resources is "UTF-8".
   * A {@code null} value implies the platform default.
   */
  public void setEncoding(@Nullable String encoding) {
    this.resource = new EncodedResource(this.resource.getResource(), encoding);
  }

  @Override
  public String getScriptAsString() throws IOException {
    synchronized(this.lastModifiedMonitor) {
      this.lastModified = retrieveLastModifiedTime();
    }
    Reader reader = this.resource.getReader();
    return FileCopyUtils.copyToString(reader);
  }

  @Override
  public boolean isModified() {
    synchronized(this.lastModifiedMonitor) {
      return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
    }
  }

  /**
   * Retrieve the current last-modified timestamp of the underlying resource.
   *
   * @return the current timestamp, or 0 if not determinable
   */
  protected long retrieveLastModifiedTime() {
    try {
      return getResource().lastModified();
    }
    catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug(getResource() + " could not be resolved in the file system - " +
                "current timestamp not available for script modification check", ex);
      }
      return 0;
    }
  }

  @Override
  @Nullable
  public String suggestedClassName() {
    String filename = getResource().getName();
    return filename != null ? StringUtils.stripFilenameExtension(filename) : null;
  }

  @Override
  public String toString() {
    return this.resource.toString();
  }

}
