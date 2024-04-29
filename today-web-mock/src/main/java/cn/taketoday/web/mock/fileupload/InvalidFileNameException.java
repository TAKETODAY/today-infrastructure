/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.web.mock.fileupload;

/**
 * This exception is thrown in case of an invalid file name.
 * A file name is invalid, if it contains a NUL character.
 * Attackers might use this to circumvent security checks:
 * For example, a malicious user might upload a file with the name
 * "foo.exe\0.png". This file name might pass security checks (i.e.
 * checks for the extension ".png"), while, depending on the underlying
 * C library, it might create a file named "foo.exe", as the NUL
 * character is the string terminator in C.
 */
public class InvalidFileNameException extends RuntimeException {

  /**
   * Serial version UID, being used, if the exception
   * is serialized.
   */
  private static final long serialVersionUID = 7922042602454350470L;

  /**
   * The file name causing the exception.
   */
  private final String name;

  /**
   * Creates a new instance.
   *
   * @param pName The file name causing the exception.
   * @param pMessage A human readable error message.
   */
  public InvalidFileNameException(final String pName, final String pMessage) {
    super(pMessage);
    name = pName;
  }

  /**
   * Returns the invalid file name.
   *
   * @return the invalid file name.
   */
  public String getName() {
    return name;
  }

}
