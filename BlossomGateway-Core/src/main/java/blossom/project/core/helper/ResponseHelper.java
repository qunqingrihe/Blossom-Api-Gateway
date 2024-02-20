package blossom.project.core.helper;

import blossom.project.common.constant.BasicConst;
import blossom.project.common.enums.ResponseCode;
import blossom.project.core.context.IContext;
import blossom.project.core.response.GatewayResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;

import java.util.Objects;

/*响应的辅助类*/
public class ResponseHelper {
    public static FullHttpResponse getFullHttpResponse(ResponseCode responseCode){
        GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(responseCode);
        DefaultFullHttpResponse httpResponse =new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes()));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,httpResponse.content().readableBytes());
        return httpResponse;
    }
    private static FullHttpResponse getHttpResponse(IContext context, GatewayResponse gatewayResponse){
        ByteBuf content;
        if(Objects.nonNull(gatewayResponse.getFutureResponse())){
            content = Unpooled.wrappedBuffer(gatewayResponse.getFutureResponse()
                    .getResponseBodyAsByteBuffer());
        }else if (gatewayResponse.getContent()!=null){
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes());
        }else{
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes());
        }
        if(Objects.isNull(gatewayResponse.getFutureResponse())){
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    gatewayResponse.getHttpResponseStatus(),
                    content);
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            httpResponse.headers().add(gatewayResponse.getExtraResponseHeaders());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            return httpResponse;
        }else{
            gatewayResponse.getFutureResponse().getHeaders().add(gatewayResponse.getExtraResponseHeaders());

            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(gatewayResponse.getFutureResponse().getStatusCode()),
                    content);
            httpResponse.headers().add(gatewayResponse.getFutureResponse().getHeaders());
            return httpResponse;
        }
    }
    /**
     * 写回响应信息方法
     */
    public static void writeResponse(IContext context){
        context.releaseRequest();
        if(context.isWritten()){
            FullHttpResponse httpResponse=ResponseHelper.getHttpResponse(context,(GatewayResponse)context.getResponse());
            if(!context.isKeepAlive()){
                context.getNettyCtx()
                        .writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            }else{
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                context.getNettyCtx().writeAndFlush(httpResponse);
            }
            context.completed();
        } else if (context.isCompleted()) {
            context.invokeCompletedCallBack();
        }
    }
}
