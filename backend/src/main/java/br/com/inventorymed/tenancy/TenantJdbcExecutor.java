package br.com.inventorymed.tenancy;

import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TenantJdbcExecutor {

    private final TenantDataSourceRegistry dataSourceRegistry;

    public TenantJdbcExecutor(TenantDataSourceRegistry dataSourceRegistry) {
        this.dataSourceRegistry = dataSourceRegistry;
    }

    public <T> T read(UUID hospitalId, Function<JdbcTemplate, T> operation) {
        return operation.apply(new JdbcTemplate(dataSourceRegistry.requiredDataSource(hospitalId)));
    }

    public <T> T write(UUID hospitalId, Function<JdbcTemplate, T> operation) {
        DataSource dataSource = dataSourceRegistry.requiredDataSource(hospitalId);
        TransactionTemplate transaction = new TransactionTemplate(
            new DataSourceTransactionManager(dataSource)
        );
        return transaction.execute(status -> operation.apply(new JdbcTemplate(dataSource)));
    }
}
