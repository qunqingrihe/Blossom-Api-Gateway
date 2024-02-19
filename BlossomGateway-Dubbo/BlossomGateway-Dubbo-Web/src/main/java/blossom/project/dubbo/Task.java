package blossom.project.dubbo;
import java.util.Date;

import blossom.project.dubbo.service.DubboRPCService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
@Component
public class Task implements CommandLineRunner{
    @DubboReference
    private DubboRPCService dubboRPCService;
    /**
 * 重写run方法，实现核心业务逻辑。
 *
 * @param args 命令行参数数组
 * @throws Exception 如果在执行过程中发生任何异常
 */
@Override
public void run(String... args) throws Exception {
    // 调用dubboRPCService的testDubboRPC方法获取结果，并将其存储在result变量中
    String result = dubboRPCService.testDubboRPC("hello world");
    System.out.println("接收到的结果=====>" + result);
    // 启动一个新的线程，该线程会持续每秒调用一次dubboRPC服务并打印返回结果及当前时间
    new Thread(()->{
        while(true){
            try {
                // 让线程休眠1秒
                Thread.sleep(1000);
                // 调用服务并打印返回结果和当前日期时间
                System.out.println(new Date() + "接收到的结果=====>" + dubboRPCService.testDubboRPC("hello world"));
            } catch (InterruptedException e) {
                // 捕获中断异常，打印堆栈信息并中断当前线程
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            // 打印当前系统时间
            System.out.println("当前时间=====>" + new Date());
        }
    }).start();
}

}
