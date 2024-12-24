package example.billingjob.configuration.step;

import example.model.BillingData;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@lombok.RequiredArgsConstructor
public class IngestBillingDataStepConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Step2 =========================================================================================================
    @Bean
    @StepScope
    public FlatFileItemReader<BillingData> billingDataFileReader(
            @Value("#{jobParameters['input.file']}") String inputFile) {
        String[] colNames = {"dataYear", "dataMonth", "accountId", "phoneNumber", "dataUsage", "callDuration", "smsCount"};
        return new FlatFileItemReaderBuilder<BillingData>()
                .name("billingDataFileReader")
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .names(colNames)
                .targetType(BillingData.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<BillingData> billingDataTableWriter(
            DataSource dataSource) {
        String sql = "insert into billing_data values (:dataYear, :dataMonth, :accountId, :phoneNumber, :dataUsage, :callDuration, :smsCount)";
        return new JdbcBatchItemWriterBuilder<BillingData>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }

    @Bean
    public Step ingestBillingDataStep(
            @Qualifier("billingDataFileReader") FlatFileItemReader<BillingData> fromFlatFile,
            @Qualifier("billingDataTableWriter") JdbcBatchItemWriter<BillingData> toRDBMSTable,
            @Qualifier("parseFailListener") SkipListener<BillingData, BillingData> skipListener) {
        return new StepBuilder("ingest-billing-data", jobRepository)
                .<BillingData, BillingData>chunk(100, transactionManager)
                .reader(fromFlatFile)
                .writer(toRDBMSTable)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(10)
                .listener(skipListener)
                .build();
    }
    // ===================================================================================================
}
