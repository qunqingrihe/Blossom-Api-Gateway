package blossom.project.httpserver.controller;


import blossom.project.client.api.ApiInvoker;
import blossom.project.client.api.ApiProperties;
import blossom.project.client.api.ApiProtocol;
import blossom.project.client.api.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class Httpcontroller {
    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() {
        log.info("{}", apiProperties);
        try {
            //Thread.sleep(10000000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "this is application2";
    }
}
