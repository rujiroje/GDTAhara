package com.gdtahara.gdtaharabackend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock private FilterChain chain;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    // Note: tests call doFilterInternal() directly — getServletPath() is NOT called
    // by doFilterInternal; it is only called by shouldNotFilter(). Tests that call
    // doFilterInternal directly must NOT stub getServletPath() to avoid strict-mode errors.

    @Test
    void underMaxAttempts_passesThroughToChain() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        verify(chain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void overMaxAttempts_returns429AndBlocksChain() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        // 11th attempt must be blocked
        filter.doFilterInternal(request, response, chain);

        verify(response, atLeastOnce()).setStatus(429);
        verify(chain, times(10)).doFilter(request, response);
        assertThat(sw.toString()).contains("Too many login attempts");
    }

    @Test
    void differentIps_trackedSeparately() throws Exception {
        HttpServletRequest requestB = mock(HttpServletRequest.class);
        when(requestB.getRemoteAddr()).thenReturn("10.0.0.2");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // IP-A: exhaust its limit
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        // IP-A 11th → blocked
        filter.doFilterInternal(request, response, chain);

        // IP-B must still pass (separate bucket)
        filter.doFilterInternal(requestB, response, chain);

        // chain was called 10 (IP-A ok) + 1 (IP-B ok) = 11 times
        verify(chain, times(11)).doFilter(any(), eq(response));
    }

    @Test
    void nonLoginPath_skipsRateLimiting() {
        when(request.getServletPath()).thenReturn("/api/production/reports");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void loginPath_isNotSkipped() {
        when(request.getServletPath()).thenReturn("/api/auth/login");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}
