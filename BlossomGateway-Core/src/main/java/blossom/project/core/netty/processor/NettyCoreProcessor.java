package blossom.project.core.netty.processor;


import blossom.project.common.enums.ResponseCode;
import blossom.project.common.exception.BaseException;
import blossom.project.common.exception.LimitedException;
import blossom.project.core.context.GatewayContext;
import blossom.project.core.context.HttpRequestWrapper;
import blossom.project.core.filter.FilterChainFactory;
import blossom.project.core.filter.GatewayFilterChainChainFactory;
import blossom.project.core.helper.RequestHelper;
import blossom.project.core.helper.ResponseHelper;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyCoreProcessor implements NettyProcessor{
    // FilterChainFactory 负责创建和管理过滤器的执行。
    private FilterChainFactory filterChainFactory = GatewayFilterChainChainFactory.getInstance();

    /**
     * 处理传入的 HTTP 请求。
     *
     * @param wrapper 包含 FullHttpRequest 和 ChannelHandlerContext 的 HttpRequestWrapper。
     */
    @Override
    public void process(HttpRequestWrapper wrapper) {
        FullHttpRequest request=wrapper.getRequest();
        ChannelHandlerContext ctx = wrapper.getCtx();
        try {
            // 创建并填充 GatewayContext 以保存有关传入请求的信息。
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);

            // 在 GatewayContext 上执行过滤器链逻辑。
            filterChainFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);
        }catch (HystrixRuntimeException e){
            // 通过记录日志并发送内部服务器错误响应处理未知异常。
            log.error("网关请求超时错误", e);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.GATEWAY_FALLBACK);
            doWriteAndRelease(ctx, request, httpResponse);
        }
        catch (LimitedException e) {
            // 通过记录日志并发送内部服务器错误响应处理未知异常。
            log.error("请求过量错误", e);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.FLOW_CONTROL_ERROR);
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (BaseException e) {
            // 通过记录日志并发送适当的 HTTP 响应处理已知异常。
            log.error("处理错误 {} {}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
            // 通过记录日志并发送内部服务器错误响应处理未知异常。
            log.error("处理未知错误", t);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            doWriteAndRelease(ctx, request, httpResponse);
        }
    }
    /**
     * 将 HTTP 响应写入通道并释放资源。
     *
     * @param ctx          用于写入响应的 ChannelHandlerContext。
     * @param request      从客户端接收的 FullHttpRequest。
     * @param httpResponse 作为响应发送的 FullHttpResponse。
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(request);

    }
    /**
     * 启动 NettyCoreProcessor。（目前为空方法）
     */
    @Override
    public void start() {
        // 如果需要，启动处理器的实现。
    }

    /**
     * 关闭 NettyCoreProcessor。（目前为空方法）
     */
    @Override
    public void shutDown() {
        // 如果需要，关闭处理器的实现。
    }
}
