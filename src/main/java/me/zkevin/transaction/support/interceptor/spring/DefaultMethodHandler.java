package me.zkevin.transaction.support.interceptor.spring;

import me.zkevin.transaction.support.SessionHook;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * spring controller拦截器
 * Created by zkevin on 16/12/5.
 */
public class DefaultMethodHandler implements HandlerInterceptor {
    private static final Logger log= Logger.getLogger(DefaultMethodHandler.class);
    //事务提交回滚钩子
    private SessionHook hook;
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        hook.preHook();
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        long now=System.currentTimeMillis();
        hook.postHook();
        log.debug(request.getRequestURL() + " spend:" + (System.currentTimeMillis() - now));
    }

    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        hook.afterHook();
    }
    public void setHook(SessionHook hook) {
        this.hook = hook;
    }
}

