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
package test.demo;

import java.util.function.Function;

import cn.taketoday.framework.Constant;

/**
 * @author TODAY <br>
 *         2019-11-17 01:37
 */
public class Json implements Constant {

    private static final long serialVersionUID = 1L;
    
    private static final String OPERATION_OK = "ok";
    private static final String OPERATION_FAILED = "操作失败";

    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_FAILED = 500;

    private Object data;
    private String message;
    private int code = 200;
    private boolean success;

    public Object getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Json data(Object data) {
        this.data = data;
        return this;
    }

    public Json message(String message) {
        this.message = message;
        return this;
    }

    public Json code(int code) {
        this.code = code;
        return this;
    }

    public Json success(boolean success) {
        this.success = success;
        return this;
    }

    /**
     * Apply the common {@link Json} result
     * 
     * @param <T>
     * @param func
     *            the {@link Function}
     * @param param
     *            parameter
     * @return
     */
    public static final <T> Json apply(Function<T, Boolean> func, T param) {
        if (func.apply(param)) {
            return Json.ok();
        }
        return Json.failed();
    }

    /**
     * 
     * @param <T>
     * @param success
     * @return
     */
    public static final <T> Json apply(boolean success) {
        if (success) {
            return Json.ok();
        }
        return Json.failed();
    }

    /**
     * @param success
     *            if success
     * @param status
     *            error status
     * @param message
     *            the message of the response
     * @param data
     *            response data
     */
    public static Json create(boolean success, int status, String message, Object data) {
        return new Json()//
                .data(data)//
                .message(message)//
                .code(status)//
                .success(success);
    }

    public static Json ok() {
        return create(true, STATUS_SUCCESS, OPERATION_OK, null);
    }

    public static Json ok(String message, Object data) {
        return create(true, STATUS_SUCCESS, message, data);
    }

    public static Json ok(Object data) {
        return create(true, STATUS_SUCCESS, OPERATION_OK, data);
    }

    public static Json ok(String message) {
        return create(true, STATUS_SUCCESS, message, null);
    }

    public static Json ok(int status) {
        return create(true, status, OPERATION_OK, null);
    }

    /**
     * default failed json
     */
    public static Json failed() {
        return create(false, STATUS_FAILED, OPERATION_FAILED, null);
    }

    public static Json failed(Object data) {
        return create(false, STATUS_FAILED, OPERATION_FAILED, data);
    }

    public static Json failed(String message) {
        return create(false, STATUS_FAILED, message, null);
    }

    public static Json failed(String message, Object data) {
        return create(false, STATUS_FAILED, message, data);
    }

    public static Json failed(String message, int status) {
        return create(false, status, message, null);
    }

    public static Json failed(int status) {
        return create(false, status, OPERATION_FAILED, null);
    }

    public static Json failed(String message, int status, Object data) {
        return create(false, status, message, data);
    }

    public static Json badRequest() {
        return badRequest(BAD_REQUEST);
    }

    /**
     * @param msg
     * @return
     */
    public static Json badRequest(String msg) {
        return create(false, 400, msg, null);
    }

    public static Json notFound() {
        return notFound(NOT_FOUND);
    }

    public static Json notFound(String msg) {
        return create(false, 404, msg, null);
    }

    public static Json unauthorized() {
        return unauthorized(UNAUTHORIZED);
    }

    public static Json unauthorized(String msg) {
        return failed(msg, 401);
    }

    public static Json accessForbidden() {
        return accessForbidden(ACCESS_FORBIDDEN);
    }

    public static Json accessForbidden(String msg) {
        return failed(msg, 403);
    }

    public String toString() {
        return new StringBuilder()//
                .append("{\"message\":\"").append(message)//
                .append("\",\"code\":\"").append(code)//
                .append("\",\"data\":\"").append(data)//
                .append("\",\"success\":\"").append(success)//
                .append("\"}")//
                .toString();
    }
}
