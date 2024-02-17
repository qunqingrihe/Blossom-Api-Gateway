package blossom.project.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@AllArgsConstructor
@Builder
public class Rule implements Comparable<Rule>, Serializable {
    /**
     * 规则ID，全局唯一
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 协议
     */
    private String protocol;

    /**
     * 后端服务ID
     */
    private String serviceId;
    /**
     * 请求前缀
     */
    private String prefix;
    /**
     * 路径集合
     */
    private List<String> paths;
    /**
     * 规则排序，对应场景：一个路径对应多条规则，然后只执行一条规则的情况
     */
    private Integer order;

    /**
     * 过滤器配置信息
     */
    private Set<FilterConfig> filterConfigs = new HashSet<>();

    /**
     * 限流规则
     */
    private Set<FlowControlConfig> flowControlConfigs = new HashSet<>();
    /**
     * 重试规则
     */
    private RetryConfig retryConfig = new RetryConfig();
    /**
     * 熔断规则
     */
    private Set<HystrixConfig> hystrixConfigs = new HashSet<>();

    public Rule() {
        super();
    }
    public Rule(String id, String name, String protocol, String serviceId, String prefix, List<String> paths,
                Integer order, Set<FilterConfig> filterConfigs) {
        super();
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.prefix = prefix;
        this.paths = paths;
        this.order = order;
    }
    @Data
    public static class FilterConfig {
        /**
         * 过滤器唯一ID
         */
        private String id;
        /**
         * 过滤器规则描述，{"timeOut":500,"balance":random}
         */
        private String config;
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FilterConfig that = (FilterConfig) o;
            return id.equals(that.id);
        }
        @Override
        public int hashCode() {return Objects.hash(id);}
    }
    @Data
    public static class FlowControlConfig {
        /**
         * 限流类型-可能是path，也可能是IP或者服务
         */
        private String type;
        /**
         * 限流对象的值
         */
        private String value;
        /**
         * 限流模式-单机还有分布式
         */
        private String model;
        /**
         * 限流规则,是一个JSON
         */
        private String config;
    }

    @Data
    public static class RetryConfig {
        private int times;

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }
    @Data
    public static class HystrixConfig {
        /**
         * 熔断降级路径
         */
        private String path;
        /**
         * 超时时间
         */
        private int timeoutInMilliseconds;
        /**
         * 核心线程数量
         */
        private int threadCoreSize;
        /**
         * 熔断降级响应
         */
        private String fallbackResponse;
    }
    /**
     * 向规则里面添加过滤器
     *
     * @param filterConfig
     * @return
     */
    public boolean addFilterConfig(FilterConfig filterConfig) {
        return filterConfigs.add(filterConfig);
    }
    /**
     * 通过一个指定的FilterID获取FilterConfig
     *
     * @param id
     * @return
     */
    public FilterConfig getFilterConfig(String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equals(id)) {
                return filterConfig;
            }
        }
        return null;
    }
    public boolean hashId(String id) {
        // 遍历filterConfigs
        for(FilterConfig filterConfig : filterConfigs){
            // 判断filterConfig的ID是否与给定的id相等
            if (filterConfig.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 重写了compareTo方法，用于比较当前对象和另一个Rule对象的顺序和ID。
     * 如果两个对象的顺序相同，则比较它们的ID。
     * 如果顺序不同，则返回顺序的比较结果。
     */
    @Override
    public int compareTo(Rule o) {
        // 比较当前对象和另一个Rule对象的顺序
        int orderCompare = Integer.compare(this.order, o.getOrder());

        // 如果顺序相同，则比较它们的ID
        if(orderCompare==0){
            return getId().compareTo(o.getId());
        }

        // 返回顺序的比较结果
        return orderCompare;
    }

    /**
     * 重写了equals方法，用于判断当前对象是否与另一个对象相等。
     * 相等的条件是两个对象的id相等。
     */
    @Override
    public boolean equals(Object o) {
        // 检查是否为同一个对象
        if(this == o){
            return true;
        }

        // 检查是否为null或者不是同一种类的对象
        if(o == null || getClass() != o.getClass()){
            return false;
        }

        // 强制类型转换为FilterConfig对象
        FilterConfig that=(FilterConfig) o;

        // 比较两个对象的id是否相等
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }//计算当前对象的哈希码。
}
