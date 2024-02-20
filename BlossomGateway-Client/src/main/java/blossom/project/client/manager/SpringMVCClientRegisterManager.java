package blossom.project.client.manager;

import blossom.project.client.api.ApiAnnotationScanner;
import blossom.project.client.api.ApiProperties;
import blossom.project.common.config.ServiceDefinition;
import blossom.project.common.config.ServiceInstance;
import blossom.project.common.utils.NetUtils;
import blossom.project.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static blossom.project.common.constant.BasicConst.COLON_SEPARATOR;
import static blossom.project.common.constant.GatewayConst.DEFAULT_WEIGHT;

@Slf4j
public class SpringMVCClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Autowired
    private ServerProperties serverProperties;

    private Set<Object> set = new HashSet<>();

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 监听spring启动事件
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 如果事件是ApplicationStartedEvent类型
        if (applicationEvent instanceof ApplicationStartedEvent) {
            try {
                // 执行具体的springmvc项目注册
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }

            log.info("springmvc api started");
        }
    }

    private void doRegisterSpringMvc() {
        //获取RequestMappingHandlerMapping类型的bean实例，了解springmvc源码的就知道其实这些就是得到我们的controller的信息
        Map<String, RequestMappingHandlerMapping> allRequestMappings=BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);
        //遍历所有请求处理器
        for(RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()){
            //这里的handlerMapping可以简单理解为我们的controller类
            Map<RequestMappingInfo, HandlerMethod> handlerMethods=handlerMapping.getHandlerMethods();
            for(Map.Entry<RequestMappingInfo, HandlerMethod>me : handlerMethods.entrySet()){
                //获取方法
                HandlerMethod handlerMethod=me.getValue();
                //todo
                Class<?> clazz=handlerMethod.getBeanType();
                Object bean=applicationContext.getBean(clazz);
                //存储方法
                if(set.contains(bean)){
                    continue;
                }
                //扫描服务得到服务信息
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);
                if(serviceDefinition==null){
                    continue;
                }
                //设定环境信息
                serviceDefinition.setEnvType(getApiProperties().getEnv());
                //服务实例 本不应该创捷 浪费空间 方便自己学习
                ServiceInstance serviceInstance = new ServiceInstance();
                String localIp = NetUtils.getLocalIp();
                int port = serverProperties.getPort();
                String serviceInstanceId = localIp + COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();

                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(DEFAULT_WEIGHT);


                if (getApiProperties().isGray()){
                    serviceInstance.setGray(true);
                }

                //注册
                register(serviceDefinition, serviceInstance);

            }
        }
    }
}
