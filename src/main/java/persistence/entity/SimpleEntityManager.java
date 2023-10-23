package persistence.entity;

import jdbc.JdbcTemplate;
import jdbc.RowMapper;
import persistence.core.EntityMetadata;
import persistence.core.EntityMetadataProvider;
import persistence.core.PersistenceEnvironment;
import persistence.exception.PersistenceException;
import persistence.util.ReflectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SimpleEntityManager implements EntityManager {

    private final JdbcTemplate jdbcTemplate;
    private final Connection connection;
    private boolean closed;
    private final EntityPersisterProvider entityPersisterProvider;

    public SimpleEntityManager(final PersistenceEnvironment persistenceEnvironment) {
        this.connection = persistenceEnvironment.getConnection();
        this.jdbcTemplate = new JdbcTemplate(connection);
        this.closed = false;
        this.entityPersisterProvider = new EntityPersisterProvider(jdbcTemplate, persistenceEnvironment.getDmlGenerator());
    }

    @Override
    public <T> T find(final Class<T> clazz, final Long id) {
        checkConnectionOpen();
        final EntityPersister entityPersister = entityPersisterProvider.getEntityPersister(clazz);
        final String query = entityPersister.renderSelect(id);
        return jdbcTemplate.queryForObject(query, getObjectRowMapper(clazz));
    }

    private <T> RowMapper<T> getObjectRowMapper(final Class<T> clazz) {
        final EntityMetadata<?> entityMetadata = EntityMetadataProvider.getInstance().getEntityMetadata(clazz);
        final List<String> columnNames = entityMetadata.getColumnNames();
        final List<String> fieldNames = entityMetadata.getColumnFieldNames();

        return rowMapper -> {
            final T instance = ReflectionUtils.createInstance(clazz);
            for (int i = 0; i < entityMetadata.getColumnSize(); i++) {
                final String fieldName = fieldNames.get(i);
                final String columnName = columnNames.get(i);
                final Object object = rowMapper.getObject(columnName);
                ReflectionUtils.injectField(instance, fieldName, object);
            }
            return instance;
        };
    }

    @Override
    public void persist(final Object entity) {
        checkConnectionOpen();
        final EntityPersister entityPersister = entityPersisterProvider.getEntityPersister(entity.getClass());
        entityPersister.insert(entity);
    }

    @Override
    public void remove(final Object entity) {
        checkConnectionOpen();
        final EntityPersister entityPersister = entityPersisterProvider.getEntityPersister(entity.getClass());
        entityPersister.delete(entity);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (final SQLException e) {
            throw new PersistenceException("커넥션 닫기를 실패했습니다.", e);
        } finally {
            this.closed = true;
        }
    }

    private void checkConnectionOpen() {
        if (this.closed) {
            throw new PersistenceException("DB와의 커넥션이 끊어졌습니다.");
        }
    }
}
