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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ConcurrentCache;
import cn.taketoday.context.utils.OrderUtils;
import freemarker.cache.StatefulTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Composite {@link TemplateLoader}
 * <p>
 * A TemplateLoader that uses a set of other loaders to load the templates.
 * <p>
 * 
 * @author TODAY <br>
 *         2019-09-07 23:48
 */
public class CompositeTemplateLoader implements TemplateLoader {

    // none null
    private TemplateLoader[] loaders;

    private final ConcurrentCache<String, TemplateLoader> cache;

    public CompositeTemplateLoader(TemplateLoader... loaders) {
        this(512, loaders);
    }

    public CompositeTemplateLoader(int cacheSize, TemplateLoader... loaders) {
        setTemplateLoaders(loaders);
        this.cache = ConcurrentCache.create(cacheSize);
    }

    public CompositeTemplateLoader(Collection<TemplateLoader> loaders) {
        this(loaders, 512);
    }

    public CompositeTemplateLoader(Collection<TemplateLoader> loaders, int cacheSize) {
        addTemplateLoaders(loaders);
        this.cache = ConcurrentCache.create(cacheSize);
    }

    /**
     * All {@link TemplateLoader}s
     * 
     * @return All {@link TemplateLoader}s
     */
    public final TemplateLoader[] getTemplateLoaders() {
        return loaders;
    }

    /**
     * Set {@link TemplateLoader}s
     * 
     * @param values
     *            Input {@link TemplateLoader}s
     * 
     * @return This object
     */
    public final CompositeTemplateLoader setTemplateLoaders(final TemplateLoader... values) {
        this.loaders = null;
        this.loaders = values;
        return this;
    }

    /**
     * Add array of {@link TemplateLoader}s
     * 
     * @param values
     *            Input {@link TemplateLoader}s
     * 
     * @return This object
     */
    public CompositeTemplateLoader addTemplateLoaders(final TemplateLoader... values) {

        if (loaders == null) {
            return setTemplateLoaders(values);
        }

        final List<TemplateLoader> list = new ArrayList<>(loaders.length + Objects.requireNonNull(values).length);

        Collections.addAll(list, values);
        Collections.addAll(list, loaders);

        OrderUtils.reversedSort(list);

        TemplateLoader[] loader = new TemplateLoader[0];
        return setTemplateLoaders(list.toArray(loader));
    }

    /**
     * Add {@link Collection} of {@link TemplateLoader}
     * 
     * @param loaders
     *            {@link Collection} of {@link TemplateLoader}
     * @return This object
     */
    public CompositeTemplateLoader addTemplateLoaders(final Collection<TemplateLoader> loaders) {

        final List<TemplateLoader> list;
        if (this.loaders == null) {
            if (Objects.requireNonNull(loaders) instanceof List) {
                list = (List<TemplateLoader>) loaders;
            }
            else {
                list = new ArrayList<>(loaders);
            }
        }
        else {
            if (Objects.requireNonNull(loaders) instanceof List) {
                list = (List<TemplateLoader>) loaders;
            }
            else {
                list = new ArrayList<>(this.loaders.length + loaders.size());
                list.addAll(loaders);
            }
            Collections.addAll(list, this.loaders);
        }

        OrderUtils.reversedSort(list);

        TemplateLoader[] loader = new TemplateLoader[0];
        return setTemplateLoaders(list.toArray(loader));
    }

    /**
     * Get a suitable {@link TemplateLoader} With given name
     * 
     * @param name
     *            Template name
     * @return Suitable {@link TemplateLoader}. May null if there isn't a suitable
     *         {@link TemplateLoader}
     * @throws IOException
     *             If any {@link IOException} coourred
     */
    protected TemplateLoader getTemplateLoader(String name) throws IOException {

        final TemplateLoader ret = cache.get(name);
        if (ret == null) {
            for (final TemplateLoader loader : loaders) {
                final Object source = loader.findTemplateSource(name);
                if (source != null) {
                    cache.put(name, loader);
                    return loader;
                }
            }
        }
        return ret;
    }

    // TemplateLoader
    // -------------------------------------------

    @Override
    public Object findTemplateSource(String name) throws IOException {

        final TemplateLoader loader = getTemplateLoader(name);
        if (loader != null) {
            return loader.findTemplateSource(name);
        }
        return null;
    }

    @Override
    public long getLastModified(Object source) {
        for (final TemplateLoader loader : loaders) {
            final long last = loader.getLastModified(source);
            if (last != -1) return last;
        }
        return -1;
    }

    @Override
    public Reader getReader(Object source, String encoding) throws IOException {
        for (final TemplateLoader loader : loaders) {
            final Reader reader = loader.getReader(source, encoding);
            if (reader != null) {
                return reader;
            }
        }
        return null;
    }

    @Override
    public void closeTemplateSource(Object source) throws IOException {

        if (source instanceof Resource == false) for (final TemplateLoader loader : loaders) {
            loader.closeTemplateSource(source);
        }
    }

    public void resetState() {
        cache.clear();
        for (final TemplateLoader loader : loaders) {
            if (loader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader) loader).resetState();
            }
        }
    }
}
