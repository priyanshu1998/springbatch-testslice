package example.billingjob.configuration.step;

import example.billingjob.configuration.BillingJobConfiguration;
import example.billingjob.configuration.listener.IngestBillingDataStepListenerConfigurations;
import example.blueprint.infrastructure.data.BillingData;

import example.blueprint.infrastructure.mapper.BillingDataPreparedStatementMapper;
import example.billingjob.configuration.BillingJobBeanDirectory.IngestBillingDataStep;

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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Configuration class for {@link #create ingestBillingDataStep} Step. ({@value IngestBillingDataStepConfiguration#STEP_NAME})
 * <br><br>
 * <table >
 *         <thead>
 *             <tr>
 *                 <th> Operator </th>
 *                 <th> Bean Name </th>
 *                 <th> Bean Definition </th>
 *                 <th> DB Reference </th>
 *             </tr>
 *         </thead>
 *         <tbody>
 *            <tr>
 *                <td> Source </td>
 *                <td> {@value IngestBillingDataStep#READER}</td>
 *                <td> {@link #reader FlatFileItemReader} </td>
 *                <td> {@value IngestBillingDataStepConfiguration#READER_NAME }</td>
 *            </tr>
 *            <tr>
 *                <td> Sink </td>
 *                <td> {@value IngestBillingDataStep#WRITER}</td>
 *                <td> {@link #writer JdbcBatchItemWriter} </td>
 *                <td>  </td>
 *            </tr>
 *            <tr>
 *                <td> Listener </td>
 *                <td> {@value IngestBillingDataStep#SKIP_LISTENER} </td>
 *                <td> {@link IngestBillingDataStepListenerConfigurations#skipListener SkipListener}</td>
 *                <td>  </td>
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

    @Bean(IngestBillingDataStep.READER)
    @StepScope
    public FlatFileItemReader<BillingData> reader(
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

    @Bean(IngestBillingDataStep.WRITER)
    public JdbcBatchItemWriter<BillingData> writer(DataSource dataSource) {
        String sql = "insert into " + BillingJobConfiguration.BILLING_DATA_TABLE +
                " (data_year, data_month, account_id, phone_number, data_usage, call_duration, sms_count)" +
                " values (?, ?, ?, ?, ?, ?, ?)";
        var billingDataPsMapper = new BillingDataPreparedStatementMapper();
        log.debug("bean name: billingDataTableWriter, sql: {}", sql);
        return new JdbcBatchItemWriterBuilder<BillingData>()
                .dataSource(dataSource)
                .namedParametersJdbcTemplate(new NamedParameterJdbcTemplate(dataSource))
                .sql(sql)
                .itemPreparedStatementSetter(billingDataPsMapper)
                .build();
    }

    /** Stores data in the {@link BillingJobConfiguration#BILLING_DATA_TABLE BILLING_DATA_TABLE} table
     * @param fromInputFile {@link #reader FlatFileItemReader}
     * @param toBillDataTable {@link #writer JdbcBatchItemWriter}
     * @param skipListener {@link IngestBillingDataStepListenerConfigurations#skipListener SkipListener}
     */
    @Bean(IngestBillingDataStep.STEP)
    public Step create(
            @Qualifier(IngestBillingDataStep.READER)                    FlatFileItemReader<BillingData> fromInputFile,
            @Qualifier(IngestBillingDataStep.WRITER)                    JdbcBatchItemWriter<BillingData> toBillDataTable,
            @Qualifier(IngestBillingDataStep.SKIP_LISTENER)    SkipListener<BillingData, BillingData> skipListener) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<BillingData, BillingData>chunk(100, transactionManager)
                .reader(fromInputFile)
                .writer(toBillDataTable)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(10)
                .listener(skipListener)
                .build();
    }
    // ===================================================================================================
}
