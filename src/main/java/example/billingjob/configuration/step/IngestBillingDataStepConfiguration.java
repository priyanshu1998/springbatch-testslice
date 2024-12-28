package example.billingjob.configuration.step;

import example.billingjob.configuration.BillingJobConfiguration;
import example.blueprint.infrastructure.data.BillingData;

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
import java.util.Arrays;

/**
 * Configuration class for {@link #ingestBillingDataStep} Step. ({@value IngestBillingDataStepConfiguration#STEP_NAME})
 * <br><br>
 * <table >
 *         <thead>
 *             <tr>
 *                 <th> Operator </th>
 *                 <th> Bean Name </th>
 *                 <th> Bean Type </th>
 *                 <th> DB Reference </th>
 *             </tr>
 *         </thead>
 *         <tbody>
 *            <tr>
 *                <td> Source </td>
 *                <td> {@link #billingDataFileReader billingDataFileReader} </td>
 *                <td> FlatFileItemReader </td>
 *                <td> {@value IngestBillingDataStepConfiguration#READER_NAME }</td>
 *            </tr>
 *            <tr>
 *                <td> Sink </td>
 *                <td> {@link #billingDataTableWriter billingDataTableWriter} </td>
 *                <td> JdbcBatchItemWriter </td>
 *            </tr>
 *            <tr>
 *                <td> Listener </td>
 *                <td> {@link BillingJobConfiguration#parseFailListener parseFailListener}</td>
 *                <td> SkipListener </td>
 *            </tr>
 *         </tbody>
 * </table>
 */
@Configuration
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class IngestBillingDataStepConfiguration {
    public static final String READER_NAME = "billing-data-file-reader";
    public static final String STEP_NAME = "ingest-billing-data";
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    @StepScope
    public FlatFileItemReader<BillingData> billingDataFileReader(
            @Value("#{jobParameters['input.file']}") String inputFile) {

        String[] orderedColNames = {
                BillingData.Fields.DATA_YEAR,
                BillingData.Fields.DATA_MONTH,
                BillingData.Fields.ACCOUNT_ID,
                BillingData.Fields.PHONE_NUMBER ,
                BillingData.Fields.DATA_USAGE,
                BillingData.Fields.CALL_DURATION,
                BillingData.Fields.SMS_COUNT};

        log.trace("reader: {}, columns: {}", READER_NAME,Arrays.stream(orderedColNames).reduce((a, b) -> a + "," + b).orElse(""));
        return new FlatFileItemReaderBuilder<BillingData>()
                .name(READER_NAME)
                .resource(new FileSystemResource(inputFile))
                .delimited()
                .names(orderedColNames)
                .targetType(BillingData.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<BillingData> billingDataTableWriter(
            DataSource dataSource) {
        String sql = "insert into " + BillingJobConfiguration.BILLING_DATA_TABLE +
                " values (:dataYear, :dataMonth, :accountId, :phoneNumber, :dataUsage, :callDuration, :smsCount)";

        log.debug("bean name: billingDataTableWriter, sql: {}", sql);
        return new JdbcBatchItemWriterBuilder<BillingData>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }

    /** Stores data in the {@link BillingJobConfiguration#BILLING_DATA_TABLE BILLING_DATA_TABLE} table
     * @param fromFlatFile {@link #billingDataFileReader FlatFileItemReader}
     * @param toRdbmsTable {@link #billingDataTableWriter JdbcBatchItemWriter}
     * @param skipListener {@link BillingJobConfiguration#parseFailListener SkipListener}
     */
    @Bean
    public Step ingestBillingDataStep(
            @Qualifier("billingDataFileReader") FlatFileItemReader<BillingData> fromFlatFile,
            @Qualifier("billingDataTableWriter") JdbcBatchItemWriter<BillingData> toRdbmsTable,
            @Qualifier("parseFailListener") SkipListener<BillingData, BillingData> skipListener) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<BillingData, BillingData>chunk(100, transactionManager)
                .reader(fromFlatFile)
                .writer(toRdbmsTable)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(10)
                .listener(skipListener)
                .build();
    }
    // ===================================================================================================
}
