
package blossom.project.common.config;

import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态服务缓存配置管理类  用于缓存从配置中心拉取下来的配置
 * 不太好理解 了解Nacos源码的可能会好理解一点
 */
public class DynamicConfigManager {

    //	服务的定义集合：uniqueId代表服务的唯一标识
    private ConcurrentHashMap<String /* uniqueId */ , ServiceDefinition>  serviceDefinitionMap = new ConcurrentHashMap<>();

    //	服务的实例集合：uniqueId与一对服务实例对应
    private ConcurrentHashMap<String /* uniqueId */ , Set<ServiceInstance>>  serviceInstanceMap = new ConcurrentHashMap<>();

    //	规则集合
    private ConcurrentHashMap<String /* ruleId */ , Rule>  ruleMap = new ConcurrentHashMap<>();

    //路径以及规则集合
    private ConcurrentHashMap<String /* 路径 */ , Rule>  pathRuleMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String /* 服务名 */ , List<Rule>>  serviceRuleMap = new ConcurrentHashMap<>();

    private DynamicConfigManager() {
    }

    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }


    /***************** 	对服务定义缓存进行操作的系列方法 	***************/

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void putServiceDefinition(String uniqueId,
                                     ServiceDefinition serviceDefinition) {
        serviceDefinitionMap.put(uniqueId, serviceDefinition);;
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }

    /***************** 	对服务实例缓存进行操作的系列方法 	***************/

    /**
     * 根据唯一标识获取服务实例缓存
     * @param uniqueId 唯一标识
     * @param gray 是否为灰度流量
     * @return 服务实例缓存集合
     */
    public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray){
        Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
        if (CollectionUtils.isEmpty(serviceInstances)) {
            return Collections.emptySet();
        }
        //不为空且为灰度流量
        if (gray) {
            return  serviceInstances.stream()
                    .filter(ServiceInstance::isGray)
                    .collect(Collectors.toSet());
        }

        return serviceInstances;
    }

    public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        set.add(serviceInstance);
    }

    public void addServiceInstance(String uniqueId, Set<ServiceInstance> serviceInstanceSet) {
        serviceInstanceMap.put(uniqueId, serviceInstanceSet);
    }

    /**
     * 更新服务实例
     * @param uniqueId 服务实例唯一标识
     * @param serviceInstance 要更新的服务实例
     */
    public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        // 获取服务实例集合
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        // 遍历服务实例集合
        Iterator<ServiceInstance> it = set.iterator();
        while (it.hasNext()) {
            ServiceInstance is = it.next();
            // 判断要更新的服务实例是否已存在于集合中
            if (is.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
                // 如果存在，则移除该服务实例
                it.remove();
                break;
            }
        }
        // 将要更新的服务实例添加到集合中
        set.add(serviceInstance);
    }
    /**
     * 从服务实例映射中移除指定的服务实例
     * @param uniqueId 服务实例的唯一标识
     * @param serviceInstanceId 要移除的服务实例ID
     */
    public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = set.iterator();
        while(it.hasNext()) {
            ServiceInstance is = it.next();
            if(is.getServiceInstanceId().equals(serviceInstanceId)) {
                it.remove();
                break;
            }
        }
    }

    public void removeServiceInstancesByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }


    /***************** 	对规则缓存进行操作的系列方法 	***************/
    /**
     * 将规则添加到规则映射中
     * @param ruleId 规则ID
     * @param rule 规则对象
     */
    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    /**
     * 将规则列表添加到当前对象中
     * @param ruleList 规则列表
     */
    public void putAllRule(List<Rule> ruleList) {
        // 创建新的规则映射
        ConcurrentHashMap<String,Rule> newRuleMap = new ConcurrentHashMap<>();
        // 创建新的路径映射
        ConcurrentHashMap<String,Rule> newPathMap = new ConcurrentHashMap<>();
        // 创建新的服务映射
        ConcurrentHashMap<String,List<Rule>> newServiceMap = new ConcurrentHashMap<>();

        // 遍历规则列表
        for(Rule rule : ruleList){
            // 将规则添加到新的规则映射中
            newRuleMap.put(rule.getId(),rule);

            // 获取服务映射中对应服务的规则列表
            List<Rule> rules = newServiceMap.get(rule.getServiceId());

            // 如果规则列表为空，则创建一个新的列表
            if(rules == null){
                rules = new ArrayList<>();
            }

            // 将规则添加到对应服务的规则列表中
            rules.add(rule);
            // 将更新后的规则列表添加到服务映射中
            newServiceMap.put(rule.getServiceId(),rules);

            // 获取规则的路径列表
            List<String> paths = rule.getPaths();

            // 遍历路径列表
            for(String path :paths){
                // 构建键值对
                String key = rule.getServiceId()+"."+path;

                // 将规则添加到新的路径映射中
                newPathMap.put(key,rule);
            }
        }

        // 更新当前对象的规则映射、路径映射和服务映射
        ruleMap = newRuleMap;
        pathRuleMap = newPathMap;
        serviceRuleMap = newServiceMap;
    }


    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }

    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public Rule  getRuleByPath(String path){
        return pathRuleMap.get(path);
    }

    public List<Rule>  getRuleByServiceId(String serviceId){
        return serviceRuleMap.get(serviceId);
    }
}