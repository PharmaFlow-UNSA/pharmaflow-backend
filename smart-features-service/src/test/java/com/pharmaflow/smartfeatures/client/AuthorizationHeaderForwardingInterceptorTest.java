package com.pharmaflow.smartfeatures.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuthorizationHeaderForwardingInterceptorTest {

  private final AuthorizationHeaderForwardingInterceptor interceptor =
      new AuthorizationHeaderForwardingInterceptor();

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void interceptShouldForwardCurrentAuthorizationHeader() throws Exception {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer user-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

    interceptor.intercept(
        new MockClientHttpRequest(
            HttpMethod.GET, URI.create("http://user-health-service/api/users/2")),
        new byte[0],
        (request, body) -> {
          assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
              .isEqualTo("Bearer user-token");
          return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });
  }

  @Test
  void interceptShouldLeaveExistingAuthorizationHeaderUntouched() throws Exception {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer user-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

    MockClientHttpRequest clientRequest =
        new MockClientHttpRequest(
            HttpMethod.GET, URI.create("http://user-health-service/api/users/2"));
    clientRequest.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer service-token");

    interceptor.intercept(
        clientRequest,
        new byte[0],
        (request, body) -> {
          assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
              .isEqualTo("Bearer service-token");
          return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });
  }
}
