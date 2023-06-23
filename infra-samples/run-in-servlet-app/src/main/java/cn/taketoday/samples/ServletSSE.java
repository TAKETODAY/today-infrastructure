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

package cn.taketoday.samples;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.http.MediaType;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RestController;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 23:14
 */
@RestController
public class ServletSSE {
  private final Executor executor;

  public ServletSSE(Executor executor) {
    this.executor = executor;
  }

  @GET("/sse-servlet")
  public void simple(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ServletOutputStream outputStream = response.getOutputStream();
    response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
//    response.setHeader(HttpHeaders.TRANSFER_ENCODING, HttpHeaders.CHUNKED);
    outputStream.print(StringUtils.generateRandomString(1000));
    AsyncContext asyncContext = request.startAsync();

    asyncContext.addListener(new AsyncListener() {
      @Override
      public void onComplete(AsyncEvent event) throws IOException {
        System.out.println("onComplete");
      }

      @Override
      public void onTimeout(AsyncEvent event) throws IOException {
        System.out.println("onTimeout");
      }

      @Override
      public void onError(AsyncEvent event) throws IOException {
        System.out.println("onError");
      }

      @Override
      public void onStartAsync(AsyncEvent event) throws IOException {
      }
    });
    asyncContext.setTimeout(-1);
    executor.execute(() -> {
      for (int i = 0; i < 5; i++) {
        ExceptionUtils.sneakyThrow(() -> {
          System.out.println("isCommitted:" + response.isCommitted());
          TimeUnit.SECONDS.sleep(1);
          outputStream.print(StringUtils.generateRandomString(1000));
          response.flushBuffer();
        });
      }

    });
  }

}
