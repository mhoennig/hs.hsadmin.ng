package net.hostsharing.hsadminng.rbac.subject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RbacSubjectListServiceUnitTest {

    @Mock
    private RbacSubjectRepository rbacSubjectRepository;

    @Mock
    private RealSubjectRepository realSubjectRepository;

    @InjectMocks
    private RbacSubjectListService service;

    @Test
    void returnsRbacVisibleSubjectsPlusCurrentSubjectGroupsSortedByName() {

        // given
        final var rbacVisibleUser = rbacSubject("visible-user@example.org", SubjectType.USER);
        final var rbacVisibleGroup = rbacSubject("/xyz-GroupA", SubjectType.GROUP);
        final var currentSubjectGroup = realSubject("/xyz-GroupB", SubjectType.GROUP);
        given(rbacSubjectRepository.findByOptionalNameLikeAndOptionalType(null, null))
                .willReturn(List.of(rbacVisibleUser, rbacVisibleGroup));
        given(realSubjectRepository.findCurrentSubjectGroupSubjectsByOptionalNameLike(null))
                .willReturn(List.of(currentSubjectGroup));

        // when
        final var result = service.findByOptionalNameLikeAndOptionalType(null, null);

        // then
        assertThat(result)
                .extracting(Subject::getName)
                .containsExactly("/xyz-GroupA", "/xyz-GroupB", "visible-user@example.org");
    }

    @Test
    void returnsOnlyRbacVisibleSubjectsForUserOnlyRequests() {

        // given
        final var groupUuid = UUID.randomUUID();
        final var rbacVisibleGroup = rbacSubject(groupUuid, "/xyz-Group", SubjectType.GROUP);
        final var currentSubjectGroup = realSubject(groupUuid, "/xyz-Group", SubjectType.GROUP);
        given(rbacSubjectRepository.findByOptionalNameLikeAndOptionalType("/xyz-Group", SubjectType.GROUP))
                .willReturn(List.of(rbacVisibleGroup));
        given(realSubjectRepository.findCurrentSubjectGroupSubjectsByOptionalNameLike("/xyz-Group"))
                .willReturn(List.of(currentSubjectGroup));

        // when
        final var result = service.findByOptionalNameLikeAndOptionalType("/xyz-Group", SubjectType.GROUP);

        // then
        assertThat(result).containsExactly(rbacVisibleGroup);
    }

    @Test
    void skipsCurrentSubjectGroupLookupForUserOnlyRequests() {

        // given
        final var rbacVisibleUser = rbacSubject("visible-user@example.org", SubjectType.USER);
        given(rbacSubjectRepository.findByOptionalNameLikeAndOptionalType("visible-user", SubjectType.USER))
                .willReturn(List.of(rbacVisibleUser));

        // when
        final var result = service.findByOptionalNameLikeAndOptionalType("visible-user", SubjectType.USER);

        // then
        assertThat(result).containsExactly(rbacVisibleUser);
        verify(rbacSubjectRepository).findByOptionalNameLikeAndOptionalType("visible-user", SubjectType.USER);
        verifyNoInteractions(realSubjectRepository);
    }

    private static RbacSubjectEntity rbacSubject(final String name, final SubjectType type) {
        return rbacSubject(UUID.randomUUID(), name, type);
    }

    private static RbacSubjectEntity rbacSubject(final UUID uuid, final String name, final SubjectType type) {
        return RbacSubjectEntity.builder()
                .uuid(uuid)
                .name(name)
                .type(type)
                .build();
    }

    private static RealSubjectEntity realSubject(final String name, final SubjectType type) {
        return realSubject(UUID.randomUUID(), name, type);
    }

    private static RealSubjectEntity realSubject(final UUID uuid, final String name, final SubjectType type) {
        return RealSubjectEntity.builder()
                .uuid(uuid)
                .name(name)
                .type(type)
                .build();
    }
}
