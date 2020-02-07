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
package cn.taketoday.aop.advice;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import lombok.Getter;

/**
 * @author TODAY <br>
 * 
 *         2018-11-10 18:48
 */
public enum AspectsRegistry {

    ASPECTS_REGISTRY;

    @Getter
    private final List<Object> aspects = new ArrayList<>();

    private boolean aspectsLoaded;

    public void addAspect(Object aspect) {
        aspects.add(aspect);
    }

    public static AspectsRegistry getInstance() {
        return ASPECTS_REGISTRY;
    }

    public void sortAspects() {
        OrderUtils.reversedSort(aspects);
    }

    public boolean isAspectsLoaded() {
        return aspectsLoaded;
    }

    public void setAspectsLoaded(boolean aspectsLoaded) {
        this.aspectsLoaded = aspectsLoaded;
    }

    public void loadAspects(final ConfigurableBeanFactory applicationContext) {
        final Logger log = LoggerFactory.getLogger(getClass());

        log.debug("Loading Aspect Objects");

        setAspectsLoaded(true);
        try {

            for (final BeanDefinition beanDefinition : applicationContext.getBeanDefinitions().values()) {

                if (beanDefinition.isAnnotationPresent(Aspect.class)) {
                    // fix use beanDefinition.getName()
                    final String aspectName = beanDefinition.getName();
                    log.debug("Found Aspect: [{}]", aspectName);

                    Object aspectInstance = applicationContext.getSingleton(aspectName);
                    if (aspectInstance == null) {
                        aspectInstance = ClassUtils.newInstance(beanDefinition, applicationContext);
                        applicationContext.registerSingleton(aspectName, aspectInstance);
                    }
                    addAspect(aspectInstance);
                }
            }
            sortAspects();
        }
        catch (Throwable e) {
            throw new ConfigurationException(e);
        }
    }

}
