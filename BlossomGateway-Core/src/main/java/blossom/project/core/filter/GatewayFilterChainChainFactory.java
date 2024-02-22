package blossom.project.core.filter;
import blossom.project.common.config.Rule;
import blossom.project.common.constant.FilterConst;
import blossom.project.core.context.GatewayContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
@Slf4j
public class GatewayFilterChainChainFactory implements FilterChainFactory{
    private static class SinletonHolder {
        private static final GatewayFilterChainChainFactory INSTANCE = new GatewayFilterChainChainFactory();
    }
    /**
     * 获取实例
     * @return GatewayFilterChainChainFactory实例
     */
    public static GatewayFilterChainChainFactory getInstance() {
        return SinletonHolder.INSTANCE;
    }
    /**
     * 使用Caffeine缓存 并且设定过期时间10min
     */
    private Cache<String, GatewayFilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10,
            TimeUnit.MINUTES).build();
    /**
     * 过滤器存储映射 过滤器id - 过滤器
     */
    private Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();
    /**
     * 构造方法
     */
    public GatewayFilterChainChainFactory() {
        //加载所有过滤器
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        serviceLoader.stream().forEach(filterProvider->{
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            log.info("load filter success:{},{},{},{}", filter.getClass(), annotation.id(), annotation.name(),
                    annotation.order());
            if(annotation.name() != null){
                //添加到过滤集合
                String filterId = annotation.id();
                if (StringUtils.isEmpty(filterId)) {
                    filterId = filter.getClass().getName();
                }
                processorFilterIdMap.put(filterId, filter);
            }
        });
    }
    /**
     * 测试方法
     * @param args 参数
     */
    public static void main(String[] args) {
        new GatewayFilterChainChainFactory();
    }
    /**
     * 构建过滤器链
     * @param ctx 上下文
     * @return GatewayFilterChain实例
     * @throws Exception 异常
     */
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        return chainCache.get(ctx.getRule().getId(),k->doBuildFilterChain(ctx.getRule()));
        //return doBuildFilterChain(ctx.getRule());
    }
    /**
     * 构建过滤器链
     * @param rule 规则
     * @return GatewayFilterChain实例
     */
    public GatewayFilterChain doBuildFilterChain(Rule rule){
        GatewayFilterChain chain=new GatewayFilterChain();
        List<Filter> filters=new ArrayList();
        //手动将某些过滤器加入到过滤器链中
        filters.add(getFilterInfo(FilterConst.GRAY_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MOCK_FILTER_ID));
        if(rule!=null){
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator iterator =filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while(iterator.hasNext()){
                filterConfig=(Rule.FilterConfig) iterator.next();
                if(filterConfig==null){
                    continue;
                }
                String filterId=filterConfig.getId();
                if(StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null){
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        //添加路由过滤器-这是最后一步
        filters.add(getFilterInfo(FilterConst.ROUTER_FILTER_ID));
        //排序
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        //添加到链表中
        chain.addFilterList(filters);
        return chain;
    }

    /**
     * 获取过滤器信息
     * @param filterId 过滤器id
     * @return Filter实例
     */
    @Override
    public Filter getFilterInfo(String filterId) {
        return processorFilterIdMap.get(filterId);
    }
}
