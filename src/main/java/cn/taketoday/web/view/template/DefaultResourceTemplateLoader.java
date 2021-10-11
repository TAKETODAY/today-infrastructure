/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.view.template;

import java.io.IOException;
import java.io.Reader;

import cn.taketoday.lang.Constant;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ConcurrentCache;
import cn.taketoday.util.ObjectUtils;
import freemarker.cache.TemplateLoader;

/**
 * Default {@link TemplateLoader} implementation
 *
 * @author TODAY <br>
 * 2019-09-07 22:22
 */
public class DefaultResourceTemplateLoader implements TemplateLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultResourceTemplateLoader.class);

  private String prefix;
  private String suffix;
  private PatternResourceLoader resourceLoader = new PathMatchingPatternResourceLoader();

  public final ConcurrentCache<String, TemplateSource> cache;

  public DefaultResourceTemplateLoader() {
    this(TemplateRenderer.DEFAULT_TEMPLATE_PATH, Constant.BLANK, 128);
  }

  public DefaultResourceTemplateLoader(String prefix, String suffix) {
    this(prefix, suffix, 128);
  }

  public DefaultResourceTemplateLoader(String prefix, String suffix, int size) {
    this.prefix = prefix;
    this.suffix = suffix;
    this.cache = ConcurrentCache.fromSize(size);
  }

  protected String getTemplate(String name) {
    return new StringBuilder(getPrefix())
            .append(name)
            .append(getSuffix())
            .toString();
  }

  @Override
  public Object findTemplateSource(String name) {
    TemplateSource ret = cache.get(name);
    // TODO 过期
    if (ret == null) {
      String template = getTemplate(name);
      try {
        Resource[] resources = resourceLoader.getResources(template);
        if (ObjectUtils.isNotEmpty(resources)) {
          for (Resource resource : resources) {
            if (resource.exists()) {
              cache.put(name, ret = TemplateSource.create(resource));
              if (log.isDebugEnabled()) {
                log.debug("Template: [{}] Found", resource);
              }
              return ret;
            }
          }
        }
      }
      catch (IOException ignored) { }
      if (log.isDebugEnabled()) {
        log.debug("Template: [{}] Not found", template);
      }
      cache.put(name, TemplateSource.EMPTY);
    }
    else if (ret == TemplateSource.EMPTY) {
      return null;
    }
    return ret;
  }

  @Override
  public long getLastModified(Object source) {
    if (source instanceof TemplateSource) {
      return ((TemplateSource) source).lastModified;
    }
    return -1;
  }

  @Override
  public Reader getReader(Object source, String encoding) throws IOException {
    if (source instanceof TemplateSource) {
      return ((TemplateSource) source).reader.get(encoding);
    }
    return null;
  }

  @Override
  public void closeTemplateSource(Object source) { }

  public void setResourceLoader(PatternResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public PatternResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  /**
   * Put a Template With from a {@link Resource}
   *
   * @param name
   *         Template name
   * @param resource
   *         {@link TemplateSource} from a {@link Resource}
   *
   * @return this
   *
   * @throws IOException
   *         If any {@link IOException} occurred
   */
  public DefaultResourceTemplateLoader putTemplate(String name, Resource resource) throws IOException {
    cache.put(name, TemplateSource.create(resource));
    return this;
  }

  /**
   * Put a Template With a {@link TemplateSource}
   *
   * @param name
   *         Template name
   * @param template
   *         {@link TemplateSource}
   *
   * @return this
   */
  public DefaultResourceTemplateLoader putTemplate(String name, TemplateSource template) {
    cache.put(name, template);
    return this;
  }

  /**
   * Put a Template With last Modified and a {@link ReaderSupplier}
   *
   * @param name
   *         Template name
   * @param lastModified
   *         lastModified
   * @param reader
   *         {@link ReaderSupplier}
   *
   * @return this
   */
  public DefaultResourceTemplateLoader putTemplate(String name, long lastModified, ReaderSupplier reader) {
    cache.put(name, TemplateSource.create(lastModified, reader));
    return this;
  }

  /**
   * Remove Template from cache
   *
   * @param name
   *         Template name
   *
   * @return this
   */
  public DefaultResourceTemplateLoader removeTemplate(String name) {
    cache.remove(name);
    return this;
  }

  static final class TemplateSource {
    static final TemplateSource EMPTY = new TemplateSource(0, null);

    final long lastModified;
    final ReaderSupplier reader;

    TemplateSource(Resource resource) throws IOException {
      this(resource.lastModified(), resource::getReader);
    }

    TemplateSource(long lastModified, ReaderSupplier reader) {
      this.reader = reader;
      this.lastModified = lastModified;
    }

    public static TemplateSource create(Resource resource) throws IOException {
      return new TemplateSource(resource);
    }

    public static TemplateSource create(long lastModified, ReaderSupplier reader) {
      return new TemplateSource(lastModified, reader);
    }
  }

  @FunctionalInterface
  public interface ReaderSupplier {
    Reader get(String encoding) throws IOException;
  }

  // Setter Getter
  // -----------------------------

  @Override
  public String toString() {
    return ObjectUtils.toHexString(this) + " [prefix=" + prefix + ", suffix=" + suffix + "]";
  }

  public String getSuffix() {
    return suffix;
  }

  public String getPrefix() {
    return prefix;
  }

  public DefaultResourceTemplateLoader setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  public DefaultResourceTemplateLoader setSuffix(String suffix) {
    this.suffix = suffix;
    return this;
  }
}
