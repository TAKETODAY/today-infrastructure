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

import static cn.taketoday.context.exception.ConfigurationException.nonNull;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.framework.env.StandardWebEnvironment;

/**
 * @author TODAY <br>
 *         2018-10-16 15:46
 */
public class WebApplication {

    private static final Logger log = LoggerFactory.getLogger(WebApplication.class);

    private final String appBasePath = System.getProperty("user.dir");
    private ConfigurableWebServerApplicationContext applicationContext;

    public WebApplication() {
        applicationContext = ClassUtils.isPresent(Constant.ENV_SERVLET)
                ? new ServletWebServerApplicationContext()
                : new StandardWebServerApplicationContext();
    }

    public WebApplication(Class<?> startupClass) {
        applicationContext = ClassUtils.isPresent(Constant.ENV_SERVLET)
                ? new ServletWebServerApplicationContext(startupClass)
                : new StandardWebServerApplicationContext(startupClass);
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
        log.debug(getAppBasePath());

        final ConfigurableWebServerApplicationContext applicationContext = getApplicationContext();
        try {
            applicationContext.registerSingleton(this);
            final Class<?> startupClass = applicationContext.getStartupClass();
            final ConfigurableEnvironment environment = new StandardWebEnvironment(startupClass, args);
            applicationContext.setEnvironment(environment);

            applicationContext.loadContext(startupClass.getPackage().getName());

            nonNull(applicationContext.getWebServer(), "Web server can't be null")
                    .start();

            log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
                     System.currentTimeMillis() - applicationContext.getStartupDate()//
            );
        }
        catch (Throwable e) {
            e = ExceptionUtils.unwrapThrowable(e);
            applicationContext.close();
            throw new ConfigurationException("Your Application Initialized ERROR: [" + e + "]", e);
        }
        return applicationContext;
    }

    public String getAppBasePath() {
        return appBasePath;
    }

}
