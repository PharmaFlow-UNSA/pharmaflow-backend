package com.pharmaflow.smartfeatures.client;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthorizationHeaderForwardingInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String authorizationHeader = currentAuthorizationHeader();
    if (StringUtils.hasText(authorizationHeader)
        && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorizationHeader);
    }

    return execution.execute(request, body);
  }

  private String currentAuthorizationHeader() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
      return null;
    }

    HttpServletRequest request = servletRequestAttributes.getRequest();
    return request.getHeader(HttpHeaders.AUTHORIZATION);
  }
}
