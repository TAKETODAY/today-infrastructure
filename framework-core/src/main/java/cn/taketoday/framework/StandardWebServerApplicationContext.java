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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.framework;

import java.util.List;
import java.util.Map;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.factory.StandardBeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.framework.server.WebServer;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.web.StandardWebBeanFactory;

/**
 * @author TODAY <br>
 *         2019-11-13 22:07
 */
public class StandardWebServerApplicationContext
        extends StandardApplicationContext
        implements ConfigurableWebServerApplicationContext, WebServerApplicationContext {

    private WebServer webServer;
    private final Class<?> startupClass;
    private String contextPath = Constant.BLANK;

    public StandardWebServerApplicationContext(Class<?> startupClass) {
        this.startupClass = startupClass;
    }

    public StandardWebServerApplicationContext() {
        this(null);
    }

    @Override
    protected StandardBeanFactory createBeanFactory() {
        return new StandardWebBeanFactory(this);
    }

    @Override
    protected void postProcessRegisterListener(Map<Class<?>, List<ApplicationListener<Object>>> applicationListeners) {
        super.postProcessRegisterListener(applicationListeners);
        registerSingleton(this);
    }

    @Override
    protected void onRefresh() {

        this.webServer = ApplicationUtils.obtainWebServer(this);
        super.onRefresh();
    }

    @Override
    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public Class<?> getStartupClass() {
        return startupClass;
    }

    @Override
    public String getContextPath() {
        final String contextPath = this.contextPath;
        if (contextPath == null) {
            return this.contextPath = Constant.BLANK;
        }
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
