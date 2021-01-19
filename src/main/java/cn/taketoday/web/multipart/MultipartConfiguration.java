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
package cn.taketoday.web.multipart;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.utils.DataSize;
import cn.taketoday.web.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 * 2019-07-11 22:47
 */
@Setter
@Getter
@MissingBean
@Props(prefix = "multipart.")
public class MultipartConfiguration {

  private static final long serialVersionUID = 1L;

  /*** file upload location */
  private String location = System.getProperty("java.io.tmpdir");

  private String encoding = Constant.DEFAULT_ENCODING;

  /**
   * Maximum size of a single uploaded file.
   */
  private DataSize maxFileSize = DataSize.ofMegabytes(512); // every single file
  private DataSize maxRequestSize = DataSize.ofGigabytes(1); // total size in every single request
  private DataSize fileSizeThreshold = DataSize.ofGigabytes(1); // cache

  @Override
  public String toString() {
    return new StringBuilder()
            .append("MultipartConfiguration [location=").append(location)
            .append(", encoding=").append(encoding)
            .append(", maxFileSize=").append(maxFileSize)
            .append(", maxRequestSize=").append(maxRequestSize)
            .append(", fileSizeThreshold=").append(fileSizeThreshold)
            .append("]").toString();
  }

}
