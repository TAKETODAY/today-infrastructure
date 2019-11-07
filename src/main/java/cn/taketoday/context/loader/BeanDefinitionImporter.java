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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.loader;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.factory.BeanDefinitionRegistry;

/**
 * @author TODAY <br>
 *         2019-10-01 19:08
 */
@FunctionalInterface
public interface BeanDefinitionImporter {

    /**
     * Register bean definitions
     * 
     * @param importingDef
     *            The BeanDefinition that annotated {@link Import}
     * @param registry
     *            Bean definition registry
     */
    void registerBeanDefinitions(BeanDefinition importingDef, BeanDefinitionRegistry registry);

}
