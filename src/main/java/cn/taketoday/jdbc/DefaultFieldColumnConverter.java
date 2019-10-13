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
package cn.taketoday.jdbc;

import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-08-30 21:33
 */
public class DefaultFieldColumnConverter implements FieldColumnConverter {

    @Override
    public String convert(String field) {
        return camelCaseName(field);
    }

    /**
     * Convert a name in camelCase to an underscored name in lower case. Any upper
     * case letters are converted to lower case with a preceding underscore.
     * 
     * @param name
     *            the original name
     * @return the converted name
     */
    protected String camelCaseName(String name) {

        if (StringUtils.isEmpty(name)) {
            return Constant.BLANK;
        }

        final int length = name.length();
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < length; i++) {
            final char c = name.charAt(i);
            if (c > 0x40 && c < 0x5b) {
                ret.append('_').append((char) (c | 0x20));
            }
            else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

}
