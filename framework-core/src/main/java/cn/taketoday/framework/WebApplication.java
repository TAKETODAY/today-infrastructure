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

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.framework.annotation.ComponentScan;
import cn.taketoday.framework.env.StandardWebServletEnvironment;
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

    /** default mapping */
    private String dispatcherServletMapping = Constant.DISPATCHER_SERVLET_MAPPING;

    private Class<?> applicationClass;

    private String appBasePath = System.getProperty("user.dir");

    public WebApplication() {
        applicationContext = new ServletWebServerApplicationContext();
    }

    public static ServletWebServerApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 
     * @param application
     */
    public WebApplication(Class<?> application) {
        applicationContext = new ServletWebServerApplicationContext(application);
        this.applicationClass = application;
    }

    public static WebServerApplicationContext run(Class<?> application, String... args) {
        return new WebApplication(application).run(args);
    }

    /**
     * 
     * @param args
     * @return
     */
    public WebServerApplicationContext run(String... args) {

        ServletWebServerApplicationContext applicationContext = getApplicationContext();

        try {

            log.debug(appBasePath);

            applicationContext.registerSingleton(WebApplication.class.getName(), this);

            ConfigurableEnvironment environment = new StandardWebServletEnvironment(applicationClass);
            applicationContext.setEnvironment(environment);

            ComponentScan componentScan = applicationClass.getAnnotation(ComponentScan.class);
            if (componentScan != null) {
                // bean name creator
                final BeanNameCreator beanNameCreator;
                final Class<? extends BeanNameCreator> nameCreator = componentScan.nameCreator();
                if (nameCreator == DefaultBeanNameCreator.class) {
                    beanNameCreator = new DefaultBeanNameCreator(environment);
                }
                else {
                    // use default constructor
                    beanNameCreator = (BeanNameCreator) ClassUtils.newInstance(nameCreator, applicationContext);
                }
                environment.setBeanNameCreator(beanNameCreator);
                applicationContext.loadContext(componentScan.value());
            }
            else {
                applicationContext.loadContext(applicationClass.getPackage().getName());
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
            log.error("Your Application Initialized ERROR: [{}]", e.getMessage(), e);
            throw new ConfigurationException(e);
        }
        return applicationContext;
    }

}
