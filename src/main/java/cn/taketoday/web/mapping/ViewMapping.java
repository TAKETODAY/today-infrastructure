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
package cn.taketoday.web.mapping;

import java.lang.reflect.Method;

import cn.taketoday.web.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * views request mapping
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:58:07
 * @version 2.0.0
 */
@Setter
@Getter
@SuppressWarnings("serial")
public class ViewMapping implements WebMapping {

    /** 返回类型 */
    private byte returnType = Constant.TYPE_FORWARD; // default -> forward
    /** 资源路径 */
    private String assetsPath = "";

    /** Bean instance @since 2.3.3 */
    private Object controller;

    /** Handler method @since 2.3.3 */
    private Method action;

    /** The resource's content type @since 2.3.3 */
    private String contentType = null;
    
    /** The request status @since 2.3.7 */
    private int status;

    public final boolean hasAction() {
        return action != null;
    }

    @Override
    public String toString() {
        return "[returnType=" + returnType + ", assetsPath=" + assetsPath + ", contentType=" + contentType + "]";
    }

}
