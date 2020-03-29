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
import javax.servlet.ServletSecurityElement;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.StringUtils;
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

    private String dispatcherServletMapping = Constant.DISPATCHER_SERVLET_MAPPING;

    public DispatcherServletInitializer(WebServletApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        setOrder(HIGHEST_PRECEDENCE - 100);
    }

    @Override
    public DispatcherServlet getServlet() {

        DispatcherServlet dispatcherServlet = super.getServlet();
        if (dispatcherServlet == null) {
            multipartConfig();
            addUrlMappings(StringUtils.split(dispatcherServletMapping));

            final WebServletApplicationContext applicationContext = getApplicationContext();

            if (!applicationContext.containsBeanDefinition(DispatcherServlet.class)) {
                applicationContext.registerBean(Constant.DISPATCHER_SERVLET, DispatcherServlet.class);
            }

            dispatcherServlet = applicationContext.getBean(DispatcherServlet.class);
            final Logger log = LoggerFactory.getLogger(DispatcherServletInitializer.class);

            log.info("Register Dispatcher Servlet: [{}] With Url Mappings: {}", dispatcherServlet, getUrlMappings());

            setName(Constant.DISPATCHER_SERVLET);
            setServlet(dispatcherServlet);
        }
        return dispatcherServlet;
    }

    protected void multipartConfig() {

        final WebServletApplicationContext applicationContext = getApplicationContext();

        MultipartConfigElement multipartConfig = applicationContext.getBean(MultipartConfigElement.class);

        if (multipartConfig == null) {

            final MultipartConfiguration configuration = applicationContext.getBean(MultipartConfiguration.class);

            multipartConfig = new MultipartConfigElement(configuration.getLocation(),
                                                         configuration.getMaxFileSize().toBytes(),
                                                         configuration.getMaxRequestSize().toBytes(),
                                                         (int) configuration.getFileSizeThreshold().toBytes());
        }

        if (multipartConfig != null) {
            setMultipartConfig(multipartConfig);
        }
        ServletSecurityElement securityConfig = applicationContext.getBean(ServletSecurityElement.class);

        if (securityConfig != null) {
            setServletSecurity(securityConfig);
        }
    }

}
