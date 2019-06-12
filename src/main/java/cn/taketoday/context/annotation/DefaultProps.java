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
package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.context.Constant;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-03-15 23:18
 */
@Setter
@SuppressWarnings("all")
public class DefaultProps implements Props, Annotation {

    private boolean replace = false;
    private Class<?>[] nested = new Class[0];
    private String[] value = Constant.EMPTY_STRING_ARRAY;
    private String[] prefix = Constant.EMPTY_STRING_ARRAY;

    public DefaultProps() {

    }

    public DefaultProps(Props props) {
        this.value = props.value();
        this.nested = props.nested();
        this.prefix = props.prefix();
        this.replace = props.replace();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Props.class;
    }

    @Override
    public String[] value() {
        return value;
    }

    @Override
    public String[] prefix() {
        return prefix;
    }

    @Override
    public boolean replace() {
        return replace;
    }

    @Override
    public Class<?>[] nested() {
        return nested;
    }

}
