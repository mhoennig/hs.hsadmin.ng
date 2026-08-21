package net.hostsharing.hsadminng.hs.hosting.asset;

import org.jetbrains.annotations.NotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class EntityManagerMock {
    public static @NotNull EntityManager createEntityManagerMockWithAssetQueryFake(final HsHostingAssetRealEntity asset) {
        final var em = mock(EntityManager.class);
        final var assetQuery = mock(TypedQuery.class);

        lenient().when(em.createQuery(any(), any(Class.class))).thenReturn(assetQuery);
        lenient().when(assetQuery.setParameter(anyString(), any())).thenReturn(assetQuery);
        lenient().when(assetQuery.setMaxResults(anyInt())).thenReturn(assetQuery);
        lenient().when(assetQuery.getResultList()).thenReturn(asset == null ? List.of() : List.of(asset));
        return em;
    }
}
