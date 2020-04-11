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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.validation;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.web.exception.WebRuntimeException;

/**
 * @author TODAY <br>
 *         2019-07-21 14:35
 */
@SuppressWarnings("serial")
public class ValidationException extends WebRuntimeException implements Errors {

    private final Set<ObjectError> errors;

    public ValidationException() {
        this.errors = new HashSet<>();
    }

    public ValidationException(Errors errors) {
        this.errors = errors.getAllErrors();
    }

    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public int getErrorCount() {
        return errors.size();
    }

    public void addError(ObjectError error) {
        this.errors.add(error);
    }

    @Override
    public Set<ObjectError> getAllErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        return errors.toString();
    }
}
