package example.billingjob.configuration;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * IMPORTANT:
 * There cannot be any @Bean definition method in this class (as proxyBeanMethods = false)
 * TODO: Enforce this via archunit test.
 */
@Configuration(proxyBeanMethods = false)
@lombok.RequiredArgsConstructor
@lombok.Getter
@lombok.experimental.Accessors(fluent = true)
public class SharedConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
}
