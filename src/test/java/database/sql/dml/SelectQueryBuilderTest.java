package database.sql.dml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SelectQueryBuilderTest {
    private final SelectQueryBuilder selectQueryBuilder = new SelectQueryBuilder(Person4.class);

    @Test
    void buildSelectQuery() {
        String actual = selectQueryBuilder.buildQuery();
        assertThat(actual).isEqualTo("SELECT id, nick_name, old, email FROM users");
    }
}
