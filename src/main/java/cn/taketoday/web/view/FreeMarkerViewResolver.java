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

import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.utils.WebUtils;
import freemarker.cache.TemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import lombok.Getter;

/**
 * 
 * @author TODAY <br>
 *         2018-06-26 19:16:46
 */
@WebDebugMode
@Props(prefix = "web.mvc.view.")
@Singleton(Constant.VIEW_RESOLVER)
public class FreeMarkerViewResolver extends AbstractViewResolver implements InitializingBean {

    private final ObjectWrapper wrapper;

    @Getter
    private final Configuration configuration;
    private final TaglibFactory taglibFactory;
    private final TemplateLoader templateLoader;
    private final ServletContextHashModel applicationModel;

    public FreeMarkerViewResolver(Configuration configuration, //
            TaglibFactory taglibFactory, TemplateLoader templateLoader, Properties settings) //
    {
        this(new DefaultObjectWrapper(Configuration.VERSION_2_3_28), //
                configuration, taglibFactory, templateLoader, settings);
    }

    @Autowired
    public FreeMarkerViewResolver(//
            @Autowired(required = false) ObjectWrapper wrapper, //
            @Autowired(required = false) Configuration configuration, //
            @Autowired(required = false) TaglibFactory taglibFactory, //
            @Autowired(required = false) TemplateLoader templateLoader, //
            @Props(prefix = "freemarker.", replace = true) Properties settings) //
    {
        WebApplicationContext webApplicationContext = WebUtils.getWebApplicationContext();
        if (configuration == null) {
            configuration = new Configuration(Configuration.VERSION_2_3_28);
            webApplicationContext.registerSingleton(configuration.getClass().getName(), configuration);
        }

        this.configuration = configuration;
        if (wrapper == null) {
            wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        }
        this.wrapper = wrapper;
        ServletContext servletContext = webApplicationContext.getServletContext();
        if (taglibFactory == null) {
            taglibFactory = new TaglibFactory(servletContext);
        }
        this.taglibFactory = taglibFactory;
        this.configuration.setObjectWrapper(wrapper);
        // Create hash model wrapper for servlet context (the application)
        this.applicationModel = new ServletContextHashModel(servletContext, wrapper);

        final Map<String, TemplateModel> templateModels = webApplicationContext.getBeansOfType(TemplateModel.class);

        templateModels.forEach(configuration::setSharedVariable);

        this.templateLoader = templateLoader;
        try {
            if (settings != null && !settings.isEmpty()) {
                this.configuration.setSettings(settings);
            }
        }
        catch (TemplateException e) {
            throw new ConfigurationException("Set FreeMarker's Properties Error, With Msg: [" + e.getMessage() + "]", e);
        }
    }

    /**
     * Use {@link afterPropertiesSet}
     * 
     * @since 2.3.3
     */
    @Override
    public void afterPropertiesSet() throws ConfigurationException {

        this.configuration.setLocale(locale);
        this.configuration.setDefaultEncoding(encoding);
        if (templateLoader == null) {
            this.configuration.setServletContextForTemplateLoading(servletContext, prefix); // prefix -> /WEB-INF/..
        }
        else {
            configuration.setTemplateLoader(templateLoader);
        }
        LoggerFactory.getLogger(getClass()).info("Configuration FreeMarker View Resolver Success.");
    }

    /**
     * create Model Attributes.
     * 
     * @param request
     * @return
     */
    protected final TemplateHashModel createModel(HttpServletRequest request) {
        final ObjectWrapper wrapper = this.wrapper;

        final AllHttpScopesHashModel allHttpScopesHashModel = //
                new AllHttpScopesHashModel(wrapper, servletContext, request);

        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, taglibFactory);
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, applicationModel);
        // Create hash model wrapper for request
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, wrapper));
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
        // Create hash model wrapper for session
        allHttpScopesHashModel.putUnlistedModel(FreemarkerServlet.KEY_SESSION, new HttpSessionHashModel(request.getSession(), wrapper));

        return allHttpScopesHashModel;
    }

    /**
     * Resolve FreeMarker View.
     */
    @Override
    public void resolveView(String templateName, //
            HttpServletRequest request, HttpServletResponse response) throws Throwable //
    {
        configuration.getTemplate(templateName + suffix, locale, encoding)//
                .process(createModel(request), response.getWriter());
    }

}
