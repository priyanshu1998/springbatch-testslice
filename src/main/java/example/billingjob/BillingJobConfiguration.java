package example.billingjob;

import example.billingjob.entity.BillingData;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

@Configuration
public class BillingJobConfiguration {


  public static final String BILLING_JOB = "billingJob";

  private String decorateStepName(String stepName) {
    return String.format("[%s]%s", BILLING_JOB, stepName);
  }

  @Bean
  public JobParametersValidator inputFileParamValidator() {
    return new BillingJobParametersValidator();
  }

  @Bean
  public Step filePreparationStep(JobRepository repository, PlatformTransactionManager transactionManager){
    String stepName = decorateStepName("filePreparationStep");
    return new StepBuilder(stepName, repository)
      .tasklet(new FilePreparationTasklet(), transactionManager)
      .build();
  }

  @Bean
  public FlatFileItemReader<BillingData> billingDataFileReader() {
    return new FlatFileItemReaderBuilder<BillingData>()
            .name("billingDataFileReader")
            .resource(new FileSystemResource("staging/billing-2023-01.csv"))
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
    return new StepBuilder("fileIngestion", jobRepository)
            .<BillingData, BillingData>chunk(100, transactionManager)
            .reader(fromFlatFile)
            .writer(toRDBMSTable)
            .build();
  }

  @Bean
  public Job job(JobRepository jobRepository,
                 //description Action
                 @Qualifier("filePreparationStep") Step prepareFlatFile,
                 @Qualifier("inputFileParamValidator") JobParametersValidator validInputFileParam,
                 @Qualifier("ingestDataStep") Step fromFlatFileToRDBMS) {
    return new JobBuilder("BillingJob", jobRepository)
            .validator(validInputFileParam)
            .start(prepareFlatFile)
            .next(fromFlatFileToRDBMS)
            .build();
  }

}
