package example.blueprint.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@lombok.extern.slf4j.Slf4j
public class FilePreparationTasklet implements Tasklet {

    public static final String STAGING = "staging";

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws IOException {
        var jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();

        var inputFile = Optional.ofNullable(jobParameters.getString("input.file"));
        var source = Paths.get(inputFile.orElseThrow(
                () -> new IllegalArgumentException("input.file parameter is required")));

        var fileName = source.toFile().getName();
        var target = Paths.get(STAGING, fileName);

        var finalLocation = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("source: {}, target: {}", source, finalLocation);

        return RepeatStatus.FINISHED;
    }
}
