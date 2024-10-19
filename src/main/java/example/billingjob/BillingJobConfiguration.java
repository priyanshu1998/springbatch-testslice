package example.billingjob;

import example.billingjob.configprops.BillingDataProcessorProperties;
import example.billingjob.entity.BillingData;
import example.billingjob.entity.ReportingData;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

@Configuration
@PropertySource("classpath:cellular.plan.properties")
@EnableConfigurationProperties(BillingDataProcessorProperties.class)
public class BillingJobConfiguration {


  public static final String BILLING_JOB = "billingJob";

  private String decorateStepName(String stepName) {
    return String.format("[%s]%s", BILLING_JOB, stepName);
  }

  @Bean
  public JobParametersValidator inputFileParamValidator() {
    return new BillingJobParametersValidator();
  }

  // Step1 ========================================================================================================
  @Bean
  public Step filePreparationStep(JobRepository repository, PlatformTransactionManager transactionManager){
    String stepName = decorateStepName("filePreparationStep");
    return new StepBuilder(stepName, repository)
      .tasklet(new FilePreparationTasklet(), transactionManager)
      .build();
  }
  //================================================================================================================


  // Step2 =========================================================================================================
  @Bean
  @StepScope
  public FlatFileItemReader<BillingData> billingDataFileReader(
          @Value("#{jobParameters['input.file']}") String inputFile) {
    return new FlatFileItemReaderBuilder<BillingData>()
            .name("billingDataFileReader")
            .resource(new FileSystemResource(inputFile))
            .delimited()
            .names("dataYear", "dataMonth", "accountId", "phoneNumber", "dataUsage", "callDuration", "smsCount")
            .targetType(BillingData.class)
            .build();
  }

  @Bean
  public JdbcBatchItemWriter<BillingData> billingDataTableWriter(DataSource dataSource) {
    String sql = "insert into billing_data values (:dataYear, :dataMonth, :accountId, :phoneNumber, :dataUsage, :callDuration, :smsCount)";
    return new JdbcBatchItemWriterBuilder<BillingData>()
            .dataSource(dataSource)
            .sql(sql)
            .beanMapped()
            .build();
  }

  @Bean
  public Step ingestDataStep(
          JobRepository jobRepository, PlatformTransactionManager transactionManager,
          @Qualifier("billingDataFileReader") FlatFileItemReader<BillingData> fromFlatFile,
          @Qualifier("billingDataTableWriter") JdbcBatchItemWriter<BillingData> toRDBMSTable) {
    String stepName = decorateStepName("ingestDataStep");
    return new StepBuilder(stepName, jobRepository)
            .<BillingData, BillingData>chunk(100, transactionManager)
            .reader(fromFlatFile)
            .writer(toRDBMSTable)
            .build();
  }
  // ===================================================================================================

  // Step 3 ============================================================================================
  @Bean
  @StepScope
  public JdbcCursorItemReader<BillingData> billingDataTableReader(
          DataSource dataSource,
          @Value("#{jobParameters['data.year']}") Integer year,
          @Value("#{jobParameters['data.month']}") Integer month) {
    String sql = String.format("select * from BILLING_DATA where DATA_YEAR = %d and DATA_MONTH = %d", year, month);

    return new JdbcCursorItemReaderBuilder<BillingData>()
            .name("billingDataTableReader")
            .dataSource(dataSource)
            .sql(sql)
            .rowMapper(new DataClassRowMapper<>(BillingData.class))
            .build();
  }

  @Bean
  public BillingDataProcessor billingDataProcessor(BillingDataProcessorProperties properties) {
    return new BillingDataProcessor(properties);
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<ReportingData> billingDataFileWriter(
          @Value("#{jobParameters['output.file']}") String outputFile) {
    return new FlatFileItemWriterBuilder<ReportingData>()
            .resource(new FileSystemResource("staging/billing-report-2023-01.csv"))
            .name("billingDataFileWriter")
            .delimited()
            .names("billingData.dataYear", "billingData.dataMonth", "billingData.accountId", "billingData.phoneNumber", "billingData.dataUsage", "billingData.callDuration", "billingData.smsCount", "billingTotal")
            .build();
  }


  @Bean
  public Step generateFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                    JdbcCursorItemReader<BillingData> billingDataTableReader,
                    ItemProcessor<BillingData, ReportingData> billingDataProcessor,
                    FlatFileItemWriter<ReportingData> billingDataFileWriter) {
    String stepName = decorateStepName("generateFileStep");
    return new StepBuilder(stepName, jobRepository)
            .<BillingData, ReportingData>chunk(100, transactionManager)
            .reader(billingDataTableReader)
            .processor(billingDataProcessor)
            .writer(billingDataFileWriter)
            .build();
  }


  // ===================================================================================================
  @Bean
  public Job job(JobRepository jobRepository,
                 //description Action
                 @Qualifier("filePreparationStep") Step prepareFlatFile,
                 @Qualifier("inputFileParamValidator") JobParametersValidator validInputFileParam,
                 @Qualifier("ingestDataStep") Step fromFlatFileToRDBMS,
                 @Qualifier("generateFileStep") Step fromRDBMSToFlatFile) {
    return new JobBuilder("BillingJob", jobRepository)
            .validator(validInputFileParam)
            .start(prepareFlatFile)
            .next(fromFlatFileToRDBMS)
            .next(fromRDBMSToFlatFile)
            .build();
  }
}
