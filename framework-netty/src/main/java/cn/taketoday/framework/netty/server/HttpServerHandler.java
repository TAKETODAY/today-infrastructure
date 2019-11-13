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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.framework.netty.server;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import cn.taketoday.framework.netty.NettyRequestContext;
import cn.taketoday.web.RequestContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2019-07-04 20:43
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        CompletableFuture<HttpRequest> future = CompletableFuture.completedFuture(httpRequest);

        Executor executor = ctx.executor();

        future.thenApplyAsync(request -> buildWebContext(ctx, request), executor)//
                .thenApplyAsync(this::executeLogic, executor);
    }

    private RequestContext buildWebContext(ChannelHandlerContext ctx, HttpRequest request) {

        return new NettyRequestContext(request);
    }

    private void writeResponse(ChannelHandlerContext ctx, CompletableFuture<HttpRequest> future, FullHttpResponse msg) {
        ctx.writeAndFlush(msg);
        future.complete(null);
    }

    private FullHttpResponse handleException(Throwable e) {
//        HttpRequest request = RequestContext.request();
//        HttpResponse response = RequestContext.response();
//        String method = request.method();
//        String uri = request.uri();
        return null;
    }

    private FullHttpResponse buildResponse(RequestContext webContext) {
        return null;
    }

    private RequestContext executeLogic(RequestContext webContext) {

        return webContext;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

//            log.error(cause.getMessage(), cause);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(500));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
