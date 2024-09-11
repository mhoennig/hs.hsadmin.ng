package net.hostsharing.hsadminng.persistence;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.Map;

/** A Spring bean wrapper for the EntityManager.
 *
 * <p>@PersistenceContext cannot be properly mocked in @WebMvcTest-based tests
 * because Spring will always create a proxy for the mock which then fails because it has no active transaction.</p>
 *
 * <p>Also, @PersistenceContext cannot be used for constructor injection, though a bean factory would solve that problem.</p>
 *
 * <p>Use this wrapper **only** if needed for a @WebMvcTest with a mocked EntityManager, otherwise use the original EntityManager.</p>
  */
@Component
@NoArgsConstructor
@AllArgsConstructor
public class EntityManagerWrapper implements EntityManager {

    @PersistenceContext
    EntityManager em;

    @Override
    public void persist(final Object entity) {
        em.persist(entity);
    }

    @Override
    public <T> T merge(final T entity) {
        return em.merge(entity);
    }

    @Override
    public void remove(final Object entity) {
        em.remove(entity);
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey) {
        return em.find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey, final Map<String, Object> properties) {
        return em.find(entityClass, primaryKey, properties);
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockMode) {
        return em.find(entityClass, primaryKey, lockMode);
    }

    @Override
    public <T> T find(
            final Class<T> entityClass,
            final Object primaryKey,
            final LockModeType lockMode,
            final Map<String, Object> properties) {
        return em.find(entityClass, primaryKey, lockMode, properties);
    }

    @Override
    public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
        return em.getReference(entityClass, primaryKey);
    }

    @Override
    public void flush() {
        em.flush();
    }

    @Override
    public void setFlushMode(final FlushModeType flushMode) {
        em.setFlushMode(flushMode);
    }

    @Override
    public FlushModeType getFlushMode() {
        return em.getFlushMode();
    }

    @Override
    public void lock(final Object entity, final LockModeType lockMode) {
        em.lock(entity, lockMode);
    }

    @Override
    public void lock(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        em.lock(entity, lockMode, properties);
    }

    @Override
    public void refresh(final Object entity) {
        em.refresh(entity);
    }

    @Override
    public void refresh(final Object entity, final Map<String, Object> properties) {
        em.refresh(entity, properties);
    }

    @Override
    public void refresh(final Object entity, final LockModeType lockMode) {
        em.refresh(entity, lockMode);
    }

    @Override
    public void refresh(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        em.refresh(entity, lockMode, properties);
    }

    @Override
    public void clear() {
        em.clear();
    }

    @Override
    public void detach(final Object entity) {
        em.detach(entity);
    }

    @Override
    public boolean contains(final Object entity) {
        return em.contains(entity);
    }

    @Override
    public LockModeType getLockMode(final Object entity) {
        return em.getLockMode(entity);
    }

    @Override
    public void setProperty(final String propertyName, final Object value) {
        em.setProperty(propertyName, value);
    }

    @Override
    public Map<String, Object> getProperties() {
        return em.getProperties();
    }

    @Override
    public Query createQuery(final String qlString) {
        return em.createQuery(qlString);
    }

    @Override
    public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> criteriaQuery) {
        return em.createQuery(criteriaQuery);
    }

    @Override
    public Query createQuery(final CriteriaUpdate updateQuery) {
        return em.createQuery(updateQuery);
    }

    @Override
    public Query createQuery(final CriteriaDelete deleteQuery) {
        return em.createQuery(deleteQuery);
    }

    @Override
    public <T> TypedQuery<T> createQuery(final String qlString, final Class<T> resultClass) {
        return em.createQuery(qlString, resultClass);
    }

    @Override
    public Query createNamedQuery(final String name) {
        return em.createNamedQuery(name);
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(final String name, final Class<T> resultClass) {
        return em.createNamedQuery(name, resultClass);
    }

    @Override
    public Query createNativeQuery(final String sqlString) {
        return em.createNativeQuery(sqlString);
    }

    @Override
    public Query createNativeQuery(final String sqlString, final Class resultClass) {
        return em.createNativeQuery(sqlString, resultClass);
    }

    @Override
    public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
        return em.createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(final String name) {
        return em.createNamedStoredProcedureQuery(name);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName) {
        return em.createStoredProcedureQuery(procedureName);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final Class... resultClasses) {
        return em.createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final String... resultSetMappings) {
        return em.createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public void joinTransaction() {
        em.joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return em.isJoinedToTransaction();
    }

    @Override
    public <T> T unwrap(final Class<T> cls) {
        return em.unwrap(cls);
    }

    @Override
    public Object getDelegate() {
        return em.getDelegate();
    }

    @Override
    public void close() {
        em.close();
    }

    @Override
    public boolean isOpen() {
        return em.isOpen();
    }

    @Override
    public EntityTransaction getTransaction() {
        return em.getTransaction();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return em.getEntityManagerFactory();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return em.getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return em.getMetamodel();
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(final Class<T> rootType) {
        return em.createEntityGraph(rootType);
    }

    @Override
    public EntityGraph<?> createEntityGraph(final String graphName) {
        return em.createEntityGraph(graphName);
    }

    @Override
    public EntityGraph<?> getEntityGraph(final String graphName) {
        return em.getEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(final Class<T> entityClass) {
        return em.getEntityGraphs(entityClass);
    }
}
