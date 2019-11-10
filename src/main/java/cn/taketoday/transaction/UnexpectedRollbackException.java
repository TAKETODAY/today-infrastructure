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
package cn.taketoday.transaction;

/**
 * Thrown when an attempt to commit a transaction resulted in an unexpected
 * rollback.
 *
 * @author Rod Johnson
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class UnexpectedRollbackException extends TransactionException {

    /**
     * Constructor for UnexpectedRollbackException.
     * 
     * @param msg
     *            the detail message
     */
    public UnexpectedRollbackException(String msg) {
        super(msg);
    }

    /**
     * Constructor for UnexpectedRollbackException.
     * 
     * @param msg
     *            the detail message
     * @param cause
     *            the root cause from the transaction API in use
     */
    public UnexpectedRollbackException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
