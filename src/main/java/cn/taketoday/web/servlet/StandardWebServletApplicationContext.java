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
package cn.taketoday.web.servlet;

import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContext;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2018-07-10 1:16:17
 */
public class StandardWebServletApplicationContext extends AbstractApplicationContext implements WebServletApplicationContext {

    /**
     * Servlet context
     */
    private ServletContext servletContext;

    private final StandardWebServletBeanFactory beanFactory;

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public StandardWebServletApplicationContext() {
        this.beanFactory = new StandardWebServletBeanFactory(this);
    }

    @Override
    public AbstractBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    public StandardWebServletApplicationContext(ServletContext servletContext) {
        this();
        this.servletContext = servletContext;
        loadContext();
    }

    /**
     * @param classes
     *            class set
     * @param servletContext
     * @since 2.3.3
     */
    public StandardWebServletApplicationContext(Set<Class<?>> classes, ServletContext servletContext) {
        this();
        this.servletContext = servletContext;
        loadContext(classes);
    }

    /**
     * @param servletContext
     * @param properties
     *            properties location
     * @param locations
     *            package locations
     * @since 2.3.3
     */
    public StandardWebServletApplicationContext(ServletContext servletContext, String propertiesLocation, String... locations) {
        this();
        if (StringUtils.isNotEmpty(propertiesLocation)) {
            setPropertiesLocation(propertiesLocation);
        }
        this.servletContext = servletContext;
        loadContext(locations);
    }

    public StandardWebServletApplicationContext(StandardWebServletBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    protected void postProcessBeanFactory(AbstractBeanFactory beanFactory) {
        // register WebApplicationContext
        registerSingleton(beanFactory.getBeanNameCreator().create(WebServletApplicationContext.class), this);

        super.postProcessBeanFactory(beanFactory);
    }

    @Override
    protected void loadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> beanClasses) {
        super.loadBeanDefinitions(beanFactory, beanClasses);
        this.beanFactory.loadConfigurationBeans();
        this.beanFactory.loadMissingBean(beanClasses);
    }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

}
