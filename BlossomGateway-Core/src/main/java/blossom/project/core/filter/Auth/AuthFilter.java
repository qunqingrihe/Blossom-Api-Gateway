package blossom.project.core.filter.Auth;

import blossom.project.common.enums.ResponseCode;
import blossom.project.common.exception.ResponseException;
import blossom.project.core.context.GatewayContext;
import blossom.project.core.filter.Filter;
import blossom.project.core.filter.FilterAspect;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import static blossom.project.common.constant.FilterConst.*;

/** AuthFilter类
         * 用户JWT鉴权过滤器
         */
@Slf4j
@FilterAspect(id= AUTH_FILTER_ID,
        name = AUTH_FILTER_NAME,
        order =AUTH_FILTER_ORDER )
public class AuthFilter implements Filter {
    /*加密密钥*/
    private static final String SECRET_KEY = "Aminoac";
    /*cookie 从对应的cookie中获取到这个键，存储的就是我们的tokens信息*/
    private static final String COOKIE_NAME="Aminoac-jwt";

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        if(ctx.getRule().getFilterConfig(AUTH_FILTER_ID)==null){
            return;
        }
        String token=ctx.getRequest().getCookie(COOKIE_NAME).value();
        if(StringUtils.isBlank(token)){
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
        try{
        //解析用户id
            long userId=parseUserId(token);
            //把用户id传给下游
            ctx.getRequest().setUserId(userId);
        }catch (Exception e){
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
    }
    private long parseUserId(String token) {
        Jwt jwt = Jwts.parser().setSigningKey(SECRET_KEY).parse(token);
        return Long.parseLong(((DefaultClaims)jwt.getBody()).getSubject());
    }
}
