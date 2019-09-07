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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.SharedVariable;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.resolver.method.DelegatingParameterResolver;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import cn.taketoday.web.utils.WebUtils;
import freemarker.cache.TemplateLoader;
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
@MissingBean(value = Constant.VIEW_RESOLVER, type = ViewResolver.class)
public class FreeMarkerViewResolver extends AbstractViewResolver implements InitializingBean, WebMvcConfiguration {

    private final ObjectWrapper wrapper;

    @Getter
    private final Configuration configuration;
    private final TaglibFactory taglibFactory;

    private final ServletContext servletContext;
    private final ServletContextHashModel applicationModel;

    @Autowired
    public FreeMarkerViewResolver(//
            @Autowired(required = false) ObjectWrapper wrapper, //
            @Autowired(required = false) Configuration configuration, //
            @Autowired(required = false) TaglibFactory taglibFactory, //
            @Props(prefix = "freemarker.", replace = true) Properties settings) //
    {

        WebServletApplicationContext context = //
                (WebServletApplicationContext) WebUtils.getWebApplicationContext();

        if (configuration == null) {
            configuration = new Configuration(Configuration.VERSION_2_3_28);
            context.registerSingleton(configuration.getClass().getName(), configuration);
        }

        this.configuration = configuration;
        if (wrapper == null) {
            wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        }
        this.wrapper = wrapper;

        this.servletContext = context.getServletContext();

        if (taglibFactory == null) {
            taglibFactory = new TaglibFactory(this.servletContext);
        }
        this.taglibFactory = taglibFactory;
        this.configuration.setObjectWrapper(wrapper);
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

        final List<TemplateLoader> beans = WebUtils.getWebApplicationContext().getBeans(TemplateLoader.class);

        if (beans.isEmpty()) {
            if (StringUtils.isNotEmpty(prefix) && prefix.contains("WEB-INF")) {// prefix -> /WEB-INF/..
                this.configuration.setServletContextForTemplateLoading(servletContext, prefix); // prefix -> /WEB-INF/..
            }
            else {
                configuration.setTemplateLoader(new DefaultTemplateLoader(prefix));
            }
        }
        else {
            configuration.setTemplateLoader(new CompositeTemplateLoader(beans));
        }
        LoggerFactory.getLogger(getClass()).info("Configuration FreeMarker View Resolver Success.");
    }

    @Override
    public void configureParameterResolver(List<ParameterResolver> resolvers) {

        resolvers.add(new DelegatingParameterResolver((m) -> m.isAssignableFrom(Configuration.class), //
                (ctx, m) -> configuration//
        ));

        resolvers.add(new DelegatingParameterResolver((m) -> m.isAnnotationPresent(SharedVariable.class), (ctx, m) -> {
            final TemplateModel sharedVariable = configuration.getSharedVariable(m.getName());

            if (m.isInstance(sharedVariable)) {
                return sharedVariable;
            }

            if (sharedVariable instanceof WrapperTemplateModel) {
                final Object wrappedObject = ((WrapperTemplateModel) sharedVariable).getWrappedObject();
                if (m.isInstance(wrappedObject)) {
                    return wrappedObject;
                }
                throw ExceptionUtils.newConfigurationException(null, "Not a instance of: " + m.getParameterClass());
            }

            if (m.isRequired()) {
                throw ExceptionUtils.newConfigurationException(null, "There is no shared variable named: " + m.getName());
            }
            return ConvertUtils.convert(m.getDefaultValue(), m.getParameterClass());
        }));

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
    protected class CompositeTemplateLoader implements TemplateLoader {

        // none null
        private TemplateLoader[] loaders;

        public CompositeTemplateLoader(TemplateLoader... loaders) {
            setTemplateLoaders(loaders);
        }

        public CompositeTemplateLoader(Collection<TemplateLoader> loaders) {
            addTemplateLoaders(loaders);
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
        public CompositeTemplateLoader setTemplateLoaders(final TemplateLoader... values) {
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

        // TemplateLoader
        // -------------------------------------------

        @Override
        public Object findTemplateSource(String name) throws IOException {

            for (final TemplateLoader loader : loaders) {
                final Object source = loader.findTemplateSource(name);
                if (source != null) return source;
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

        public DefaultTemplateLoader() {}

        public DefaultTemplateLoader(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Object findTemplateSource(String name) throws IOException {

            final Resource resource;
            final String prefix = getPrefix();

            if (StringUtils.isEmpty(prefix)) {
                resource = ResourceUtils.getResource(name);
            }
            else {
                resource = ResourceUtils.getResource(prefix + name);
            }
            return resource.exists() ? resource : null;
        }

        @Override
        public long getLastModified(final Object source) {

            if (source instanceof Resource) {
                try {
                    return ((Resource) source).lastModified();
                }
                catch (IOException e) {}
            }
            return -1;
        }

        @Override
        public Reader getReader(final Object source, final String encoding) throws IOException {
            if (source instanceof Resource) {
                return ((Resource) source).getReader(encoding);
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
    }

}
