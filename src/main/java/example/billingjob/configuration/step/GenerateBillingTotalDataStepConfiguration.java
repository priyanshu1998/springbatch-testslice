package example.billingjob.configuration.step;

import example.billingjob.configuration.property.PricingProperties;
import example.blueprint.exception.PricingException;
import example.blueprint.infrastructure.mapper.ReportingDataFieldSetMapper;
import example.blueprint.infrastructure.data.BillingData;
import example.blueprint.infrastructure.data.ReportingData;
import example.blueprint.processor.BillingDataProcessor;
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
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Configuration Class for {@link GenerateBillingTotalDataStepConfiguration#step generateBillingTotalDataStep} Step. ({@value GenerateBillingTotalDataStepConfiguration#STEP_NAME})
 * <br>
 * <br>
 *
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
 *             <tr>
 *                 <td> Source </td>
 *                 <td> {@value GenerateBillingTotalDataStep#READER}  </td>
 *                 <td> {@link GenerateBillingTotalDataStepConfiguration#reader JdbcCursorItemReader} </td>
 *                 <td> {@value GenerateBillingTotalDataStepConfiguration#READER_NAME} </td>
 *             </tr>
 *             <tr>
 *                 <td> Transform & Filter </td>
 *                 <td> {@value GenerateBillingTotalDataStep#PROCESSOR}  </td>
 *                 <td> {@link GenerateBillingTotalDataStepConfiguration#processor BillingDataProcessor }</td>
 *             </tr>
 *             <tr>
 *                 <td> Sink </td>
 *                 <td> {@value GenerateBillingTotalDataStep#WRITER}  </td>
 *                 <td> {@link GenerateBillingTotalDataStepConfiguration#writer FlatFileItemWriter}</td>
 *                 <td> {@value GenerateBillingTotalDataStepConfiguration#WRITER_NAME} </td>
 *             </tr>
 *         </tbody>
 *     </table>
 */
@Configuration
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
@EnableConfigurationProperties(PricingProperties.class)
@PropertySource("classpath:cellular-plan.properties")
public class GenerateBillingTotalDataStepConfiguration {
    public static class GenerateBillingTotalDataStep {
        public static final String READER = "billingDataTableReader";
        public static final String PROCESSOR = "billingDataProcessor";
        public static final String WRITER = "billingDataFileWriter";

        public static final String STEP = "generateBillingTotalDataStep";
    }

    private static final String STEP_NAME = "generate-billing-total-data";
    public static final String READER_NAME = "billing-data-table-reader";
    public static final String WRITER_NAME = "billing-data-file-writer";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    // Step 3 ============================================================================================
    @Bean(GenerateBillingTotalDataStep.READER)
    @StepScope
    public JdbcCursorItemReader<BillingData> reader(
            @Value("#{jobParameters['data.year']}") Integer year,
            @Value("#{jobParameters['data.month']}") Integer month) {
        String sql = String.format("select * from billing_data where data_year = %d and data_month = %d",
                year, month);
        log.debug("Reader invoked sql: {}", sql);
        return new JdbcCursorItemReaderBuilder<BillingData>()
                .name(READER_NAME)
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new DataClassRowMapper<>(BillingData.class))
                .build();
    }

    @Bean(GenerateBillingTotalDataStep.PROCESSOR)
    public BillingDataProcessor processor(
            PricingService pricingService,
            @Value("${cellular.plan.spending-threshold:#{null}}") Float spendingThreshold) {

        return new BillingDataProcessor(pricingService, spendingThreshold);
    }

    @Bean(GenerateBillingTotalDataStep.WRITER)
    @StepScope
    public FlatFileItemWriter<ReportingData> writer(
            @Value("#{jobParameters['output.file']}") String outputFile) {
        log.debug("writer is writing to outputFile: {}", outputFile);

        String[] fields = getOrderedFields();
        FieldExtractor<ReportingData> reportingDataFieldSetMapper = new ReportingDataFieldSetMapper(fields);

        return new FlatFileItemWriterBuilder<ReportingData>()
                .resource(new FileSystemResource(outputFile))
                .name(WRITER_NAME)
                .delimited()
                .fieldExtractor(reportingDataFieldSetMapper)
                .build();
    }

    private String[] getOrderedFields() {
        List<String> orderedFields = new ArrayList<>(Stream.of(BillingData.Fields.DATA_YEAR, BillingData.Fields.DATA_MONTH, BillingData.Fields.ACCOUNT_ID,
                        BillingData.Fields.PHONE_NUMBER, BillingData.Fields.DATA_USAGE, BillingData.Fields.CALL_DURATION, BillingData.Fields.SMS_COUNT)
                .map(field -> String.format("%s.%s", ReportingData.Fields.BILLING_DATA, field))
                .toList());

        orderedFields.add(ReportingData.Fields.BILLING_TOTAL);
        log.trace("fields: {}", orderedFields.stream().reduce((a, b) -> a + "," + b).orElse(""));

        return orderedFields.toArray(String[]::new);
    }


    /**
     * Creates a CSV file that contains all the billing totals.
     *
     * @param fromBillingDataTable {@link GenerateBillingTotalDataStepConfiguration#reader JdbcCursorItemReader}
     * @param calculateTotal       {@link GenerateBillingTotalDataStepConfiguration#processor ItemProcessor}
     * @param toOutputFile         {@link GenerateBillingTotalDataStepConfiguration#writer FlatFileItemWriter}
     */
    @Bean(GenerateBillingTotalDataStep.STEP)
    public Step step(
            @Qualifier(GenerateBillingTotalDataStep.READER)     JdbcCursorItemReader<BillingData> fromBillingDataTable,
            @Qualifier(GenerateBillingTotalDataStep.PROCESSOR)  ItemProcessor<BillingData, ReportingData> calculateTotal,
            @Qualifier(GenerateBillingTotalDataStep.WRITER)     FlatFileItemWriter<ReportingData> toOutputFile) {

        return new StepBuilder(STEP_NAME, jobRepository)
                .<BillingData, ReportingData>chunk(100, transactionManager)
                .reader(fromBillingDataTable)
                .processor(calculateTotal)
                .writer(toOutputFile)
                .faultTolerant()
                .retry(PricingException.class)
                .retryLimit(100)
                .build();
    }
}
