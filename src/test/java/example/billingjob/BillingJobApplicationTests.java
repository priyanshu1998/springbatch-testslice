package example.billingjob;

import example.billingjob.configuration.BatchConfig;

import example.billingjob.configuration.property.PricingProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.FileSystemUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(
		classes = BillingJobApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SpringBatchTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
class BillingJobApplicationTests {

	private final JobRepositoryTestUtils jobRepositoryTestUtils;
	private final JobLauncherTestUtils jobLauncherTestUtils;

	@BeforeAll
	@lombok.SneakyThrows
	void init(@Value("embedded.db.location") String folderPath){
		FileSystemUtils.deleteRecursively(Paths.get(folderPath));
		jobRepositoryTestUtils.removeJobExecutions();
	}

	@Test
	void contextLoads(ApplicationContext context) {
		// verify no profile is loaded
		assertThat(context.getBean(Environment.class).getActiveProfiles()).containsExactly("test");

		assertThat(jobLauncherTestUtils.getJob().getName())
				.isEqualTo("billing-job");
	}

	@Test
	@Sql(statements = BatchConfig.CREATE_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(statements = BatchConfig.DROP_BILLING_TABLE, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void testJobExecution(@Qualifier("dataSource") DataSource dataSource) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("input.file", "input/billing-2023-01.csv")
				.addString("output.file", "staging/billing-report-2023-01.csv")
				.addJobParameter("data.year", 2023, Integer.class)
				.addJobParameter("data.month", 1, Integer.class)
				.toJobParameters();

		// when
		JobExecution execution = jobLauncherTestUtils.launchJob(jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
		Assertions.assertTrue(Files.exists(Paths.get("staging", "billing-2023-01.csv")));

		JdbcTemplate template = new JdbcTemplate(dataSource);
		Assertions.assertEquals(1000, JdbcTestUtils.countRowsInTable(template, "billing_data"));

		Path billingReport = Paths.get("staging", "billing-report-2023-01.csv");
		Assertions.assertTrue(Files.exists(billingReport));

		try(var lines = Files.lines(billingReport)) {
			Assertions.assertEquals(781,lines.count());
		}
	}

}
