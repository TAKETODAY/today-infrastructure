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
package cn.taketoday.framework.reactive;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.Executor;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.web.exception.ExceptionUnhandledException;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author TODAY <br>
 *         2019-07-04 21:50
 */
@Singleton
public class ReactiveDispatcher extends DispatcherHandler implements ChannelInboundHandler {

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) { // sync(ctx, msg);
            async(ctx, (FullHttpRequest) msg);
        }
        else {
            ctx.fireChannelRead(msg);
        }
    }

    protected void sync(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // Lookup handler mapping
        final NettyRequestContext context = new NettyRequestContext(getContextPath(), ctx, msg);
        try {
            handle(context);
            context.send();
        }
        catch (Throwable e) {
            throw new ExceptionUnhandledException(e);
        }
    }

    private void async(ChannelHandlerContext ctx, FullHttpRequest request) {

        final NettyRequestContext context = new NettyRequestContext(getContextPath(), ctx, request);

        final Executor executor = ctx.executor();

        completedFuture(context)
                .thenApplyAsync(this::lookupHandler, executor)
                .thenApplyAsync(handler -> handle(ctx, context, handler), executor)
                .thenAcceptAsync(handler -> context.send(), executor);
    }

    protected Object handle(ChannelHandlerContext ctx, final NettyRequestContext context, Object handler) {
        try {
            handle(handler, context);
        }
        catch (Throwable e) {
            ctx.fireExceptionCaught(e);
        }
        return handler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        log.error("cause :{}", cause.toString(), cause);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

}
