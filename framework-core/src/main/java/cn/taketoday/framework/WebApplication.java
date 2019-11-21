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
package cn.taketoday.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.framework.env.StandardWebEnvironment;
import cn.taketoday.framework.server.WebServer;
import lombok.Getter;

/**
 * @author TODAY <br>
 *         2018-10-16 15:46
 */
@Getter
public class WebApplication {

    private static final Logger log = LoggerFactory.getLogger(WebApplication.class);

    private final String appBasePath = System.getProperty("user.dir");
    private ConfigurableWebServerApplicationContext applicationContext;

    public WebApplication() {
        if (ClassUtils.isPresent(Constant.ENV_SERVLET)) {
            applicationContext = new ServletWebServerApplicationContext();
        }
        else {
            applicationContext = new StandardWebServerApplicationContext();
        }
    }

    public WebApplication(Class<?> startupClass) {
        if (ClassUtils.isPresent(Constant.ENV_SERVLET)) {
            applicationContext = new ServletWebServerApplicationContext(startupClass);
        }
        else {
            applicationContext = new StandardWebServerApplicationContext(startupClass);
        }
    }

    public ConfigurableWebServerApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Startup Web Application
     * 
     * @param startupClass
     *            Startup class
     * @param args
     *            Startup arguments
     */
    public static ConfigurableWebServerApplicationContext run(Class<?> startupClass, String... args) {
        return new WebApplication(startupClass).run(args);
    }

    /**
     * Startup Web Application
     * 
     * @param args
     *            Startup arguments
     * @return {@link WebServerApplicationContext}
     */
    public ConfigurableWebServerApplicationContext run(String... args) {
        log.debug(appBasePath);

        final ConfigurableWebServerApplicationContext applicationContext = getApplicationContext();

        try {

            applicationContext.registerSingleton(this);

            final Class<?> startupClass = applicationContext.getStartupClass();

            final ConfigurableEnvironment environment = new StandardWebEnvironment(startupClass, args);

            applicationContext.setEnvironment(environment);

            applicationContext.loadContext(startupClass.getPackage().getName());

            final WebServer webServer = applicationContext.getWebServer();
            if (webServer == null) {
                throw new ConfigurationException("Web server can't be null");
            }

            webServer.start();

            log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
                     System.currentTimeMillis() - applicationContext.getStartupDate()//
            );
        }
        catch (Throwable e) {
            e = ExceptionUtils.unwrapThrowable(e);
            applicationContext.close();
            log.error("Your Application Initialized ERROR: [{}]", e.toString(), e);
            throw ExceptionUtils.newConfigurationException(e);
        }
        return applicationContext;
    }

}
