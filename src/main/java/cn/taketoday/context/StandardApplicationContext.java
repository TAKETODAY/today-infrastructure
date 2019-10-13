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

import java.util.Collection;

import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.StandardBeanFactory;
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
        super();
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

        // load beans form scanned classes
        super.loadBeanDefinitions(beanFactory, beanClasses);
        // load beans form beans that annotated Configuration
        this.beanFactory.loadConfigurationBeans();
        // @since 2.1.6
        beanClasses.addAll(this.beanFactory.loadMetaInfoBeans());

        this.beanFactory.loadMissingBean(beanClasses);
    }

}
