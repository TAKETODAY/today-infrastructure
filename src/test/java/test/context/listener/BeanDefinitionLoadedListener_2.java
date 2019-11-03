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
package test.context.listener;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.event.BeanDefinitionLoadedEvent;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author Today <br>
 * 
 *         2018-11-08 20:38
 */
@ContextListener
public class BeanDefinitionLoadedListener_2 implements ApplicationListener<BeanDefinitionLoadedEvent>, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BeanDefinitionLoadedListener_2.class);

    @Override
    public void onApplicationEvent(BeanDefinitionLoadedEvent event) {
        log.debug("BeanDefinitionLoadedListener_2");
    }

    @Override
    public int getOrder() {

        return 2;
    }

}
