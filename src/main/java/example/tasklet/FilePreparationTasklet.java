package example.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.NonNull;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class FilePreparationTasklet implements Tasklet {

    public static final String STAGING = "staging";

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();

        var inputFile = Optional.ofNullable(jobParameters.getString("input.file"));
        var source = Paths.get(inputFile.orElseThrow(
                () -> new IllegalArgumentException("input.file parameter is required")));

        var fileName = source.toFile().getName();
        var target = Paths.get(STAGING, fileName);

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return RepeatStatus.FINISHED;
    }
}
