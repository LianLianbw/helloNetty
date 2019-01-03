package com.tyc.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.AsciiHeadersEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

import java.io.*;

/*1)Handler需要声明泛型为<FullHttpRequest>，声明之后，只有msg为FullHttpRequest的消息才能进来。
2)生成response，这里使用的FullHttpResponse，同FullHttpRequest类似，
3)通过这个我们就不用将response拆分成多个channel返回给请求端了。
4)添加header描述length。这一步是很重要的一步，如果没有这一步，你会发现用postman发出请求之后就一直在刷新，
  因为http请求方不知道返回的数据到底有多长。channel读取完成之后需要输出缓冲流。如果没有这一步，你会发现postman同样会一直在刷新。*/
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws IOException {
        System.out.println("class:" + fullHttpRequest.getClass().getName());
        File file = new File("C:\\Users\\A\\Desktop\\文件夹\\七年级\\七年级\\11\\1-卢帅任-1601072018210043-男.jpg");
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int n;
        while ((n = fis.read(b)) != -1){
            bos.write(b, 0, n);
        }
        fis.close();
        bos.close();
        byte[] bytes = bos.toByteArray();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes));

        HttpHeaders headers = response.headers();
        headers.add(HttpHeaderNames.CONTENT_TYPE,contentType+"; charset=UTF-8");
        headers.add(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
        headers.add(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);

        channelHandlerContext.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelReadComplete");
        super.channelReadComplete(ctx);
        ctx.flush(); // 4
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if(null != cause){
            cause.printStackTrace();
        }
        if(null != ctx){
            ctx.close();
        }
    }
}
