package example.billingjob.configuration;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.test.StepRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@TestConfiguration
@EnableBatchProcessing
public class BatchConfig {
    public static final String CREATE_BILLING_TABLE = """
            	CREATE TABLE IF NOT EXISTS billing_data (
            		data_year INTEGER,
            		data_month INTEGER,
            		account_id INTEGER,
            		phone_number VARCHAR(12),
            		data_usage DOUBLE PRECISION,
            		call_duration INTEGER,
            		sms_count INTEGER
            	);
            """;

    public static final String DROP_BILLING_TABLE = """
                DROP TABLE billing_data IF EXISTS;
            """;

    @Bean
    public StepRunner stepRunner(
            JobLauncher jobLauncher,
            JobRepository jobRepository) {
        return new StepRunner(jobLauncher, jobRepository);
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new ResourcelessTransactionManager();
    }


    @Bean
    public JobRepository jobRepository() {
        return new ResourcelessJobRepository();
    }
}