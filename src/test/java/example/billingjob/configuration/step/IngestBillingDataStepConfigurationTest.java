//package example.billingjob.configuration.step;
//
//import example.billingjob.configuration.BatchConfig;
//import example.listener.BillingDataSkipListener;
//import example.model.BillingData;
//import example.support.SkipReaderStub;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.mockito.AdditionalAnswers;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.Mockito;
//import org.springframework.batch.core.ExitStatus;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.item.Chunk;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.batch.item.database.JdbcBatchItemWriter;
//import org.springframework.batch.test.StepRunner;
//import org.springframework.batch.test.StepScopeTestExecutionListener;
//import org.springframework.batch.test.context.SpringBatchTest;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.annotation.Primary;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.TestConstructor;
//import org.springframework.test.context.TestExecutionListeners;
//import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
//import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
//import org.springframework.test.jdbc.JdbcTestUtils;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBatchTest
//@SpringJUnitConfig(classes = {BatchConfig.class, IngestBillingDataStepConfiguration.class})
//@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@lombok.RequiredArgsConstructor
//@Import(IngestBillingDataStepConfigurationTest.OverridingConfiguration.class)
//class IngestBillingDataStepConfigurationTest {
//    private final StepRunner stepRunner;
//    private final JdbcTemplate jdbcTemplate;
//
//
//    @Slf4j
//    @TestConfiguration
//    public static class OverridingConfiguration{
//
//        @Bean
//        @StepScope
//        public BillingDataSkipListener parseFailListener(@Value("#{jobParameters['skip.file']}") String skippedFile) {
//            return new BillingDataSkipListener(skippedFile);
//        }
//    }
//
//
//
//    @Test
//    void contextLoads(ConfigurableApplicationContext context) {
//        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);
//
//        assertThat(assertableContext).hasSingleBean(Step.class);
//        assertEquals(context.getBean(Step.class).getName(), "ingest-billing-data");
//    }
//
//    @Test
//    void testExecute(@Qualifier("ingestBillingDataStep") Step step){
//        JobParameters jobParameters = getJobParameters();
//
//        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);
//        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);
//
//
//        Path billingReport = Paths.get("staging", "billing-report-2023-01.csv");
//
//        JdbcTestUtils.countRowsInTable( jdbcTemplate, "")
//        Assertions.assertTrue(Files.exists(billingReport));
//    }
//
//    private static JobParameters getJobParameters() {
//        return new JobParametersBuilder()
//                .addString("input.file", "input/billing-2023-01.csv")
//                .toJobParameters();
//    }
//}