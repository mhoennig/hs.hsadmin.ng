package net.hostsharing.hsadminng.mapper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Tag("generalIntegrationTest")
class PostgresArrayIntegrationTest {

    @Autowired
    EntityManager em;

    @Test
    void shouldCreateEmptyArray() {
        em.createNativeQuery("""            
            create or replace function returnEmptyArray()
                returns text[]
                stable -- leakproof
                language plpgsql as $$
            declare
                emptyArray text[] = '{}';
            begin
                return emptyArray;
            end; $$;
            """).executeUpdate();
        final String[] result = (String[]) em.createNativeQuery("SELECT returnEmptyArray()", String[].class).getSingleResult();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreateStringArray() {
        em.createNativeQuery("""            
            create or replace function returnStringArray()
                returns varchar(63)[]
                stable -- leakproof
                language plpgsql as $$
            declare
                text1 text = 'one';
                text2 text = 'two, three';
                text3 text = 'four; five';
                text4 text = 'say "Hello" to me';
            begin
                return array[text1, text2, text3, null, text4];
            end; $$;
            """).executeUpdate();
        final String[] result = (String[]) em.createNativeQuery("SELECT returnStringArray()", String[].class).getSingleResult();

        assertThat(result).containsExactly("one", "two, three", "four; five", null, "say \"Hello\" to me");
    }

    @Test
    void shouldCreateUUidArray() {
        em.createNativeQuery("""            
            create or replace function returnUuidArray()
                returns uuid[]
                stable -- leakproof
                language plpgsql as $$
            declare
                uuid1 UUID = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
                uuid2 UUID = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';
                uuid3 UUID = '01234567-89ab-cdef-0123-456789abcdef';
            begin
                return ARRAY[uuid1, uuid2, null, uuid3];
            end; $$;
            """).executeUpdate();
        final UUID[] result = (UUID[]) em.createNativeQuery("SELECT returnUuidArray()", UUID[].class).getSingleResult();

        assertThat(result).containsExactly(
                UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
                UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
                null,
                UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"));
    }
}
