package example.billingjob.configuration.step;

import example.billingjob.configuration.BatchConfig;
import example.billingjob.configuration.SharedConfiguration;
import example.billingjob.configuration.property.PricingProperties;
import example.billingjob.service.PricingService;
import example.blueprint.infrastructure.data.BillingData;
import example.blueprint.infrastructure.data.ReportingData;
import example.blueprint.processor.BillingDataProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringJUnitConfig(classes = {GenerateBillingTotalDataStepConfiguration.Components.class,
        BatchConfig.class, PricingService.class, SharedConfiguration.class})
@EnableConfigurationProperties(PricingProperties.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class GenerateBillingTotalDataStepTest {
    private final StepRunner stepRunner;
    private final JdbcCursorItemReader<BillingData> fromBillingDataTable;
    private final BillingDataProcessor calculateTotal;
    private final FlatFileItemWriter<ReportingData> toOutputFile;
    private final SharedConfiguration sharedConfiguration;

    private Step getStep(JdbcCursorItemReader<BillingData> reader,
                         ItemProcessor<BillingData, ReportingData> processor,
                         FlatFileItemWriter<ReportingData> writer) {
        var generateBillingTotalDataStepConfiguration = new GenerateBillingTotalDataStepConfiguration(sharedConfiguration);

        return generateBillingTotalDataStepConfiguration.create(reader, processor, writer);
    }

    private void testContextAssertions(ConfigurableApplicationContext context){
        AssertableApplicationContext assertableContext = AssertableApplicationContext.get(() -> context);

        // there should be are two beans: `dataSource` and `batchDataSource`
        assertThat(assertableContext.getBeansOfType(DataSource.class)).hasSize(2)
                .satisfies(map -> assertTrue(Mockito.mockingDetails(map.get("batchDataSource")).isMock()))
                .satisfies(map -> assertFalse(Mockito.mockingDetails(map.get("dataSource")).isMock()));

        assertFalse(Mockito.mockingDetails(sharedConfiguration.dataSource()).isMock());
    }


    
    @Test
    void contextLoads(ConfigurableApplicationContext context) {
        this.testContextAssertions(context);

        Step realObject = this.getStep(fromBillingDataTable, calculateTotal, toOutputFile);
        assertEquals("generate-billing-total-data", realObject.getName());
    }

    @Test
    @lombok.SneakyThrows
    void testExecuteUsingMocks() {
        JdbcCursorItemReader<BillingData> reader = Mockito.mock();
        FlatFileItemWriter<ReportingData> writer = Mockito.mock();

        Step step = this.getStep(reader, calculateTotal, writer);

        Mockito.when(reader.read()).thenReturn(
                new BillingData(2023,1,102,"404-555-1002",15.72f,110,827),
                new BillingData(2023,1,101,"404-555-1001",69.87f,289,77),
                null);

        Mockito.doAnswer(invocation -> {
            var args = List.of(invocation.getArguments());
            assertEquals(1, args.size());
            return null;
        }).when(writer).write(Mockito.any());

        JobParameters jobParameters = getJobParameters();
        stepRunner.launchStep(step, jobParameters);

        Mockito.verify(reader, Mockito.times(3)).read();
        Mockito.verify(writer, Mockito.times(1)).write(Mockito.any());
    }

    private void populateDatabase(){
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(new ClassPathResource("populate-billing.sql"));
        DatabasePopulatorUtils.execute(databasePopulator, sharedConfiguration.dataSource());
    }

    @Test
    @Sql(statements = BatchConfig.CREATE_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = BatchConfig.DROP_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testExecuteUsingRealObjects() {
        populateDatabase();
        JobParameters jobParameters = getJobParameters();
        Step step = this.getStep(fromBillingDataTable, calculateTotal, toOutputFile);


        JobExecution jobExecution = stepRunner.launchStep(step, jobParameters);

        // verify the step run
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Path billingReport = Paths.get("staging", "billing-2023-01.csv");

        // verify that file is created
        assertTrue(Files.exists(billingReport));
    }

    private JobParameters getJobParameters() {
        return new JobParametersBuilder()
                .addString("output.file", "staging/billing-report-2023-01.csv")
                .addJobParameter("data.year", 2023, Integer.class)
                .addJobParameter("data.month", 1, Integer.class)
                .toJobParameters();
    }
}