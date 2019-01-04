package com.tyc.netty.live;

import com.dz.netty.live.LiveChannelCache;
import com.dz.netty.live.LiveMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*Channel 数据传输流，与channel相关的概念有以下四个，上一张图让你了解netty里面的Channel。
Channel，表示一个连接，可以理解为每一个请求，就是一个Channel。
ChannelHandler，核心处理业务就在这里，用于处理业务请求。
ChannelHandlerContext，用于传输业务数据。
ChannelPipeline，用于保存处理过程需要用到的ChannelHandler和ChannelHandlerContext。*/

public class LiveHandler extends SimpleChannelInboundHandler<LiveMessage> {
    //LiveChannelCache该对象保存着一个Channel对应的定时任务ScheduledFuture
    private static Map<Integer,LiveChannelCache> channelCacheMap = new HashMap<>(16);
    private Logger logger = LoggerFactory.getLogger(com.dz.netty.live.LiveHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LiveMessage msg) throws Exception {
        //相当于拿到一个连接(一个请求)
        Channel channel = ctx.channel();
        //作为Map中的key
        final int hashCode = channel.hashCode();
        logger.debug("channel hashCode:" + hashCode + " msg:" + msg + " cache:" + channelCacheMap.size());

        if(!channelCacheMap.containsKey(hashCode)){
            logger.debug("channelCache.containsKey(hashCode), put key:" + hashCode);
            //给channel要close时设一个监听事件
            channel.closeFuture().addListener(f ->{
                System.out.println("channel close, remove key:" + hashCode);
                channelCacheMap.remove(hashCode);
            });
            //创建一个线程定时执行任务
            ScheduledFuture scheduledFuture = ctx.executor().schedule(
                    ()->{
                        logger.debug("schedule runs, close channel:" + hashCode);
                        channel.close();
                    },10,TimeUnit.SECONDS);
            channelCacheMap.put(hashCode,new LiveChannelCache(channel,scheduledFuture));
        }

        switch (msg.getType()){
            case LiveMessage.TYPE_HEART:{
                LiveChannelCache cache = channelCacheMap.get(hashCode);
                ScheduledFuture<ChannelFuture> schedule = ctx.executor()
                        .schedule(() -> channel.close(), 5, TimeUnit.SECONDS);
                //取消之前的定时任务
                cache.getScheduledFuture().cancel(true);
                //重新设置一个定时任务
                cache.setScheduledFuture(schedule);
                //write写到队列中，flush触发监听事件
                ctx.channel().writeAndFlush(msg);
                break;
            }
            case LiveMessage.TYPE_MESSAGE:{
                //遍历每个一个连接进行操作
                channelCacheMap.entrySet().stream().forEach(entry -> {
                    Channel otherChannel = entry.getValue().getChannel();
                    otherChannel.writeAndFlush(msg);
                });
                break;
            }
            default:
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channelReadComplete");
        super.channelReadComplete(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("exceptionCaught");
        if(null != cause) {cause.printStackTrace();}
        if(null != ctx) {ctx.close();}
    }

}
