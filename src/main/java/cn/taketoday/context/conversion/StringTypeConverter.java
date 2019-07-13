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
package cn.taketoday.context.conversion;

import cn.taketoday.context.exception.ConversionException;

/**
 * @author TODAY <br>
 *         2019-06-06 15:31
 * @since 2.1.6
 */
public abstract class StringTypeConverter implements TypeConverter {

    public boolean supports(Class<?> targetClass, Object source) {
        return source instanceof String && supports(targetClass);
    }

    public boolean supports(Class<?> targetClass) {
        return true;
    }

    @Override
    public final Object convert(Class<?> targetClass, Object source) throws ConversionException {
        return convertInternal(targetClass, (String) source);
    }

    protected abstract Object convertInternal(Class<?> targetClass, String source) throws ConversionException;
}
