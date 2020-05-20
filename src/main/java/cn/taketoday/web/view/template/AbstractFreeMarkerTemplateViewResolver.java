/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.web.resolver.DelegatingParameterResolver.delegate;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.SharedVariable;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.resolver.ParameterResolver;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.Version;

/**
 * @author TODAY <br>
 *         2019-11-22 13:25
 */
public abstract class AbstractFreeMarkerTemplateViewResolver
        extends AbstractTemplateViewResolver implements WebMvcConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AbstractFreeMarkerTemplateViewResolver.class);

    protected int cacheSize = 1024;
    private ObjectWrapper objectWrapper;
    private Configuration configuration;

    public AbstractFreeMarkerTemplateViewResolver() {
        setSuffix(".ftl");
    }

    protected void configConfiguration(WebApplicationContext context) {
        Configuration config = configuration;
        if (config == null) {
            config = configuration = new Configuration(freemakerVersion());
            context.registerSingleton(config.getClass().getName(), config);
        }
        log.info("Configure FreeMarker TemplateModel");
        context.getBeansOfType(TemplateModel.class)
                .forEach(config::setSharedVariable);

        config.setLocale(locale);
        config.setDefaultEncoding(encoding);
    }

    protected Version freemakerVersion() {
        return Configuration.VERSION_2_3_28;
    }

    protected void configObjectWrapper() {
        if (getObjectWrapper() == null) {
            setObjectWrapper(new DefaultObjectWrapper(freemakerVersion()));
        }
        getConfiguration().setObjectWrapper(getObjectWrapper());
    }

    @PostConstruct
    public void initFreeMarker(WebApplicationContext context, @Props(prefix = "freemarker.", replace = true) Properties settings) {

        log.info("Initialize FreeMarker");

        configConfiguration(context);
        configObjectWrapper();

        try {
            if (ObjectUtils.isNotEmpty(settings)) {
                getConfiguration().setSettings(settings);
            }
        }
        catch (TemplateException e) {
            throw new ConfigurationException("Set FreeMarker's Properties Error, With: [" + e + "]", e);
        }
        log.info("Configuration FreeMarker Template View Resolver Success.");
    }

    @Override
    public void configureParameterResolver(List<ParameterResolver> resolvers) {

        resolvers.add(delegate((m) -> m.isAssignableFrom(Configuration.class), (ctx, m) -> getConfiguration()));
        resolvers.add(delegate((m) -> m.isAnnotationPresent(SharedVariable.class), (ctx, m) -> {
            final TemplateModel sharedVariable = getConfiguration().getSharedVariable(m.getName());

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
                throw new ConfigurationException("There is no shared variable named: ".concat(m.getName()));
            }
            return ConvertUtils.convert(m.getDefaultValue(), m.getParameterClass());
        }, Ordered.LOWEST_PRECEDENCE + 10));
    }

    @Override
    public <T> void configureTemplateLoader(List<T> loaders) {
        final TemplateLoader loader = createTemplateLoader(loaders);
        getConfiguration().setTemplateLoader(loader);

        if (log.isInfoEnabled()) {
            log.info("FreeMarker use [{}] to load templates, prefix: [{}], suffix: [{}]", loader, prefix, suffix);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> TemplateLoader createTemplateLoader(List<T> loaders) {

        return ObjectUtils.isEmpty(loaders)
                ? new DefaultResourceTemplateLoader(prefix, suffix, cacheSize)
                : new CompositeTemplateLoader((Collection<TemplateLoader>) loaders, cacheSize);
    }

    /**
     * Create Model Attributes.
     * 
     * @param context
     *            Current request context
     * @return {@link TemplateHashModel}
     */
    protected abstract TemplateHashModel createModel(RequestContext context);

    @Override
    public void resolveView(final String name, final RequestContext context) throws Throwable {

        final Template template = getConfiguration().getTemplate(name, locale, encoding);
        final TemplateHashModel model = createModel(context);

        // Give subclasses a chance to hook into preprocessing
        if (preTemplateProcess(template, model, context)) {
            try {
                // Process the template
                final Environment env = template.createProcessingEnvironment(model, context.getWriter());
                env.setOutputEncoding(encoding);
                processEnvironment(env, context);
            }
            finally {
                // Give subclasses a chance to hook into postprocessing
                postTemplateProcess(template, model, context);
            }
        }
    }

    /**
     * Called before the execution is passed to
     * {@link Template#process(Object, java.io.Writer)}. This is a generic hook you
     * might use in subclasses to perform a specific action before the template is
     * processed.
     *
     * @param context
     *            The HTTP response. The HTTP headers are already initialized here,
     *            such as the {@code contentType} and the
     *            {@code responseCharacterEncoding} are already set, but you can do
     *            the final adjustments here. The response {@link Writer} isn't
     *            created yet, so changing HTTP headers and buffering parameters
     *            works.
     * @param template
     *            The template that will get executed
     * @param model
     *            The data model that will be passed to the template. By default
     *            this will be an
     *            {@link freemarker.ext.servlet.AllHttpScopesHashModel} (which is a
     *            {@link freemarker.template.SimpleHash} subclass). Thus, you can
     *            add new variables to the data-model with the
     *            {@link freemarker.template.SimpleHash#put(String, Object)}
     *            subclass) method. However, to adjust the data-model, overriding
     *            {@link #createModel(RequestContext)} is probably a more
     *            appropriate place.
     * 
     * @return true to process the template, false to suppress template processing.
     */
    protected boolean preTemplateProcess(final Template template, final TemplateModel model, final RequestContext context)
            throws IOException {
        return true;
    }

    /**
     * This is the method that actually executes the template. The original
     * implementation coming from {@link freemarker.ext.servlet.FreemarkerServlet}
     * simply calls {@link Environment#process()}. Overriding this method allows you
     * to prepare the {@link Environment} before the execution, or extract
     * information from the {@link Environment} after the execution. It also allows
     * you to capture exceptions throw by the template.
     * 
     * @param env
     *            The {@link Environment} object already set up to execute the
     *            template. You only have to call {@link Environment#process()} and
     *            the output will be produced by the template.
     * 
     * @since 2.3.7
     */
    protected void processEnvironment(final Environment env, final RequestContext context)
            throws TemplateException, IOException {
        env.process();
    }

    /**
     * Called after the execution returns from
     * {@link Template#process(Object, java.io.Writer)}. This is a generic hook you
     * might use in subclasses to perform a specific action after the template is
     * processed. It will be invoked even if the template processing throws an
     * exception. By default does nothing.
     * 
     * @param request
     *            the actual HTTP request context
     * @param template
     *            the template that was executed
     * @param data
     *            the data that was passed to the template
     * @since 2.3.7
     */
    protected void postTemplateProcess(final Template template, final TemplateModel data, final RequestContext context)
            throws IOException {}

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setObjectWrapper(ObjectWrapper wrapper) {
        this.objectWrapper = wrapper;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
