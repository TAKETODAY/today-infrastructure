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
package cn.taketoday.aop;

/**
 * @author TODAY <br>
 *         2018-11-10 16:46
 */
public interface Constant extends cn.taketoday.context.Constant {

    /* Indicates the ASM API version that is used throughout cglib */
    //	int			ASM_API					= ASM7;

    /*************************************************
     * Parameter Types
     */
    byte TYPE_NULL = 0x00;
    byte TYPE_THROWING = 0x01;
    byte TYPE_ARGUMENT = 0x02;
    byte TYPE_ARGUMENTS = 0x03;
    byte TYPE_RETURNING = 0x04;
    byte TYPE_ANNOTATED = 0x05;
    byte TYPE_JOIN_POINT = 0x06;

}
