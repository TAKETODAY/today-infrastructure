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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view;

import static cn.taketoday.web.resolver.method.DelegatingParameterResolver.delegate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.SharedVariable;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import lombok.Getter;

/**
 * @author TODAY <br>
 *         2018-06-26 19:16:46
 */
@Props(prefix = "web.mvc.view.")
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@MissingBean(value = Constant.VIEW_RESOLVER, type = ViewResolver.class)
public class FreeMarkerViewResolver extends AbstractViewResolver implements InitializingBean, WebMvcConfiguration {

    private final ObjectWrapper wrapper;

    @Getter
    private final Configuration configuration;
    private final TaglibFactory taglibFactory;

    private final ServletContext servletContext;
    private final ServletContextHashModel applicationModel;

    private int cacheSize = 1024;

    @Autowired
    public FreeMarkerViewResolver(//
            @Autowired(required = false) ObjectWrapper wrapper, //
            @Autowired(required = false) Configuration configuration, //
            @Autowired(required = false) TaglibFactory taglibFactory, //
            @Props(prefix = "freemarker.", replace = true) Properties settings) //
    {
        final WebServletApplicationContext context = //
                (WebServletApplicationContext) ContextUtils.getApplicationContext();

        if (configuration == null) {
            configuration = new Configuration(Configuration.VERSION_2_3_28);
            context.registerSingleton(configuration.getClass().getName(), configuration);
        }

        this.configuration = configuration;
        this.servletContext = context.getServletContext();
        this.wrapper = wrapper != null ? wrapper : new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        this.taglibFactory = taglibFactory != null ? taglibFactory : new TaglibFactory(this.servletContext);
        this.configuration.setObjectWrapper(this.wrapper);

        // Create hash model wrapper for servlet context (the application)
        this.applicationModel = new ServletContextHashModel(this.servletContext, wrapper);

        context.getBeansOfType(TemplateModel.class).forEach(configuration::setSharedVariable);

        try {
            if (settings != null && !settings.isEmpty()) {
                this.configuration.setSettings(settings);
            }
        }
        catch (TemplateException e) {
            throw new ConfigurationException("Set FreeMarker's Properties Error, With: [" + e + "]", e);
        }
    }

    /**
     * Use {@link afterPropertiesSet}
     * 
     * @since 2.3.3
     */
    @Override
    public void afterPropertiesSet() {

        this.configuration.setLocale(locale);
        this.configuration.setDefaultEncoding(encoding);
    }

    @Override
    public void configureParameterResolver(List<ParameterResolver> resolvers) {

        resolvers.add(delegate((m) -> m.isAssignableFrom(Configuration.class), (ctx, m) -> configuration));

        resolvers.add(delegate((m) -> m.isAnnotationPresent(SharedVariable.class), (ctx, m) -> {
            final TemplateModel sharedVariable = configuration.getSharedVariable(m.getName());

            if (m.isInstance(sharedVariable)) {
                return sharedVariable;
            }

            if (sharedVariable instanceof WrapperTemplateModel) {
                final Object wrappedObject = ((WrapperTemplateModel) sharedVariable).getWrappedObject();
                if (m.isInstance(wrappedObject)) {
                    return wrappedObject;
                }
                throw new ConfigurationException("Not a instance of: " + m.getParameterClass());
            }

            if (m.isRequired()) {
                throw new ConfigurationException("There is no shared variable named: " + m.getName());
            }
            return ConvertUtils.convert(m.getDefaultValue(), m.getParameterClass());
        }));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void configureTemplateLoader(List<T> loaders) {

        final TemplateLoader loader;
        if (loaders.isEmpty()) {
            if (StringUtils.isNotEmpty(prefix) && prefix.startsWith("/WEB-INF/")) {// prefix -> /WEB-INF/..
                loader = new WebappTemplateLoader(servletContext, prefix);
            }
            else {
                loader = new DefaultTemplateLoader(prefix, cacheSize);
            }
        }
        else {
            loader = new CompositeTemplateLoader((Collection<TemplateLoader>) loaders, cacheSize);
        }

        configuration.setTemplateLoader(loader);

        final Logger logger = LoggerFactory.getLogger(getClass());

        logger.info("FreeMarker use [{}] to load templates", loader);
        logger.info("Configuration FreeMarker View Resolver Success.");
    }

    /**
     * Create Model Attributes.
     * 
     * @param context
     *            Current request context
     * @return {@link TemplateHashModel}
     */
    protected TemplateHashModel createModel(RequestContext context) {
        final ObjectWrapper wrapper = this.wrapper;

        final HttpServletRequest request = context.nativeRequest();

        final AllHttpScopesHashModel ret = //
                new AllHttpScopesHashModel(wrapper, servletContext, request);

        ret.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, taglibFactory);
        ret.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, applicationModel);
        // Create hash model wrapper for request
        ret.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, wrapper));
        ret.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
        // Create hash model wrapper for session
        ret.putUnlistedModel(FreemarkerServlet.KEY_SESSION,
                             new HttpSessionHashModel(context.nativeSession(), wrapper));

        return ret;
    }

    /**
     * Resolve FreeMarker View.
     */
    @Override
    public void resolveView(final String template, final RequestContext context) throws Throwable {

        configuration.getTemplate(template + suffix, locale, encoding)//
                .process(createModel(context), context.getWriter());
    }

    // TemplateLoaders
    // -------------------------------------------

    /**
     * Composite {@link TemplateLoader}
     * <p>
     * A TemplateLoader that uses a set of other loaders to load the templates.
     * <p>
     * 
     * @author TODAY <br>
     *         2019-09-07 23:48
     */
    public static class CompositeTemplateLoader implements TemplateLoader {

        // none null
        private TemplateLoader[] loaders;

        private final Cache<String, TemplateLoader> cache;

        public CompositeTemplateLoader(TemplateLoader... loaders) {
            this(512, loaders);
        }

        public CompositeTemplateLoader(int cacheSize, TemplateLoader... loaders) {
            setTemplateLoaders(loaders);
            this.cache = new Cache<>(cacheSize);
        }

        public CompositeTemplateLoader(Collection<TemplateLoader> loaders) {
            this(loaders, 512);
        }

        public CompositeTemplateLoader(Collection<TemplateLoader> loaders, int cacheSize) {
            addTemplateLoaders(loaders);
            this.cache = new Cache<>(cacheSize);
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

    }

    /**
     * Default {@link TemplateLoader} implementation
     * 
     * @author TODAY <br>
     *         2019-09-07 22:22
     */
    public static class DefaultTemplateLoader implements TemplateLoader {

        private String prefix;
        private static final Object empty = new Object();
        public final Cache<String, TemplateSource> cache;
        private HashMap<String, Object> noneExist = new HashMap<>();

        public DefaultTemplateLoader() {
            this(null, 1024);
        }

        public DefaultTemplateLoader(String prefix) {
            this(prefix, 1024);
        }

        public DefaultTemplateLoader(String prefix, int size) {
            this.prefix = prefix;
            this.cache = new Cache<>(size);
        }

        protected String getTemplateName(final String prefix, final String name) {

            if (StringUtils.isEmpty(prefix)) {
                return StringUtils.checkUrl(name);
            }
            return prefix + StringUtils.checkUrl(name);
        }

        @Override
        public Object findTemplateSource(String name) throws IOException {

            final String templateName = getTemplateName(getPrefix(), name);

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
                noneExist.put(templateName, empty);
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

        public String getPrefix() {
            return prefix;
        }

        public DefaultTemplateLoader setPrefix(String prefix) {
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
        public DefaultTemplateLoader putTemplate(String name, Resource resource) throws IOException {
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
        public DefaultTemplateLoader putTemplate(String name, TemplateSource template) {
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
        public DefaultTemplateLoader putTemplate(final String name, final long lastModified,
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
        public DefaultTemplateLoader removeTemplate(String name) {
            cache.remove(getTemplateName(prefix, name));
            return this;
        }
    }

    public static final class Cache<K, V> {

        private final int size;
        private final Map<K, V> eden;
        private final Map<K, V> longterm;

        private Cache(int size) {
            this.size = size;
            this.longterm = new WeakHashMap<>(size);
            this.eden = new ConcurrentHashMap<>(size);
        }

        public V get(K k) {
            V v = this.eden.get(k);
            if (v == null) {
                synchronized (longterm) {
                    v = this.longterm.get(k);
                }
                if (v != null) {
                    this.eden.put(k, v);
                }
            }
            return v;
        }

        public void put(K k, V v) {
            if (this.eden.size() >= size) {
                synchronized (longterm) {
                    this.longterm.putAll(this.eden);
                }
                this.eden.clear();
            }
            this.eden.put(k, v);
        }

        public void remove(K k) {
            this.eden.remove(k);
            synchronized (longterm) {
                this.longterm.remove(k);
            }
        }
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

    /**
     * @author TODAY <br>
     *         2019-09-08 12:05
     */
    @FunctionalInterface
    public interface ReaderSupplier {

        Reader get(String c) throws IOException;
    }

}
