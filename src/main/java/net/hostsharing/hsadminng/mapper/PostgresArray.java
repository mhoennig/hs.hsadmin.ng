package net.hostsharing.hsadminng.mapper;

import com.vladmihalcea.hibernate.type.range.Range;
import lombok.experimental.UtilityClass;
import org.postgresql.util.PGtokenizer;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.function.Function;

@UtilityClass
public class PostgresArray {

    /**
     * Converts a byte[], as returned for a Postgres-array by native queries, to a Java array.
     *
     * <p>This example code worked with Hibernate 5 (Spring Boot 3.0.x):
     * <pre><code>
     *      return (UUID[]) em.createNativeQuery("select currentSubjectsUuids() as uuids", UUID[].class).getSingleResult();
     * </code></pre>
     * </p>
     *
     * <p>With Hibernate 6 (Spring Boot 3.1.x), this utility method can be used like such:
     * <pre><code>
     *      final byte[] result = (byte[]) em.createNativeQuery("select * from currentSubjectsUuids() as uuids", UUID[].class)
     *                 .getSingleResult();
     *      return fromPostgresArray(result, UUID.class, UUID::fromString);
     * </code></pre>
     * </p>
     *
     * @param pgArray the byte[] returned by a native query containing as rendered for a Postgres array
     * @param elementClass the class of a single element of the Java array to be returned
     * @param itemParser converts a string element to the specified elementClass
     * @return a Java array containing the data from pgArray
     * @param <T> type of a single element of the Java array
     */
    public static <T> T[] fromPostgresArray(final byte[] pgArray, final Class<T> elementClass, final Function<String, T> itemParser) {
        final var pgArrayLiteral = new String(pgArray, StandardCharsets.UTF_8);
        if (pgArrayLiteral.length() == 2) {
            return newGenericArray(elementClass, 0);
        }
        final PGtokenizer tokenizer = new PGtokenizer(pgArrayLiteral.substring(1, pgArrayLiteral.length()-1), ',');
        tokenizer.remove("\"", "\"");
        final T[] array = newGenericArray(elementClass, tokenizer.getSize()); // Create a new array of the specified type and length
        for ( int n = 0; n < tokenizer.getSize(); ++n ) {
            array[n] = itemParser.apply(tokenizer.getToken(n).trim().replace("\\\"", "\""));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] newGenericArray(final Class<T> elementClass, final int length) {
        return (T[]) Array.newInstance(elementClass, length);
    }

}
