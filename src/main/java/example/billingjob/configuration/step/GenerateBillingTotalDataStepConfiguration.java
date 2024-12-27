package example.billingjob.configuration.step;

import example.billingjob.configuration.BillingJobConfiguration;
import example.exception.PricingException;
import example.model.BillingData;
import example.model.ReportingData;
import example.processor.BillingDataProcessor;
import example.billingjob.service.PricingService;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Configuration Class for {@link #generateBillingTotalDataStep } Step. ({@value GenerateBillingTotalDataStepConfiguration#STEP_NAME})
 * <br>
 * <br>
 *
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
 *             <tr>
 *                 <td> Source </td>
 *                 <td> {@link #billingDataTableReader billingDataTableReader} </td>
 *                 <td> JdbcCursorItemReader </td>
 *                 <td> {@value GenerateBillingTotalDataStepConfiguration#READER_NAME} </td>
 *             </tr>
 *             <tr>
 *                 <td> Transform </td>
 *                 <td> {@link #billingDataProcessor billingDataProcessor }</td>
 *                 <td> {@link example.processor.BillingDataProcessor BillingDataProcessor} </td>
 *             </tr>
 *             <tr>
 *                 <td> Sink </td>
 *                 <td> {@link #billingDataFileWriter billingDataFileWriter}</td>
 *                 <td> FlatFileItemWriter </td>
 *                 <td> {@value GenerateBillingTotalDataStepConfiguration#WRITER_NAME} </td>
 *             </tr>
 *         </tbody>
 *     </table>
 */
@Configuration
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class GenerateBillingTotalDataStepConfiguration {
    private static final String STEP_NAME = "generate-billing-total-data";
    public static final String READER_NAME = "billing-data-table-reader";
    public static final String WRITER_NAME = "billing-data-file-writer";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Step 3 ============================================================================================
    @Bean
    @StepScope
    public JdbcCursorItemReader<BillingData> billingDataTableReader(
            DataSource dataSource,
            @Value("#{jobParameters['data.year']}") Integer year,
            @Value("#{jobParameters['data.month']}") Integer month) {

        String sql = String.format("select * from " + BillingJobConfiguration.BILLING_DATA_TABLE +
                " where DATA_YEAR = %d and DATA_MONTH = %d", year, month);

        log.debug("reader: {}, sql: {}", READER_NAME, sql);
        return new JdbcCursorItemReaderBuilder<BillingData>()
                .name(READER_NAME)
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new DataClassRowMapper<>(BillingData.class))
                .build();
    }

    @Bean
    public BillingDataProcessor billingDataProcessor(
            PricingService pricingService,
            @Value("${cellular.plan.spending-threshold:150.0f}") Float spendingThreshold) {

        return new BillingDataProcessor(pricingService, spendingThreshold);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<ReportingData> billingDataFileWriter(
            @Value("#{jobParameters['output.file']}") String outputFile) {
        log.debug("writer: {},  outputFile: {}", WRITER_NAME, outputFile);

        String[] fields = getOrderedFields();

        return new FlatFileItemWriterBuilder<ReportingData>()
                .resource(new FileSystemResource(outputFile))
                .name(WRITER_NAME)
                .delimited()
                .names(fields)
                .build();
    }

    private String[] getOrderedFields() {
        List<String> orderedFields = new ArrayList<>(Stream.of(BillingData.Fields.DATA_YEAR, BillingData.Fields.DATA_MONTH, BillingData.Fields.ACCOUNT_ID,
                BillingData.Fields.PHONE_NUMBER, BillingData.Fields.DATA_USAGE, BillingData.Fields.CALL_DURATION, BillingData.Fields.SMS_COUNT)
                .map(field -> String.format("%s.%s", ReportingData.Fields.BILLING_DATA , field))
                .toList());

        orderedFields.add(ReportingData.Fields.BILLING_TOTAL);
        log.trace("fields: {}", orderedFields.stream().reduce((a, b) -> a + "," + b).orElse(""));

        return orderedFields.toArray(String[]::new);
    }


    /**
     * Creates a CSV file that contains all the billing totals.
     *
     * @param fromRdbms {@link #billingDataTableReader JdbcCursorItemReader}
     * @param calculateTotal {@link #billingDataProcessor ItemProcessor}
     * @param toFlatFile {@link #billingDataFileWriter FlatFileItemWriter}
     */
    @Bean
    public Step generateBillingTotalDataStep(
            @Qualifier("billingDataTableReader") JdbcCursorItemReader<BillingData> fromRdbms,
            @Qualifier("billingDataProcessor") ItemProcessor<BillingData, ReportingData> calculateTotal,
            @Qualifier("billingDataFileWriter") FlatFileItemWriter<ReportingData> toFlatFile) {


        return new StepBuilder(STEP_NAME, jobRepository)
                .<BillingData, ReportingData>chunk(100, transactionManager)
                .reader(fromRdbms)
                .processor(calculateTotal)
                .writer(toFlatFile)
                .faultTolerant()
                .retry(PricingException.class)
                .retryLimit(100)
                .build();
    }

}
