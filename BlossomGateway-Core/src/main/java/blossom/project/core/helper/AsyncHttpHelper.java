package blossom.project.core.helper;


import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;

/**异步的http辅助类 **/
public class AsyncHttpHelper {
    private static final class singletonHolder {
        private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
    }
    private AsyncHttpHelper() {}
    public static AsyncHttpHelper getInstance() {
        return singletonHolder.INSTANCE;
    }
    private AsyncHttpClient asyncHttpClient;
    public void initialized(AsyncHttpClient asyncHttpClient){this.asyncHttpClient=asyncHttpClient;}
    public CompletableFuture<Response> executeRequest(Request request){
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
        return future.toCompletableFuture();
    }
    public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> hander){
        ListenableFuture<T> future = asyncHttpClient.executeRequest(request, hander);
        return future.toCompletableFuture();
}
}
