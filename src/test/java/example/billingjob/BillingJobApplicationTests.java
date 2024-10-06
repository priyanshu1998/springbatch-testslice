package example.billingjob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@lombok.RequiredArgsConstructor
class BillingJobApplicationTests {

	private final ConfigurableApplicationContext context;
	private final JobRepositoryTestUtils jobRepositoryTestUtils;

	@BeforeEach
	void tearDown() {
		this.jobRepositoryTestUtils.removeJobExecutions();
	}


	@Test
	void contextLoads() {
		assertThat(AssertableApplicationContext.get(() -> context)) //
				.hasSingleBean(JobRepository.class)
				.hasSingleBean(TransactionManager.class)
				.satisfies(ctx -> {
					// assert that correct job bean is present
					assertThat(ctx.getBean(Job.class).getName())
							.isEqualTo("BillingJob");
				});
	}

	@Test
	void testJobExecution(CapturedOutput output) throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder()
				.addString("input.file", "/some/input/file")
				.addString("file.format", "csv", false)
				.toJobParameters();
		// when
		var jobLauncher = context.getBean(JobLauncher.class);
		var job = context.getBean(Job.class);

		JobExecution jobExecution = jobLauncher.run(job, jobParameters);
		// then
		Assertions.assertTrue(output.getOut().contains("processing billing information from file /some/input/file"));
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

}
