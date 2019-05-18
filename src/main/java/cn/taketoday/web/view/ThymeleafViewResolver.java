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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.factory.InitializingBean;

/**
 * 
 * @author Today <br>
 *         2018-06-26 11:26:01
 */
public class ThymeleafViewResolver extends AbstractViewResolver implements InitializingBean {

    private final TemplateEngine templateEngine;

    @Value(value = "#{thymeleaf.cacheable}", required = false)
    private boolean cacheable = true;

    public ThymeleafViewResolver() {
        templateEngine = new TemplateEngine();
    }

    /**
     * Init Thymeleaf View Resolver.
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);

        templateResolver.setPrefix(prefix);
        templateResolver.setSuffix(suffix);
        templateResolver.setCacheable(cacheable);
        templateResolver.setCharacterEncoding(encoding);
        templateResolver.setTemplateMode(TemplateMode.HTML);

        templateEngine.setTemplateResolver(templateResolver);

        LoggerFactory.getLogger(getClass()).info("Configuration Thymeleaf View Resolver Success.");
    }

    /**
     * resolve Thymeleaf View.
     */
    @Override
    public void resolveView(String templateName, //
            HttpServletRequest request, HttpServletResponse response) throws Throwable //
    {
        templateEngine.process(templateName, //
                new WebContext(request, response, servletContext, locale), response.getWriter());
    }

}
