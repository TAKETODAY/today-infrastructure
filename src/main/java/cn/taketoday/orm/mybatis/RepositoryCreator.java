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
package cn.taketoday.orm.mybatis;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.annotation.Repository;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2018-10-05 19:03
 */
@ContextListener
public class RepositoryCreator implements ApplicationListener<BeanDefinitionLoadingEvent> {

    @Override
    public void onApplicationEvent(BeanDefinitionLoadingEvent event) {

        final Logger log = LoggerFactory.getLogger(getClass());
        log.debug("Loading Mybatis Mapper Bean Definitions");

        ApplicationContext applicationContext = event.getApplicationContext();
        BeanNameCreator beanNameCreator = applicationContext.getEnvironment().getBeanNameCreator();

        for (final Class<?> beanClass : event.getCandidates()) {

            if (!beanClass.isInterface()) {
                continue;
            }
            Repository repository = beanClass.getAnnotation(Repository.class);
            if (repository == null) {
                continue;
            }
            log.debug("Found Mapper: [{}]", beanClass.getName());

            final String name;
            String[] names = repository.value();
            if (ObjectUtils.isNotEmpty(names)) {
                name = names[0];
            }
            else {
                name = beanNameCreator.create(beanClass);
            }

            applicationContext.registerSingleton(BeanFactory.FACTORY_BEAN_PREFIX + name, new MapperFactoryBean<>(beanClass));
            applicationContext.registerBean(name, MapperFactoryBean.class);
        }
    }

}
