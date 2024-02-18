package blossom.project.common.utils;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
public class RemotingHelper {

    /**
     * 获取异常的简单描述
     * @param e 异常对象
     * @return 异常的简单描述
     */
    public static String exceptionSimpleDesc(final Throwable e) {
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());

            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement elment = stackTrace[0];
                sb.append(", ");
                sb.append(elment.toString());
            }
        }

        return sb.toString();
    }


    /**
     * 将字符串转换为Socket地址
     * @param addr 字符串地址
     * @return Socket地址
     */
    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
        return isa;
    }
    /**
     * 解析Channel的远程地址
     * @param channel Channel对象
     * @return 远程地址
     */
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }
    /**
     * 解析Socket地址的地址部分
     * @param socketAddress Socket地址对象
     * @return 地址部分
     */
    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }


}
