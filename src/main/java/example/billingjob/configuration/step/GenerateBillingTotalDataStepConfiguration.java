package example.billingjob.configuration.step;

import example.billingjob.configuration.SharedConfiguration;
import example.blueprint.exception.PricingException;
import example.blueprint.infrastructure.mapper.ReportingDataFieldSetMapper;
import example.blueprint.infrastructure.data.BillingData;
import example.blueprint.infrastructure.data.ReportingData;
import example.blueprint.processor.BillingDataProcessor;
import example.billingjob.service.PricingService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;

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
 *                 <td> {@link BillingDataProcessor BillingDataProcessor} </td>
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
@Slf4j
@lombok.Getter
public class GenerateBillingTotalDataStepConfiguration {
    private static final String STEP_NAME = "generate-billing-total-data";
    public static final String READER_NAME = "billing-data-table-reader";
    public static final String WRITER_NAME = "billing-data-file-writer";

    private final SharedConfiguration sharedConfiguration;

    @Configuration
    @lombok.RequiredArgsConstructor
    public static class Components {
        // Step 3 ============================================================================================
        @Bean
        @StepScope
        public JdbcCursorItemReader<BillingData> billingDataTableReader(
                @Value("#{jobParameters['data.year']}") Integer year,
                @Value("#{jobParameters['data.month']}") Integer month, SharedConfiguration sharedConfiguration) {

            String sql = String.format("select * from billing_data where data_year = %d and data_month = %d",
                    year, month);

            log.debug("reader: {}, sql: {}", READER_NAME, sql);
            return new JdbcCursorItemReaderBuilder<BillingData>()
                    .name(READER_NAME)
                    .dataSource(sharedConfiguration.dataSource())
                    .sql(sql)
                    .rowMapper(new DataClassRowMapper<>(BillingData.class))
                    .build();
        }

        @Bean
        public BillingDataProcessor billingDataProcessor(
                PricingService pricingService,
                @Value("${cellular.plan.spending-threshold:150.0f}") float spendingThreshold) {

            return new BillingDataProcessor(pricingService, spendingThreshold);
        }

        @Bean
        @StepScope
        public FlatFileItemWriter<ReportingData> billingDataFileWriter(
                @Value("#{jobParameters['output.file']}") String outputFile) {
            log.debug("writer: {},  outputFile: {}", WRITER_NAME, outputFile);

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
    }


    /**
     * Creates a CSV file that contains all the billing totals.
     *
     * @param fromBillingDataTable {@link #billingDataTableReader JdbcCursorItemReader}
     * @param calculateTotal {@link #billingDataProcessor ItemProcessor}
     * @param toOutputFile {@link #billingDataFileWriter FlatFileItemWriter}
     */
    @Bean("generateBillingTotalDataStep")
    public Step create(
            @Qualifier("billingDataTableReader") JdbcCursorItemReader<BillingData> fromBillingDataTable,
            @Qualifier("billingDataProcessor") ItemProcessor<BillingData, ReportingData> calculateTotal,
            @Qualifier("billingDataFileWriter") FlatFileItemWriter<ReportingData> toOutputFile) {
        return new StepBuilder(STEP_NAME, sharedConfiguration.jobRepository())
                .<BillingData, ReportingData>chunk(100, sharedConfiguration.transactionManager())
                .reader(fromBillingDataTable)
                .processor(calculateTotal)
                .writer(toOutputFile)
                .faultTolerant()
                .retry(PricingException.class)
                .retryLimit(100)
                .build();
    }
}
