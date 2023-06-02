/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm.jaxb;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = "http://springframework.org/spring-ws")
public class BinaryObject {

  @XmlElement(namespace = "http://springframework.org/spring-ws")
  private byte[] bytes;

  @XmlElement(namespace = "http://springframework.org/spring-ws")
  private DataHandler dataHandler;

  @XmlElement(namespace = "http://springframework.org/spring-ws")
  @XmlAttachmentRef
  private DataHandler swaDataHandler;

  public BinaryObject() {
  }

  public BinaryObject(byte[] bytes, DataHandler dataHandler) {
    this.bytes = bytes;
    this.dataHandler = dataHandler;
    swaDataHandler = dataHandler;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public DataHandler getDataHandler() {
    return dataHandler;
  }

  public DataHandler getSwaDataHandler() {
    return swaDataHandler;
  }
}
