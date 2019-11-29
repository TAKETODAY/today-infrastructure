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
package cn.taketoday.context.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;

/**
 * 
 * @author TODAY <br>
 *         2018-11-13 21:25
 */
public abstract class ExceptionUtils {

    /**
     * Unwrap
     * 
     * @param ex
     *            target {@link Throwable}
     * @return unwrapped {@link Throwable}
     */
    public static Throwable unwrapThrowable(Throwable ex) {
        Throwable unwrapped = ex;
        while (true) {
            if (unwrapped instanceof InvocationTargetException) {
                unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
            }
            else if (unwrapped instanceof UndeclaredThrowableException) {
                unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
            }
            else {
                return unwrapped;
            }
        }
    }

    public static ContextException newContextException(Throwable ex) {
        if (ex instanceof ContextException) {
            return (ContextException) ex;
        }
        return new ContextException(ex);
    }

    public static ContextException newContextException(Throwable ex, String message) {
        if (ex instanceof ContextException) {
            return (ContextException) ex;
        }
        return new ContextException(message, ex);
    }

    public static ConfigurationException newConfigurationException(Throwable ex) {
        return newConfigurationException(ex, null);
    }

    public static ConfigurationException newConfigurationException(Throwable ex, String message) {
        if (ex instanceof ConfigurationException) {
            return (ConfigurationException) ex;
        }
        return new ConfigurationException(message, ex);
    }

}
