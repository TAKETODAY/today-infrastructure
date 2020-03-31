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

import static cn.taketoday.web.resolver.method.DelegatingParameterResolver.delegate;

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
import cn.taketoday.web.resolver.method.ParameterResolver;
import freemarker.cache.TemplateLoader;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * @author TODAY <br>
 *         2019-11-22 13:25
 */
public abstract class AbstractFreeMarkerTemplateViewResolver
        extends AbstractTemplateViewResolver implements WebMvcConfiguration {

    protected int cacheSize = 1024;
    private ObjectWrapper wrapper;
    private Configuration configuration;

    public AbstractFreeMarkerTemplateViewResolver() {
        setSuffix(".ftl");
    }

    protected void configConfiguration(WebApplicationContext context) {
        Configuration config = configuration;
        if (config == null) {
            config = configuration = new Configuration(Configuration.VERSION_2_3_28);
            context.registerSingleton(config.getClass().getName(), config);
        }
        context.getBeansOfType(TemplateModel.class)
                .forEach(config::setSharedVariable);

        config.setLocale(locale);
        config.setDefaultEncoding(encoding);
    }

    protected void configObjectWrapper() {
        if (getWrapper() == null) {
            setWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_28));
        }
        getConfiguration().setObjectWrapper(getWrapper());
    }

    @PostConstruct
    public void initFreeMarker(WebApplicationContext context, @Props(prefix = "freemarker.", replace = true) Properties settings) {

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

        final Logger log = LoggerFactory.getLogger(AbstractFreeMarkerTemplateViewResolver.class);
        if (log.isInfoEnabled()) {
            log.info("FreeMarker use [{}] to load templates, prefix: [{}], suffix: [{}]", loader, prefix, suffix);
            log.info("Configuration FreeMarker Template View Resolver Success.");
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
    public void resolveView(final String template, final RequestContext context) throws Throwable {

        getConfiguration().getTemplate(template, locale, encoding)
                .process(createModel(context), context.getWriter());
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public ObjectWrapper getWrapper() {
        return wrapper;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setWrapper(ObjectWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
