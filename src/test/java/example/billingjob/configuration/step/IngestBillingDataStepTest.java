package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;
import example.billingjob.configuration.BillingJobConfiguration;
import example.billingjob.configuration.SharedConfiguration;
import example.billingjob.configuration.listener.IngestBillingDataStepListenerConfigurations;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBatchTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig(classes = {BatchConfig.class, IngestBillingDataStepConfiguration.class,
        IngestBillingDataStepListenerConfigurations.class, SharedConfiguration.class})
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class IngestBillingDataStepTest {
    private final Step step;
    private final StepRunner stepRunner;
    private final SharedConfiguration sharedConfiguration;

    @BeforeAll
    @lombok.SneakyThrows
    void init() {
        var params = getJobParametersForSomeSkip();
        FileSystemUtils.deleteRecursively(Paths.get(Objects.requireNonNull(params.getString("skip.file"))));
    }

    @Test
    void contextLoads() {
        assertEquals("ingest-billing-data", step.getName());
    }


    @Test
    @Sql(statements = BatchConfig.CREATE_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = BatchConfig.DROP_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testNoSkip() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(sharedConfiguration.dataSource());
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
    void testSkip() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(sharedConfiguration.dataSource());
        JobParameters jobParameters = getJobParametersForSomeSkip();

        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify step run
        assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);

        long countInTable = JdbcTestUtils.countRowsInTable(jdbcTemplate, BillingJobConfiguration.BILLING_DATA_TABLE);
        long countInInputFile = countLines(Paths.get(Objects.requireNonNull(jobParameters.getString("input.file"))));
        long countInSkipFile = countLines(Paths.get(Objects.requireNonNull(jobParameters.getString("skip.file"))));

        log.info("countInTable={}, countInSkipFile={}, countInInputFile={}", countInTable, countInSkipFile, countInInputFile);
        // verify that data is stored
        assertEquals(countInInputFile, countInTable + countInSkipFile);
    }

    private static JobParameters getJobParametersForSomeSkip() {
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