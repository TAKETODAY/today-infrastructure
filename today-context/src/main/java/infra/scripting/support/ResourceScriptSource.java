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

package infra.scripting.support;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import infra.core.io.EncodedResource;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.scripting.ScriptSource;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;

/**
 * {@link ScriptSource} implementation
 * based on Framework's {@link Resource}
 * abstraction. Loads the script text from the underlying Resource's
 * {@link Resource#getFile() File} or
 * {@link Resource#getInputStream() InputStream},
 * and tracks the last-modified timestamp of the file (if possible).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Resource#getInputStream()
 * @see Resource#getFile()
 * @see ResourceLoader
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
    Assert.notNull(resource, "Resource is required");
    this.resource = resource;
  }

  /**
   * Create a new ResourceScriptSource for the given resource.
   *
   * @param resource the Resource to load the script from (using UTF-8 encoding)
   */
  public ResourceScriptSource(Resource resource) {
    Assert.notNull(resource, "Resource is required");
    this.resource = new EncodedResource(resource, StandardCharsets.UTF_8);
  }

  /**
   * Return the {@link Resource} to load the
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
