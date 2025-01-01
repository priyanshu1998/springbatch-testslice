package example.billingjob.configuration;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@lombok.RequiredArgsConstructor
@lombok.Getter
@lombok.experimental.Accessors(fluent = true)
public class SharedConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
}
