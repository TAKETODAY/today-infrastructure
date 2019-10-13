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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.framework.annotation.ComponentScan;
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

    private static ServletWebServerApplicationContext applicationContext;

    private Class<?> startupClass;

    private final String appBasePath = System.getProperty("user.dir");

    public WebApplication() {
        applicationContext = new ServletWebServerApplicationContext();
    }

    public WebApplication(Class<?> startupClass) {
        applicationContext = new ServletWebServerApplicationContext(startupClass);
        this.setStartupClass(Objects.requireNonNull(startupClass));
    }

    public static ServletWebServerApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Startup Web Application
     * 
     * @param startupClass
     *            Startup class
     * @param args
     *            Startup arguments
     * 
     * @return
     */
    public static WebServerApplicationContext run(Class<?> startupClass, String... args) {
        return new WebApplication(startupClass).run(args);
    }

    /**
     * Startup Web Application
     * 
     * @param args
     *            Startup arguments
     * @return {@link WebServerApplicationContext}
     */
    public WebServerApplicationContext run(String... args) {

        final ServletWebServerApplicationContext applicationContext = getApplicationContext();

        try {

            log.debug(appBasePath);

            applicationContext.registerSingleton(WebApplication.class.getName(), this);

            final Class<?> startupClass = getStartupClass();

            final ConfigurableEnvironment environment = new StandardWebEnvironment(startupClass, args);

            applicationContext.setEnvironment(environment);

            ComponentScan componentScan = startupClass.getAnnotation(ComponentScan.class);
            if (componentScan != null) {
                // bean name creator
                final BeanNameCreator beanNameCreator;
                final Class<? extends BeanNameCreator> nameCreator = componentScan.nameCreator();
                if (nameCreator == DefaultBeanNameCreator.class) {
                    beanNameCreator = new DefaultBeanNameCreator(true);
                }
                else {
                    beanNameCreator = ClassUtils.newInstance(nameCreator, applicationContext);
                }

                applicationContext.getBeanFactory().setBeanNameCreator(beanNameCreator);
                applicationContext.loadContext(componentScan.value());
            }
            else {
                applicationContext.loadContext(startupClass.getPackage().getName());
            }

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

    /**
     * Apply startup class
     * 
     * @param startupClass
     *            Startup class such as Application or XXXApplication
     */
    public void setStartupClass(Class<?> startupClass) {
        this.startupClass = startupClass;
    }

}
