/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.context.io.PathMatchingResourcePatternResolver;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.ResourceResolver;
import cn.taketoday.context.utils.ConcurrentCache;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.Constant;
import freemarker.cache.TemplateLoader;

/**
 * Default {@link TemplateLoader} implementation
 *
 * @author TODAY <br>
 * 2019-09-07 22:22
 */
public class DefaultResourceTemplateLoader implements TemplateLoader {

  private String prefix;
  private String suffix;
  private ResourceResolver pathMatchingResolver = new PathMatchingResourcePatternResolver();

  public final ConcurrentCache<String, TemplateSource> cache;

  public DefaultResourceTemplateLoader() {
    this(Constant.DEFAULT_TEMPLATE_PATH, Constant.BLANK, 1024);
  }

  public DefaultResourceTemplateLoader(String prefix, String suffix) {
    this(prefix, suffix, 1024);
  }

  public DefaultResourceTemplateLoader(String prefix, String suffix, int size) {
    this.prefix = prefix;
    this.suffix = suffix;
    this.cache = ConcurrentCache.create(size);
  }

  protected String getTemplate(final String name) {
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
      try {
        final String template = getTemplate(name);
        final Resource[] resources = pathMatchingResolver.getResources(template);
        if (ObjectUtils.isNotEmpty(resources)) {
          for (final Resource resource : resources) {
            if (resource.exists()) {
              cache.put(name, ret = TemplateSource.create(resource));
              return ret;
            }
          }
        }
      }
      catch (IOException ignored) {}
      cache.put(name, TemplateSource.EMPTY);
    }
    else if (ret == TemplateSource.EMPTY) {
      return null;
    }
    return ret;
  }

  @Override
  public long getLastModified(final Object source) {

    if (source instanceof TemplateSource) {
      return ((TemplateSource) source).lastModified;
    }
    return -1;
  }

  @Override
  public Reader getReader(final Object source, final String encoding) throws IOException {
    if (source instanceof TemplateSource) {
      return ((TemplateSource) source).reader.get(encoding);
    }
    return null;
  }

  @Override
  public void closeTemplateSource(final Object source) throws IOException {}

  public void setPathMatchingResolver(ResourceResolver pathMatchingResolver) {
    this.pathMatchingResolver = pathMatchingResolver;
  }

  public ResourceResolver getPathMatchingResolver() {
    return pathMatchingResolver;
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
  public DefaultResourceTemplateLoader putTemplate(final String name, final long lastModified, final ReaderSupplier reader) {
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

  public static final class TemplateSource {
    static final TemplateSource EMPTY = new TemplateSource(0, null);

    private final long lastModified;
    private final ReaderSupplier reader;

    protected TemplateSource(Resource resource) throws IOException {
      this(resource.lastModified(), resource::getReader);
    }

    protected TemplateSource(long lastModified, ReaderSupplier reader) {
      this.reader = reader;
      this.lastModified = lastModified;
    }

    public static TemplateSource create(Resource resource) throws IOException {
      return new TemplateSource(resource);
    }

    public static TemplateSource create(final long lastModified, final ReaderSupplier reader) {
      return new TemplateSource(lastModified, reader);
    }
  }

  @FunctionalInterface
  public interface ReaderSupplier {
    Reader get(String c) throws IOException;
  }

  // Setter Getter
  // -----------------------------

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DefaultResourceTemplateLoader [prefix=").append(prefix).append(", suffix=").append(suffix).append("]");
    return builder.toString();
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
