package net.hostsharing.hsadminng.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContextUnitTest {

    private String defineContextQueryString = """
            call defineContext(
                cast(:currentTask as varchar),
                cast(:currentRequest as varchar),
                cast(:currentUser as varchar),
                cast(:assumedRoles as varchar));
            """;

    @Nested
    class WithoutHttpRequest {

        @Mock
        EntityManager em;

        @Mock
        Query nativeQuery;

        @InjectMocks
        Context context;

        @BeforeEach
        void setup() {
            RequestContextHolder.setRequestAttributes(null);
            given(em.createNativeQuery(any())).willReturn(nativeQuery);
        }

        @Test
        void registerWithoutHttpServletRequestUsesCallStackForTask() {
            given(em.createNativeQuery(any())).willReturn(nativeQuery);

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter(
                    "currentTask",
                    "WithoutHttpRequest.registerWithoutHttpServletRequestUsesCallStackForTask");
        }

        @Test
        void registerWithoutHttpServletRequestUsesEmptyStringForRequest() {
            given(em.createNativeQuery(any())).willReturn(nativeQuery);

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter("currentRequest", "");
        }
    }

    @Nested
    class WithHttpRequest {

        @Mock
        EntityManager em;

        @Mock
        Query nativeQuery;

        @Mock
        HttpServletRequest request;

        @Mock
        RequestAttributes requestAttributes;

        @Mock
        BufferedReader requestBodyReader;

        @Mock
        Stream<String> requestBodyLines;

        @InjectMocks
        Context context;

        @BeforeEach
        void setup() {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            given(em.createNativeQuery(any())).willReturn(nativeQuery);
        }

        @Test
        void registerWithHttpServletRequestUsesRequest() throws IOException {
            givenRequest("POST", "http://localhost:9999/api/endpoint", Map.ofEntries(
                            Map.entry("current-user", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter("currentTask", "POST http://localhost:9999/api/endpoint");
        }

        @Test
        void registerWithHttpServletRequestForwardsRequestAsCurl() throws IOException {
            givenRequest("POST", "http://localhost:9999/api/endpoint", Map.ofEntries(
                            Map.entry("current-user", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter("currentRequest", """
                    curl -0 -v -X POST http://localhost:9999/api/endpoint \\
                    -H 'current-user:given-user' \\
                    -H 'content-type:application/json' \\
                    --data-binary @- << EOF
                                            
                    {}
                    EOF
                    """.trim());
        }

        @Test
        void shortensCurrentTaskTo96Chars() throws IOException {
            givenRequest("GET", "http://localhost:9999/api/endpoint/" + "0123456789".repeat(10),
                    Map.ofEntries(
                            Map.entry("current-user", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter(eq("currentTask"), argThat((String t) -> t.length() == 96));
        }

        @Test
        void shortensCurrentRequestTo512Chars() throws IOException {
            givenRequest("GET", "http://localhost:9999/api/endpoint",
                    Map.ofEntries(
                            Map.entry("current-user", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    """
                               {
                                   "dummy": "%s"
                               }
                            """.formatted("0123456789".repeat(60)));

            context.define("current-user");

            verify(em).createNativeQuery(defineContextQueryString);
            verify(nativeQuery).setParameter(eq("currentRequest"), argThat((String t) -> t.length() == 512));
        }

        private void givenRequest(final String method, final String url, final Map<String, String> headers, final String body)
                throws IOException {
            given(request.getMethod()).willReturn(method);
            given(request.getRequestURI()).willReturn(url);
            given(request.getHeaderNames()).willReturn(Collections.enumeration(headers.keySet()));
            given(request.getHeader(anyString())).willAnswer(invocation -> headers.get(invocation.getArgument(0).toString()));
            given(request.getReader()).willReturn(requestBodyReader);
            given(requestBodyReader.lines()).willReturn(requestBodyLines);
            given(requestBodyLines.collect(any())).willReturn(body);
        }
    }
}
