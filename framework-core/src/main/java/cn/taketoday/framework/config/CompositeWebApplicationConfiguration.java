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
package cn.taketoday.framework.config;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.framework.bean.ErrorPage;
import cn.taketoday.framework.bean.MimeMappings;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.web.ServletContextInitializer;

/**
 * @author TODAY <br>
 *         2019-06-18 17:36
 */
public class CompositeWebApplicationConfiguration implements WebApplicationConfiguration {

    private final List<WebApplicationConfiguration> webApplicationConfigurations;

    public CompositeWebApplicationConfiguration(List<WebApplicationConfiguration> applicationConfigurations) {
        this.webApplicationConfigurations = applicationConfigurations;
    }

    @Override
    public void configureCompression(CompressionConfiguration compressionConfiguration) {

        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureCompression(compressionConfiguration);
        }
    }

    @Override
    public void configureDefaultServlet(DefaultServletConfiguration defaultServletConfiguration) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureDefaultServlet(defaultServletConfiguration);
        }
    }

    @Override
    public void configureJspServlet(JspServletConfiguration jspServletConfiguration) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureJspServlet(jspServletConfiguration);
        }
    }

    @Override
    public void configureSession(SessionConfiguration sessionConfiguration) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureSession(sessionConfiguration);
        }
    }

    @Override
    public void configureWebServer(AbstractWebServer webServer) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureWebServer(webServer);
        }
    }

    @Override
    public void configureErrorPages(Set<ErrorPage> errorPages) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureErrorPages(errorPages);
        }
    }

    @Override
    public void configureMimeMappings(MimeMappings mimeMappings) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureMimeMappings(mimeMappings);
        }
    }

    @Override
    public void configureWelcomePages(Set<String> welcomePages) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureWelcomePages(welcomePages);
        }
    }

    @Override
    public void configureLocaleCharsetMapping(Map<Locale, Charset> localeCharsetMappings) {
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureLocaleCharsetMapping(localeCharsetMappings);
        }
    }

    @Override
    public void configureServletContextInitializer(List<ServletContextInitializer> initializer) {
        
        for (WebApplicationConfiguration webApplicationConfiguration : webApplicationConfigurations) {
            webApplicationConfiguration.configureServletContextInitializer(initializer);
        }
    }
}
