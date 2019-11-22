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
package cn.taketoday.web.view;

import static cn.taketoday.web.resolver.method.DelegatingParameterResolver.delegate;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.RequestContext;
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
public abstract class AbstractFreeMarkerViewResolver
        extends AbstractViewResolver implements InitializingBean, WebMvcConfiguration {

    protected int cacheSize = 1024;
    protected final ObjectWrapper wrapper;
    protected final Configuration configuration;

    public AbstractFreeMarkerViewResolver(ObjectWrapper wrapper, Configuration configuration, Properties settings) {

        this.wrapper = configObjectWrapper(wrapper);
        this.configuration = configConfiguration(configuration);
        this.configuration.setObjectWrapper(this.wrapper);

        ContextUtils.getApplicationContext()
                .getBeansOfType(TemplateModel.class).forEach(configuration::setSharedVariable);

        try {
            if (ObjectUtils.isNotEmpty(settings)) {
                configuration.setSettings(settings);
            }
        }
        catch (TemplateException e) {
            throw new ConfigurationException("Set FreeMarker's Properties Error, With: [" + e + "]", e);
        }
    }

    protected Configuration configConfiguration(final Configuration configuration) {
        Configuration configurationToUse = configuration;
        if (configurationToUse == null) {
            configurationToUse = new Configuration(Configuration.VERSION_2_3_28);
            ContextUtils.getApplicationContext()
                    .registerSingleton(configurationToUse.getClass().getName(), configurationToUse);
        }
        return configuration;
    }

    protected ObjectWrapper configObjectWrapper(final ObjectWrapper wrapper) {
        return wrapper != null ? wrapper : new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
    }

    /**
     * Use {@link afterPropertiesSet}
     * 
     * @since 2.3.3
     */
    @Override
    public void afterPropertiesSet() {
        if (configuration == null) {
            throw new ConfigurationException("'freemarker.template.Configuration' can't be null");
        }
        configuration.setLocale(locale);
        configuration.setDefaultEncoding(encoding);
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
    public <T> void configureTemplateLoader(List<T> loaders) {

        final TemplateLoader loader = createTemplateLoader(loaders);

        configuration.setTemplateLoader(loader);

        final Logger logger = LoggerFactory.getLogger(getClass());
        logger.info("FreeMarker use [{}] to load templates", loader);
        logger.info("Configuration FreeMarker View Resolver Success.");
    }

    @SuppressWarnings("unchecked")
    protected <T> TemplateLoader createTemplateLoader(List<T> loaders) {

        return loaders.isEmpty()
                ? new DefaultResourceTemplateLoader(prefix, cacheSize)
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

        configuration.getTemplate(template.concat(suffix), locale, encoding)
                .process(createModel(context), context.getWriter());
    }

    public final int getCacheSize() {
        return cacheSize;
    }

    public final void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public final ObjectWrapper getWrapper() {
        return wrapper;
    }

    public final Configuration getConfiguration() {
        return configuration;
    }

}
