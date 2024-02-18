package blossom.project.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {
    /**IP正则**/
    public static Pattern pattern=Pattern.compile("(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
            + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");
    /**
     * 验证IP列表是否有效
     * @param ipList IP列表
     * @return 验证结果，true表示有效，false表示无效
     */
    public static boolean validate(List<String> ipList){
        if(null!=ipList){
            for(String ip:ipList){
                if(!pattern.matcher(ip).matches()){
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * 验证规则是否有效
     * @param ipList IP列表
     * @return 验证结果，true表示有效，false表示无效
     */
    public static boolean validateRule(List<String> ipList){
        if(null!=ipList){
            for(String ip:ipList){
                int nMaskBits = 1;
                if (ip.indexOf('/') > 0) {
                    String[] addressAndMask = StringUtils.split(ip, "/");
                    ip = addressAndMask[0];
                    nMaskBits = Integer.parseInt(addressAndMask[1]);
                }
                if(!pattern.matcher(ip).matches()||nMaskBits<0||nMaskBits>32){
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * 判断字符串是否为合法的HTTP或HTTPS网址
     * @param urls 待判断的字符串
     * @return 如果是合法的HTTP或HTTPS网址则返回true，否则返回false
     */
    public static boolean isHttpUrl(String urls){
        //	设置正则表达式
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";
        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        return mat.matches();
    }
    /**
     * 将给定的地址规范化，将其拆分为主机名和端口号。
     * 如果地址中包含端口号，则会使用该端口号；否则将使用默认端口80。
     * 若地址格式不正确，将会抛出IllegalArgumentException异常。
     * @param address 需要规范化的地址
     * @return 规范化后的地址格式为 "主机名:端口号"
     * @throws IllegalArgumentException 当地址格式不合法时抛出此异常
     */
    public static String normalizeAddress(String address) {
        String[] blocks = address.split("[:]");
        if (blocks.length > 2) {
            throw new IllegalArgumentException(address + " 格式不正确");
        }
        String host = blocks[0];
        int port = 80; // 默认端口
        if (blocks.length > 1) {
            port = Integer.valueOf(blocks[1]);
        } else {
            address += ":" + port; // 使用默认端口80
        }
        String serverAddr = String.format("%s:%d", host, port);
        return serverAddr;
    }
    /**
     * 获取本地地址
     * @param address 地址字符串
     * @return 本地地址
     * @throws IllegalArgumentException 地址格式不正确时抛出此异常
     */
    public static String getLocalAddress(String address) {
        String[] blocks = address.split("[:]");
        if (blocks.length != 2) {
            throw new IllegalArgumentException(address + " is invalid address");
        }
        String host = blocks[0];
        int port = Integer.valueOf(blocks[1]);

        if ("0.0.0.0".equals(host)) {
            return String.format("%s:%d", NetUtils.getLocalIp(), port);
        }
        return address;
    }
    /**
     * 在给定的前缀数组中查找与指定IP地址匹配的前缀索引。
     *
     * @param ip 需要匹配前缀的IP地址
     * @param prefix 用于搜索匹配项的前缀数组
     * @return 匹配到的前缀的索引，若无匹配项则返回-1
     */
    private static int matchedIndex(String ip, String[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            String p = prefix[i];
            if ("*".equals(p)) { // 符号"*"代表匹配任意IP
                if (ip.startsWith("127.") ||
                        ip.startsWith("10.") ||
                        ip.startsWith("172.") ||
                        ip.startsWith("192.")) {
                    continue; // 若IP地址属于本地或私有地址范围，则继续查找下一个前缀
                }
                return i; // 返回当前索引位置，表示匹配成功
            } else {
                if (ip.startsWith(p)) {
                    return i; // 如果IP地址以当前前缀开头，则返回当前索引位置，表示匹配成功
                }
            }
        }

        return -1; // 若遍历完所有前缀都没有匹配到，则返回-1
    }
    /**
     * 获取本地IP地址
     * @param ipPreference IP地址的优先级，格式为">10>172>192>127"
     * @return 本地IP地址
     */
    public static String getLocalIp(String ipPreference) {
        if (ipPreference == null) {
            ipPreference = "*>10>172>192>127";
        }
        String[] prefix = ipPreference.split("[> ]+");
        try {
            Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String matchedIp = null;
            int matchedIdx = -1;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // 跳过虚拟网卡
                if(ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> en = ni.getInetAddresses();
                // 跳过虚拟网卡
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    if(addr.isLoopbackAddress() ||
                            !addr.isSiteLocalAddress() ||
                            addr.isAnyLocalAddress()) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    Matcher matcher = pattern.matcher(ip);
                    if (matcher.matches()) {
                        int idx = matchedIndex(ip, prefix);
                        if (idx == -1) {
                            continue;
                        }
                        if (matchedIdx == -1) {
                            matchedIdx = idx;
                            matchedIp = ip;
                        } else {
                            if (matchedIdx > idx) {
                                matchedIdx = idx;
                                matchedIp = ip;
                            }
                        }
                    }
                }
            }
            if (matchedIp != null)
                return matchedIp;
            return "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
    public static String getLocalIp() {
        return getLocalIp("*>10>172>192>127");
    }

    /**
     * 获取SocketChannel的远端地址
     * @param channel SocketChannel对象
     * @return 远端地址的字符串表示
     */
    public static String remoteAddress(SocketChannel channel) {
        // 获取SocketChannel的socket对象
        Socket socket = channel.socket();
        // 获取远端地址
        SocketAddress addr = socket.getRemoteSocketAddress();
        // 将远端地址转换为字符串
        String res = String.format("%s", addr);
        return res;
    }


    /**
     * 获取SocketChannel的本地地址
     * @param channel SocketChannel对象
     * @return 本地地址的字符串表示
     */
    public static String localAddress(SocketChannel channel) {
        // 获取SocketChannel的socket对象
        Socket socket = channel.socket();
        // 获取本地地址
        SocketAddress addr = socket.getLocalSocketAddress();
        // 将本地地址转换为字符串
        String res = String.format("%s", addr);
        // 如果本地地址为null，则直接返回转换后的字符串
        // 否则，返回转换后的字符串去掉开头的斜杠
        return addr == null ? res : res.substring(1);
    }


    /**
     * 获取当前Java进程的PID
     * @return 当前Java进程的PID，如果获取失败则返回null
     */
    public static String getPid() {
        // 获取RuntimeMXBean对象
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // 获取Java进程的名称
        String name = runtime.getName();
        // 查找Java进程名称中"@"符号的索引
        int index = name.indexOf("@");
        // 如果找到了"@"符号，则截取索引之前的部分作为PID
        if (index != -1) {
            return name.substring(0, index);
        }
        // 如果没有找到"@"符号，则返回null
        return null;
    }


    /**
     * 获取本机的主机名
     * @return 本机的主机名
     */
    public static String getLocalHostName() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage();
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return "UnknownHost";
        }
    }

}
