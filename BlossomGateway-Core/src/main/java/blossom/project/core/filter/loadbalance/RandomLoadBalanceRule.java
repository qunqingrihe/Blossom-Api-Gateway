package blossom.project.core.filter.loadbalance;

import blossom.project.common.config.DynamicConfigManager;
import blossom.project.common.config.ServiceInstance;
import blossom.project.common.exception.NotFoundException;
import blossom.project.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import static blossom.project.common.enums.ResponseCode.SERVICE_INSTANCE_NOT_FOUND;

/**当前类用于提供随机抽取后端服务的负载均衡实现
 */
@Slf4j
public class RandomLoadBalanceRule implements LoadBalanceGatewayRule {


    private final String serviceId;

    /**
     * 服务列表
     */
    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    /**
     * 获取指定服务ID的负载均衡规则实例
     * @param serviceId 服务ID
     * @return 负载均衡规则实例
     */
    public static RandomLoadBalanceRule getInstance(String serviceId) {
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }



    /**
     * 重写父类方法，根据给定的网关上下文和是否开启灰度选择服务实例
     * @param ctx 网关上下文
     * @param gray 是否开启灰度
     * @return 选择的服务实例
     */
    @Override
    public ServiceInstance choose(GatewayContext ctx, boolean gray) {
        String serviceId = ctx.getUniqueId();
        return choose(serviceId, gray);
    }


    /**
     * 根据服务ID和是否开启灰度选择服务实例
     * @param serviceId 服务ID
     * @param gray 是否开启灰度
     * @return 选择的服务实例
     */
    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        // 根据服务ID和是否开启灰度获取服务实例集合
        Set<ServiceInstance> serviceInstanceSet =
                DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        // 如果服务实例集合为空，则打印日志并抛出异常
        if (serviceInstanceSet.isEmpty()) {
            log.warn("No instance available for:{}", serviceId);
            throw new NotFoundException(SERVICE_INSTANCE_NOT_FOUND);
        }
        // 将服务实例集合转换为列表
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);
        // 生成随机索引
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        // 根据随机索引获取服务实例
        ServiceInstance instance = (ServiceInstance) instances.get(index);
        // 返回选择的服务实例
        return instance;
    }

}
