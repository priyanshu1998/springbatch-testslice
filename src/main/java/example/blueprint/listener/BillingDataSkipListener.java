package example.blueprint.listener;

import example.blueprint.infrastructure.data.BillingData;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class BillingDataSkipListener implements SkipListener<BillingData, BillingData> {

    public Path skippedItemsFile;

    public BillingDataSkipListener(String skippedItemsFile) {
        this(Paths.get(skippedItemsFile));
    }

    public BillingDataSkipListener(Path filePath){
        this.skippedItemsFile = filePath;
    }

    @Override
    public void onSkipInRead(@NonNull Throwable throwable) {
        if (throwable instanceof FlatFileParseException exception) {
            String skippedLine = getSkippedLine(exception);
            try {
                Files.writeString(this.skippedItemsFile, skippedLine, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException("Unable to write skipped item " + skippedLine);
            }
        }
    }

    @VisibleForTesting
    String getSkippedLine(FlatFileParseException exception) {
        String rawLine = exception.getInput();
        int lineNumber = exception.getLineNumber();
        return lineNumber + "|" + rawLine + System.lineSeparator();
    }
}
