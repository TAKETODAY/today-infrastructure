/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.samples;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import infra.http.HttpHeaders;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ExceptionUtils;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.async.DeferredResult;
import infra.web.async.WebAsyncTask;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 20:55
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/async")
class AsyncController {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @GET("/deferred-result")
  public DeferredResult<String> deferredResult(@Nullable Long timeout) {
    DeferredResult<String> result = new DeferredResult<>(timeout, "Timeout");
    Future.run(() -> {
      logger.info("异步任务开始执行");

      ExceptionUtils.sneakyThrow(() -> TimeUnit.SECONDS.sleep(2));

      logger.info("异步任务执行结束，开始返回");

      if (result.setResult("result from " + Thread.currentThread().getName())) {
        logger.info("返回结果设置成功");
      }
      else {
        logger.info("返回结果设置失败，已经返回任务可能超时");
      }

      logger.info("异步任务执行结束");
    });

    result.onTimeout(() ->
            logger.warn("任务执行超时了"));

    result.onCompletion(() ->
            logger.info("结束回调"));

    result.onError(throwable ->
            logger.error("出现异常", throwable));

    return result;
  }

  @GET("/callable")
  public Callable<String> callable() {
    return () -> {
      // 可以获取 RequestContext
      RequestContext request = RequestContextHolder.getRequired();
      logger.info("异步任务开始执行");
//        TimeUnit.SECONDS.sleep(2);
      LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
      HttpHeaders headers = request.getHeaders();
      logger.info(headers.toString());
      logger.info("异步任务执行结束，开始返回");
      return "result from " + Thread.currentThread().getName();
    };
  }

  @GET("/web-async-task")
  public WebAsyncTask<String> webAsyncTask(@Nullable Long timeout) {
    return new WebAsyncTask<>(timeout, callable());
  }

}
