package net.hostsharing.hsadminng.rbac.context;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContextUnitTest {

    private static final String DEFINE_CONTEXT_QUERY_STRING = """
           call base.defineContext(
               cast(:currentTask as varchar(127)),
               cast(:currentRequest as text),
               cast(:currentSubject as varchar(63)),
               cast(:assumedRoles as text));
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
            lenient().when(em.createNativeQuery(any())).thenReturn(nativeQuery);
        }

        @Test
        void registerWithoutHttpServletRequestUsesCallStackForTask() {
            given(em.createNativeQuery(any())).willReturn(nativeQuery);

            context.define("current-subject");

            verify(em).createNativeQuery(DEFINE_CONTEXT_QUERY_STRING);
            verify(nativeQuery).setParameter(
                    "currentTask",
                    "WithoutHttpRequest.registerWithoutHttpServletRequestUsesCallStackForTask");
        }

        @Test
        void registerWithoutHttpServletRequestUsesEmptyStringForRequest() {
            given(em.createNativeQuery(any())).willReturn(nativeQuery);

            context.define("current-subject");

            verify(em).createNativeQuery(DEFINE_CONTEXT_QUERY_STRING);
            verify(nativeQuery).setParameter("currentRequest", null);
        }

        @Test
        void registerWithSubjectUuidResolvesSubjectName() {
            val subjectUuid = UUID.randomUUID();
            lenient().when(nativeQuery.setParameter("uuid", subjectUuid)).thenReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn("resolved-subject@example.org");

            context.define(subjectUuid.toString());

            verify(nativeQuery).setParameter("currentSubject", "resolved-subject@example.org");
        }

        @Test
        void fetchCurrentTaskReturnsCurrentTask() {
            given(em.createNativeQuery("select current_setting('hsadminng.currentTask');")).willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn("given task");

            val result = context.fetchCurrentTask();

            assertThat(result).isEqualTo("given task");
        }

        @Test
        void fetchCurrentSubjectReturnsCurrentSubject() {
            given(em.createNativeQuery("select base.currentSubject()")).willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn("given-subject@example.org");

            val result = context.fetchCurrentSubject();

            assertThat(result).isEqualTo("given-subject@example.org");
        }

        @Test
        void fetchCurrentSubjectUuidReturnsCurrentSubjectUuid() {
            val subjectUuid = UUID.randomUUID();
            given(em.createNativeQuery("select rbac.currentSubjectUuid()", UUID.class)).willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn(subjectUuid);

            val result = context.fetchCurrentSubjectUuid();

            assertThat(result).isEqualTo(subjectUuid);
        }

        @Test
        void fetchAssumedRolesNamesReturnsAssumedRoles() {
            val assumedRoles = new String[] { "rbac.global#global:ADMIN" };
            given(em.createNativeQuery("select base.assumedRoles() as roles", String[].class)).willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn(assumedRoles);

            val result = context.fetchAssumedRolesNames();

            assertThat(result).containsExactly("rbac.global#global:ADMIN");
        }

        @Test
        void fetchCurrentSubjectOrAssumedRolesUuidsReturnsUuids() {
            val uuids = new UUID[] { UUID.randomUUID(), UUID.randomUUID() };
            given(em.createNativeQuery("select rbac.currentSubjectOrAssumedRolesUuids() as uuids", UUID[].class))
                    .willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn(uuids);

            val result = context.fetchCurrentSubjectOrAssumedRolesUuids();

            assertThat(result).containsExactly(uuids);
        }

        @Test
        void isGlobalAdminReturnsCurrentFlag() {
            given(em.createNativeQuery("select rbac.isGlobalAdmin()", boolean.class)).willReturn(nativeQuery);
            given(nativeQuery.getSingleResult()).willReturn(true);

            val result = context.isGlobalAdmin();

            assertThat(result).isTrue();
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
            lenient().when(em.createNativeQuery(any())).thenReturn(nativeQuery);
        }

        @Test
        void registerWithHttpServletRequestUsesRequest() throws IOException {
            givenRequest("POST", "http://localhost:9999/api/endpoint", Map.ofEntries(
                            Map.entry("current-subject", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-subject");

            verify(em).createNativeQuery(DEFINE_CONTEXT_QUERY_STRING);
            verify(nativeQuery).setParameter("currentTask", "POST http://localhost:9999/api/endpoint");
        }

        @Test
        void registerWithHttpServletRequestForwardsRequestAsCurl() throws IOException {
            givenRequest("POST", "http://localhost:9999/api/endpoint", Map.ofEntries(
                            Map.entry("current-subject", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-subject");

            verify(em).createNativeQuery(DEFINE_CONTEXT_QUERY_STRING);
            verify(nativeQuery).setParameter("currentRequest", """
                    curl -0 -v -X POST http://localhost:9999/api/endpoint \\
                    -H 'content-type:application/json' \\
                    -H 'current-subject:given-user' \\
                    --data-binary @- << EOF

                    {}
                    EOF
                    """.trim());
        }

        @Test
        void shortensCurrentTaskToMaxLength() throws IOException {
            givenRequest("GET", "http://localhost:9999/api/endpoint/" + "0123456789".repeat(13),
                    Map.ofEntries(
                            Map.entry("current-subject", "given-user"),
                            Map.entry("content-type", "application/json"),
                            Map.entry("user-agent", "given-user-agent")),
                    "{}");

            context.define("current-subject");

            verify(em).createNativeQuery(DEFINE_CONTEXT_QUERY_STRING);
            verify(nativeQuery).setParameter(eq("currentTask"), argThat((String t) -> t.length() == 127));
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
