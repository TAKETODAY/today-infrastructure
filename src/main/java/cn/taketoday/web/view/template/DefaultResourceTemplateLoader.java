/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ConcurrentCache;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import freemarker.cache.TemplateLoader;

/**
 * Default {@link TemplateLoader} implementation
 * 
 * @author TODAY <br>
 *         2019-09-07 22:22
 */
public class DefaultResourceTemplateLoader implements TemplateLoader {

    private String prefix;
    public final ConcurrentCache<String, TemplateSource> cache;
    private HashMap<String, Object> noneExist = new HashMap<>();

    public DefaultResourceTemplateLoader() {
        this(null, 1024);
    }

    public DefaultResourceTemplateLoader(String prefix) {
        this(prefix, 1024);
    }

    public DefaultResourceTemplateLoader(String prefix, int size) {
        this.prefix = prefix;
        this.cache = ConcurrentCache.create(size);
    }

    protected String getTemplateName(final String prefix, final String name) {

        if (StringUtils.isEmpty(prefix)) {
            return StringUtils.checkUrl(name);
        }
        return prefix.concat(StringUtils.checkUrl(name));
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {

        final String templateName = getTemplateName(prefix, name);

        if (noneExist.containsKey(templateName)) {
            return null;
        }

        TemplateSource ret = cache.get(templateName);
        if (ret == null) {
            try {
                final Resource res = ResourceUtils.getResource(templateName);
                if (res.exists()) {
                    cache.put(templateName, ret = TemplateSource.create(res));
                    return ret;
                }
            }
            catch (FileNotFoundException e) {}

            noneExist.put(templateName, Constant.EMPTY_OBJECT);
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

    // Setter Getter
    // -----------------------------

    public final String getPrefix() {
        return prefix;
    }

    public DefaultResourceTemplateLoader setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Put a Template With from a {@link Resource}
     * 
     * @param name
     *            Template name
     * @param resource
     *            {@link TemplateSource} from a {@link Resource}
     * @return this
     * @throws IOException
     *             If any {@link IOException} occurred
     */
    public DefaultResourceTemplateLoader putTemplate(String name, Resource resource) throws IOException {
        cache.put(getTemplateName(prefix, name), TemplateSource.create(resource));
        return this;
    }

    /**
     * Put a Template With a {@link TemplateSource}
     * 
     * @param name
     *            Template name
     * @param template
     *            {@link TemplateSource}
     * @return this
     */
    public DefaultResourceTemplateLoader putTemplate(String name, TemplateSource template) {
        cache.put(getTemplateName(prefix, name), template);
        return this;
    }

    /**
     * Put a Template With last Modified and a {@link ReaderSupplier}
     * 
     * @param name
     *            Template name
     * @param lastModified
     *            lastModified
     * @param reader
     *            {@link ReaderSupplier}
     * @return this
     */
    public DefaultResourceTemplateLoader putTemplate(final String name,
                                                     final long lastModified,
                                                     final ReaderSupplier reader) {
        cache.put(getTemplateName(prefix, name), TemplateSource.create(lastModified, reader));
        return this;
    }

    /**
     * Remove Template from cache
     * 
     * @param name
     *            Template name
     * @return this
     */
    public DefaultResourceTemplateLoader removeTemplate(String name) {
        cache.remove(getTemplateName(prefix, name));
        return this;
    }

    public static final class TemplateSource {

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DefaultResourceTemplateLoader [prefix=");
        builder.append(prefix);
        builder.append(']');
        return builder.toString();
    }
}
