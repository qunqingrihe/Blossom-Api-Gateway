package blossom.project.core.filter.Mock;

/** MockFilter类
 * 前端mock过滤器
 */

import blossom.project.common.config.Rule;
import blossom.project.common.utils.JSONUtil;
import blossom.project.core.context.GatewayContext;
import blossom.project.core.filter.Filter;
import blossom.project.core.filter.FilterAspect;
import blossom.project.core.helper.ResponseHelper;
import blossom.project.core.response.GatewayResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static blossom.project.common.constant.FilterConst.*;

@Slf4j
@FilterAspect(id=MOCK_FILTER_ID,
        name = MOCK_FILTER_NAME,
        order = MOCK_FILTER_ORDER)
public class MockFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //若没有配置Mock 直接返回
        Rule.FilterConfig config=ctx.getRule().getFilterConfig(MOCK_FILTER_ID);
        if(config==null){
            return;
        }
        //解析
        Map<String,String> map=JSONUtil.parse(config.getConfig(),Map.class);
        String value=map.get(ctx.getRequest().getMethod().name() + " " + ctx.getRequest().getPath());
        //不为空说明命中了mock规则
        if(value!=null){
            ctx.setResponse(GatewayResponse.buildGatewayResponse(value));
            ctx.written();
            //数据写回
            ResponseHelper.writeResponse(ctx);
            log.info("mock {} {} {}", ctx.getRequest().getMethod(), ctx.getRequest().getPath(), value);
            ctx.terminated();
        }
    }
}