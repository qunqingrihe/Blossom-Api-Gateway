package blossom.project.common.utils;

import java.lang.reflect.Method;
import java.util.Properties;

public class PropertiesUtils {

    /**
     * 将Properties对象的属性值设置到指定对象上
     *
     * @param p Properties对象
     * @param object 目标对象
     * @param prefix 前缀字符串
     */
    public static void properties2Object(final Properties p, final Object object, String prefix) {
        // 获取目标对象的所有方法
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    // 设置属性值
                    String tmp = mn.substring(4);
                    // 获取属性名
                    String first = mn.substring(3, 4);
                    String key = prefix + first.toLowerCase() + tmp;
                    String property = p.getProperty(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg = null;
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            // 调用方法设置属性值
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static void properties2Object(final Properties p, final Object object) {
        properties2Object(p, object, "");
    }

}

