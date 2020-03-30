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
package cn.taketoday.web.servlet.initializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.Constant;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-03 14:08
 */
@Setter
@Getter
public class DispatcherServletInitializer extends WebServletInitializer<DispatcherServlet> {

    private final WebServletApplicationContext applicationContext;

    public DispatcherServletInitializer(WebServletApplicationContext context) {
        this(context, null);
    }

    public DispatcherServletInitializer(WebServletApplicationContext context, DispatcherServlet dispatcherServlet) {
        super(dispatcherServlet);
        this.applicationContext = context;
        setOrder(HIGHEST_PRECEDENCE - 100);
        setName(Constant.DISPATCHER_SERVLET);
        addUrlMappings(Constant.DISPATCHER_SERVLET_MAPPING);
    }

    @Override
    public DispatcherServlet getServlet() {
        DispatcherServlet dispatcherServlet = super.getServlet();
        if (dispatcherServlet == null) {
            final WebServletApplicationContext applicationContext = getApplicationContext();
            if (!applicationContext.containsBeanDefinition(DispatcherServlet.class)) {
                applicationContext.registerBean(Constant.DISPATCHER_SERVLET, DispatcherServlet.class);
            }
            dispatcherServlet = applicationContext.getBean(DispatcherServlet.class);
            setServlet(dispatcherServlet);
        }
        return dispatcherServlet;
    }

    @Override
    protected void configureRegistration(Dynamic registration) {
        super.configureRegistration(registration);

        final Logger log = LoggerFactory.getLogger(DispatcherServletInitializer.class);
        log.info("Register Dispatcher Servlet: [{}] With Url Mappings: {}", getServlet(), getUrlMappings());
    }

    @Override
    protected void configureMultipart(Dynamic registration) {
        MultipartConfigElement multipartConfig = getMultipartConfig();
        if (multipartConfig == null) {
            final WebServletApplicationContext context = getApplicationContext();
            multipartConfig = context.getBean(MultipartConfigElement.class);
            if (multipartConfig == null) {
                final MultipartConfiguration configuration = context.getBean(MultipartConfiguration.class);
                multipartConfig = new MultipartConfigElement(configuration.getLocation(),
                                                             configuration.getMaxFileSize().toBytes(),
                                                             configuration.getMaxRequestSize().toBytes(),
                                                             (int) configuration.getFileSizeThreshold().toBytes());
            }
        }

        if (multipartConfig != null) {
            setMultipartConfig(multipartConfig);
        }
    }

    @Override
    protected void configureServletSecurity(Dynamic registration) {

        ServletSecurityElement securityConfig = getApplicationContext().getBean(ServletSecurityElement.class);
        if (securityConfig != null) {
            setServletSecurity(securityConfig);
        }
    }

}
