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
package cn.taketoday.web.config.initializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletSecurityElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.MultipartResolver;
import cn.taketoday.web.servlet.DispatcherServlet;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-03 14:08
 */
@Setter
@Getter
@MissingBean
public class DispatcherServletInitializer extends WebServletInitializer<DispatcherServlet> implements WebApplicationContextAware {

    private WebApplicationContext applicationContext;

    private String dispatcherServletMapping = Constant.DISPATCHER_SERVLET_MAPPING;

    @Override
    public void setWebApplicationContext(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public DispatcherServlet getServlet() {

        DispatcherServlet dispatcherServlet = super.getServlet();
        if (dispatcherServlet == null) {

            multipartConfig();

            addUrlMappings(StringUtils.split(dispatcherServletMapping));

            if (!applicationContext.containsBeanDefinition(Constant.DISPATCHER_SERVLET)) {
                applicationContext.registerBean(Constant.DISPATCHER_SERVLET, DispatcherServlet.class);
            }
            dispatcherServlet = applicationContext.getBean(Constant.DISPATCHER_SERVLET, DispatcherServlet.class);
            final Logger log = LoggerFactory.getLogger(DispatcherServletInitializer.class);

            log.info("Register Dispatcher Servlet: [{}] With Url Mappings: {}", dispatcherServlet, getUrlMappings());

            setName(Constant.DISPATCHER_SERVLET);
            setServlet(dispatcherServlet);
        }
        return dispatcherServlet;
    }

    /**
     * 
     */
    private void multipartConfig() {

        MultipartResolver multipartResolver = //
                applicationContext.getBean(Constant.MULTIPART_RESOLVER, MultipartResolver.class);

        MultipartConfigElement multipartConfig = //
                applicationContext.getBean(Constant.MULTIPART_CONFIG_ELEMENT, MultipartConfigElement.class);

        if (multipartResolver instanceof AbstractMultipartResolver) {

            AbstractMultipartResolver abstractMultipartResolver = (AbstractMultipartResolver) multipartResolver;
            multipartConfig = new MultipartConfigElement(//
                    abstractMultipartResolver.getLocation(), //
                    abstractMultipartResolver.getMaxFileSize(), //
                    abstractMultipartResolver.getMaxRequestSize(), //
                    abstractMultipartResolver.getFileSizeThreshold()//
            );
        }

        if (multipartConfig != null) {
            setMultipartConfig(multipartConfig);
        }
        ServletSecurityElement securityConfig = //
                applicationContext.getBean(Constant.SERVLET_SECURITY_ELEMENT, ServletSecurityElement.class);

        if (securityConfig != null) {
            setServletSecurity(securityConfig);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE - 100;
    }

}
