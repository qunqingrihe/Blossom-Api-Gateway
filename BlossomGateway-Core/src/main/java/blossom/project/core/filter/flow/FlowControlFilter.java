package blossom.project.core.filter.flow;

import blossom.project.common.config.Rule;
import blossom.project.core.context.GatewayContext;
import blossom.project.core.filter.Filter;
import blossom.project.core.filter.FilterAspect;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Set;

import static blossom.project.common.constant.FilterConst.*;

/** FlowControlFilter类
 * 流量控制过滤器
 * 常见流量控制方法有令牌桶和漏桶算法
 */
@Slf4j
@FilterAspect(id=FLOW_CTL_FILTER_ID,
                name=FLOW_CTL_FILTER_NAME,
                order=FLOW_CTL_FILTER_ORDER)
public class FlowControlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
            Rule rule=ctx.getRule();
            if(rule!=null){
                Set<Rule.FlowControlConfig> flowControlConfigs=rule.getFlowControlConfigs();
                Iterator iterator=flowControlConfigs.iterator();
                Rule.FlowControlConfig flowControlConfig;
                while (iterator.hasNext()) {
                    GatewayFlowControlRule flowControlRule=null;
                    flowControlConfig=(Rule.FlowControlConfig) iterator.next();
                    if(flowControlConfig==null){
                        continue;
                    }
                    //*http-server*//ping
                    String path=ctx.getRequest().getPath();
                    if(flowControlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_PATH)&&
                    path.equals(flowControlConfig.getValue())){
                        flowControlRule=FlowControlByPathRule.getInstance(rule.getServiceId(),path);
                    }else if(flowControlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_SERVICE)){
                        //todo 可以实现自己的服务流控
                    }
                    if(flowControlRule!=null){
                        //流量控制
                        flowControlRule.doFlowControlFilter(flowControlConfig,rule.getServiceId() );
                    }
                }
            }
    }
}
