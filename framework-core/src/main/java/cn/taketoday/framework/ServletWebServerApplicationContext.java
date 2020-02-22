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
package cn.taketoday.framework;

import javax.servlet.Servlet;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.framework.server.WebServer;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;

/**
 * {@link Servlet} based Web {@link ApplicationContext}
 * 
 * @author TODAY <br>
 *         2019-01-17 15:54
 */
public class ServletWebServerApplicationContext
        extends StandardWebServletApplicationContext
        implements WebServerApplicationContext, ConfigurableWebServerApplicationContext {

    private WebServer webServer;

    private Class<?> startupClass;

    public ServletWebServerApplicationContext(Class<?> startupClass) {
        this.startupClass = startupClass;
    }

    public ServletWebServerApplicationContext() {
        this(null);
    }

    @Override
    protected void onRefresh()  {
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
