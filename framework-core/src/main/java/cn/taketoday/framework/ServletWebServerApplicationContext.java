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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanPostProcessor;
import cn.taketoday.framework.aware.WebApplicationContextAwareProcessor;
import cn.taketoday.framework.aware.WebServerApplicationContextAware;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.framework.server.ConfigurableWebServer;
import cn.taketoday.framework.server.WebServer;
import cn.taketoday.web.DefaultWebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.config.initializer.OrderedInitializer;

/**
 * @author Today <br>
 * 
 *         2019-01-17 15:54
 */
public class ServletWebServerApplicationContext extends DefaultWebApplicationContext implements WebServerApplicationContext {

    private WebServer webServer;

    private final Class<?> startupClass;

    public ServletWebServerApplicationContext(Class<?> startupClass) {
        this.startupClass = startupClass;
    }

    public ServletWebServerApplicationContext() {
        this(null);
    }

    @Override
    protected void onRefresh() throws Throwable {

        // disable web mvc xml
        getEnvironment().setProperty(Constant.ENABLE_WEB_MVC_XML, "false");

        final BeanPostProcessor beanPostProcessor = new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
                if (bean instanceof Aware) {
                    if (bean instanceof WebServerApplicationContextAware) {
                        ((WebServerApplicationContextAware) bean)//
                                .setWebServerApplicationContext(ServletWebServerApplicationContext.this);
                    }
                    if (bean instanceof WebApplicationContextAware) {
                        ((WebApplicationContextAware) bean).setWebApplicationContext(ServletWebServerApplicationContext.this);
                    }
                }
                return bean;
            }
        };
        addBeanPostProcessor(beanPostProcessor);

        LoggerFactory.getLogger(getClass()).info("Looking For: [{}]", WebServer.class.getName());

        // Get WebServer instance
        this.webServer = getBean(WebServer.class);
        if (this.webServer == null) {
            throw new ConfigurationException("The context doesn't exist a [cn.taketoday.framework.server.WebServer] bean");
        }

        if (this.webServer instanceof ConfigurableWebServer) {
            if (this.webServer instanceof AbstractWebServer) {

                ((AbstractWebServer) webServer).getWebApplicationConfiguration()//
                        .configureWebServer((AbstractWebServer) webServer);
            }

            ((ConfigurableWebServer) webServer).initialize(getContextInitializer());
        }
        super.onRefresh();
        removeBeanPostProcessor(beanPostProcessor);
    }

    private OrderedInitializer getContextInitializer() {
        return new OrderedInitializer() {

            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {

                ServletWebServerApplicationContext.this.getBeanFactory().addBeanPostProcessor(//
                        new WebApplicationContextAwareProcessor(servletContext, ServletWebServerApplicationContext.this)//
                );

                prepareServletContext(servletContext);

                new WebApplicationLoader().onStartup(null, servletContext);
            }

            @Override
            public int getOrder() {
                return Ordered.LOWEST_PRECEDENCE;
            }
        };
    }

    /**
     * @param servletContext
     */
    protected void prepareServletContext(ServletContext servletContext) {

        setServletContext(servletContext);
        final Object attribute = servletContext.getAttribute(Constant.KEY_WEB_APPLICATION_CONTEXT);

        if (attribute == null) {
            LoggerFactory.getLogger(getClass()).info("ServletContext: [{}] Configure Success.", servletContext);
            servletContext.setAttribute(Constant.KEY_WEB_APPLICATION_CONTEXT, this);
        }
    }

    @Override
    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public Class<?> getStartupClass() {
        return startupClass;
    }

}
