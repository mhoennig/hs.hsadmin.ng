package net.hostsharing.hsadminng.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContextUnitTest {

    @Mock
    EntityManager em;

    @Mock
    Query nativeQuery;

    @InjectMocks
    Context context;

    @Test
    void registerWithoutHttpServletRequestUsesCallStack() {
        given(em.createNativeQuery(any())).willReturn(nativeQuery);

        context.register("current-user", null);

        verify(em).createNativeQuery(
                "set local hsadminng.currentTask = 'ContextUnitTest.registerWithoutHttpServletRequestUsesCallStack';");
    }
}
