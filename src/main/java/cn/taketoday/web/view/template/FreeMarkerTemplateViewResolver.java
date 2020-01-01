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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.condition.ConditionalOnClass;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.config.WebMvcConfiguration;
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
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;

/**
 * @author TODAY <br>
 *         2018-06-26 19:16:46
 */
@Props(prefix = "web.mvc.view.")
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@MissingBean(value = Constant.VIEW_RESOLVER, type = TemplateViewResolver.class)
@ConditionalOnClass({ Constant.ENV_SERVLET, "freemarker.template.Configuration" })
public class FreeMarkerTemplateViewResolver
        extends AbstractFreeMarkerTemplateViewResolver implements InitializingBean, WebMvcConfiguration {

    private final TaglibFactory taglibFactory;
    private final ServletContext servletContext;
    private final ServletContextHashModel applicationModel;

    @Autowired
    public FreeMarkerTemplateViewResolver(
            @Autowired(required = false) ObjectWrapper wrapper,
            @Autowired(required = false) Configuration configuration,
            @Autowired(required = false) TaglibFactory taglibFactory,
            @Props(prefix = "freemarker.", replace = true) Properties settings)//
    {
        super(wrapper, configuration, settings);
        final WebServletApplicationContext context = //
                (WebServletApplicationContext) ContextUtils.getApplicationContext();

        this.servletContext = context.getServletContext();
        this.taglibFactory = taglibFactory != null ? taglibFactory : new TaglibFactory(this.servletContext);

        // Create hash model wrapper for servlet context (the application)
        this.applicationModel = new ServletContextHashModel(this.servletContext, wrapper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> TemplateLoader createTemplateLoader(List<T> loaders) {

        if (loaders.isEmpty()) {
            if (StringUtils.isNotEmpty(prefix) && prefix.startsWith("/WEB-INF/")) {// prefix -> /WEB-INF/..
                return new WebappTemplateLoader(servletContext, prefix);
            }
            return new DefaultResourceTemplateLoader(prefix, cacheSize);
        }
        return new CompositeTemplateLoader((Collection<TemplateLoader>) loaders, cacheSize);
    }

    /**
     * Create Model Attributes.
     * 
     * @param context
     *            Current request context
     * @return {@link TemplateHashModel}
     */
    @Override
    protected TemplateHashModel createModel(final RequestContext context) {

        final ObjectWrapper wrapper = this.getWrapper();

        final HttpServletRequest request = context.nativeRequest();

        final AllHttpScopesHashModel ret = //
                new AllHttpScopesHashModel(wrapper, servletContext, request);

        ret.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, this.taglibFactory);
        ret.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, applicationModel);
        // Create hash model wrapper for request
        ret.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, wrapper));
        ret.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
        // Create hash model wrapper for session
        ret.putUnlistedModel(FreemarkerServlet.KEY_SESSION,
                             new HttpSessionHashModel(context.nativeSession(), wrapper));
        return ret;
    }

    public final TaglibFactory getTaglibFactory() {
        return taglibFactory;
    }

    public final ServletContext getServletContext() {
        return servletContext;
    }

    public final ServletContextHashModel getApplicationModel() {
        return applicationModel;
    }

}
