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
package cn.taketoday.context.annotation.condition;

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;

/**
 * @author TODAY <br>
 *         2018-11-15 20:28
 */
public class WindowsCondition implements Condition {

    @Override
    public boolean matches(final ApplicationContext context, AnnotatedElement annotatedElement) {
        String system = context.getEnvironment().getProperty("os.name");
        if (system != null && system.contains("Windows")) {
            return true;
        }
        return false;
    }
}
