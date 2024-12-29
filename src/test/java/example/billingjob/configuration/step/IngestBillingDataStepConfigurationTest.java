package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;
import example.billingjob.configuration.BillingJobConfiguration;
import example.blueprint.listener.BillingDataSkipListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBatchTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringJUnitConfig(classes = {BatchConfig.class, IngestBillingDataStepConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Import(IngestBillingDataStepConfigurationTest.OverridingConfiguration.class)
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class IngestBillingDataStepConfigurationTest {
    private final StepRunner stepRunner;

    @TestConfiguration
    public static class OverridingConfiguration {
        @Bean
        @StepScope
        public BillingDataSkipListener parseFailListener(@Value("#{jobParameters['skip.file']}") String skippedFile) {
            assertThat(skippedFile).isNotBlank();
            return new BillingDataSkipListener(skippedFile);
        }
    }

    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        // verify no profile is loaded
        assertThat(context.getBean(Environment.class).getActiveProfiles()).isEmpty();


        // verify correct step is loaded
        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);
        assertThat(assertableContext).hasSingleBean(Step.class);
        assertEquals("ingest-billing-data", context.getBean(Step.class).getName());

    }


    @Test
    @Sql(statements = BatchConfig.CREATE_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = BatchConfig.DROP_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testExecute(@Qualifier("ingestBillingDataStep") Step step,
                     @Qualifier("dataSource") DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JobParameters jobParameters = getJobParametersForNoSkip();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify step run
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);

        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, BillingJobConfiguration.BILLING_DATA_TABLE);

        // verify that data is stored
        log.info("Found {} rows in {} table", count, BillingJobConfiguration.BILLING_DATA_TABLE);
        assertThat(count).isEqualTo(1000);
    }

    private static JobParameters getJobParametersForNoSkip() {
        return new JobParametersBuilder()
                .addString("input.file", "input/billing-2023-01.csv")
                .toJobParameters();
    }


    @Test
    @Sql(statements = BatchConfig.CREATE_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = BatchConfig.DROP_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testSkip(@Qualifier("ingestBillingDataStep") Step step,
                  @Qualifier("dataSource") DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JobParameters jobParameters = getJobParametersSomeSkip();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify step run
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);

        long countInTable = JdbcTestUtils.countRowsInTable(jdbcTemplate, BillingJobConfiguration.BILLING_DATA_TABLE);
        long countInInputFile = countLines(Paths.get(jobParameters.getString("input.file")));
        long countInSkipFile = countLines(Paths.get(jobParameters.getString("skip.file")));

        log.info("countInTable={}, countInSkipFile={}, countInInputFile={}", countInTable, countInSkipFile, countInInputFile);
        // verify that data is stored
        assertEquals(countInInputFile, countInTable + countInSkipFile);
    }

    private static JobParameters getJobParametersSomeSkip() {
        return new JobParametersBuilder()
                .addString("input.file", "input/billing-2023-03.csv")
                .addString("skip.file", "staging/billing-skipped-2023-03.csv")
                .toJobParameters();
    }


    @lombok.SneakyThrows
    private long countLines(Path filePath) {
        try (var lines = Files.lines(filePath)) {
            return lines.count();
        }
    }


}