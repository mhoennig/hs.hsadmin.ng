package net.hostsharing.hsadminng.rbac.subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RbacSubjectListService {

    @Autowired
    private RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    private RealSubjectRepository realSubjectRepository;

    // TODO.impl: this implementation feels ugly, maybe there is a better way?
    public List<Subject<?>> findByOptionalNameLikeAndOptionalType(final String userName, final SubjectType subjectType) {
        final var subjectsByUuid = new LinkedHashMap<UUID, Subject<?>>();

        addRbacVisibleSubjects(subjectsByUuid, userName, subjectType);
        if (includesGroups(subjectType)) {
            addCurrentSubjectGroupSubjects(subjectsByUuid, userName);
        }

        return subjectsByUuid.values().stream()
                .sorted(Comparator.comparing(Subject::getName))
                .toList();
    }

    private void addRbacVisibleSubjects(
            final Map<UUID, Subject<?>> subjectsByUuid,
            final String userName,
            final SubjectType subjectType) {

        rbacSubjectRepository.findByOptionalNameLikeAndOptionalType(userName, subjectType)
                .forEach(subject -> subjectsByUuid.put(subject.getUuid(), subject));
    }

    private void addCurrentSubjectGroupSubjects(
            final Map<UUID, Subject<?>> subjectsByUuid,
            final String userName) {

        realSubjectRepository.findCurrentSubjectGroupSubjectsByOptionalNameLike(userName)
                .forEach(subject -> subjectsByUuid.putIfAbsent(subject.getUuid(), subject));
    }

    private static boolean includesGroups(final SubjectType subjectType) {
        return subjectType == null || subjectType == SubjectType.GROUP;
    }
}
