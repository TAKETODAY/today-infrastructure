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
package cn.taketoday.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.StandardBeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.listener.ContextCloseListener;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Standard {@link ApplicationContext}
 * 
 * @author TODAY <br>
 *         <p>
 *         2018-09-06 13:47
 */
public class StandardApplicationContext extends AbstractApplicationContext implements ConfigurableApplicationContext {

    private StandardBeanFactory beanFactory;

    /**
     * Start with given class set
     *
     * @param classes
     *            class set
     */
    public StandardApplicationContext(Collection<Class<?>> classes) {
        this(Constant.BLANK);
        loadContext(classes);
    }

    /**
     * Set given properties location
     * 
     * @param propertiesLocation
     *            a file or a directory to scan
     */
    public StandardApplicationContext(String propertiesLocation) {
        this();
        if (StringUtils.isNotEmpty(propertiesLocation)) {
            setPropertiesLocation(propertiesLocation);
        }
    }

    /**
     * Start context with given properties location and base scan packages
     * 
     * @param propertiesLocation
     *            a file or a directory contains
     * @param locations
     *            scan classes from packages
     */
    public StandardApplicationContext(String propertiesLocation, String... locations) {
        this(propertiesLocation);
        loadContext(locations);
    }

    public StandardApplicationContext(StandardBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public StandardApplicationContext() {

    }

    @Override
    public AbstractBeanFactory getBeanFactory() {

        if (this.beanFactory == null) {
            this.beanFactory = createBeanFactory();
        }
        return this.beanFactory;
    }

    protected StandardBeanFactory createBeanFactory() {
        return new StandardBeanFactory(this);
    }

    @Override
    protected void loadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> beanClasses) {

        super.loadBeanDefinitions(beanFactory, beanClasses);

        this.beanFactory.loadConfigurationBeans();
        // @since 2.1.6
        beanClasses.addAll(this.beanFactory.loadMetaInfoBeans());

        this.beanFactory.loadMissingBean(beanClasses);
    }

    @Override
    protected void postProcessRegisterListener(Map<Class<?>, List<ApplicationListener<EventObject>>> applicationListeners) {
        addApplicationListener(new ContextCloseListener());

        for (final Class<?> listener : loadMetaInfoListeners()) {
            registerListener(listener);
        }
    }

    /**
     * Load the META-INF/listeners
     * 
     * @since 2.1.6
     */
    public Set<Class<?>> loadMetaInfoListeners() { // fixed #9 Some listener in a jar can't be load

        // Load the META-INF/listeners
        // ---------------------------------------------------
        final Set<Class<?>> beans = new HashSet<>();

        try {

            final ClassLoader classLoader = ClassUtils.getClassLoader();
            final Enumeration<URL> resources = classLoader.getResources("META-INF/listeners");
            final Charset charset = Constant.DEFAULT_CHARSET;

            while (resources.hasMoreElements()) {
                try (final BufferedReader reader = new BufferedReader(//
                        new InputStreamReader(resources.nextElement().openStream(), charset))) { // fix

                    String str;
                    while ((str = reader.readLine()) != null) {
                        beans.add(classLoader.loadClass(str));
                    }
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
            LoggerFactory.getLogger(getClass()).error("Exception occurred when load 'META-INF/listeners'", e);
            throw ExceptionUtils.newContextException(e);
        }
        return beans;
    }

}
