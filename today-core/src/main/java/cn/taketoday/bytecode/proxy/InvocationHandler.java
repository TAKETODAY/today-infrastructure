/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode.proxy;

/**
 * {@link java.lang.reflect.InvocationHandler} replacement (unavailable under
 * JDK 1.2). This callback type is primarily for use by the {@link Proxy} class
 * but may be used with {@link Enhancer} as well.
 *
 * @author Neeme Praks <a href="mailto:neeme@apache.org">neeme@apache.org</a>
 * @version $Id: InvocationHandler.java,v 1.3 2004/06/24 21:15:20 herbyderby Exp
 * $
 */
@FunctionalInterface
public interface InvocationHandler extends Callback, java.lang.reflect.InvocationHandler {

}
