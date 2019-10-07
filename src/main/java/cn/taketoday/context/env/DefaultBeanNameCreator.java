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
package cn.taketoday.context.env;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;

/**
 * Default implementation of {@link BeanNameCreator}
 * 
 * @author TODAY <br>
 * 
 *         2019-01-13 13:39
 */
public class DefaultBeanNameCreator implements BeanNameCreator {

    private final boolean useSimpleName;

    public DefaultBeanNameCreator(@Env(value = Constant.KEY_USE_SIMPLE_NAME,
                                       defaultValue = "true") boolean useSimpleName) {
        this.useSimpleName = useSimpleName;
    }

    @Autowired
    public DefaultBeanNameCreator(ConfigurableEnvironment environment) {
        this(environment.getProperty(Constant.KEY_USE_SIMPLE_NAME, boolean.class, true));
    }

    @Override
    public String create(Class<?> beanClass) {

        if (beanClass == null) {
            return Constant.DEFAULT;
        }
        if (useSimpleName) {

            final String simpleName = beanClass.getSimpleName();
            final char c = simpleName.charAt(0);
            if (c > 0x40 && c < 0x5b) {

                return new StringBuilder(simpleName)
                        .replace(0, 1, String.valueOf((char) (c | 0x20)))
                        .toString();
            }
            return simpleName;
        }
        return beanClass.getName(); // full name
    }
}
