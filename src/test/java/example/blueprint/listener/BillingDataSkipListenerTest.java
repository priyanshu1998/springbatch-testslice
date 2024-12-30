package example.blueprint.listener;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.item.file.FlatFileParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BillingDataSkipListenerTest {

    private BillingDataSkipListener skipListener;

    @TempDir
    private Path tempDir;  // JUnit 5 creates a temporary directory for each test


    @BeforeEach
    void setUp() throws IOException {
        // Create a temp file inside the tempDir
        var skippedItemsFilePath = Files.createFile(tempDir.resolve("skipped-items.txt"));

        // Manually initialize skipListener with the temp file path
        skipListener = new BillingDataSkipListener(skippedItemsFilePath.toString());
    }

    private String getSkippedLine(FlatFileParseException exception) {
        String rawLine = exception.getInput();
        int lineNumber = exception.getLineNumber();
        return lineNumber + "|" + rawLine + System.lineSeparator();
    }


    @Test
    void WhenFlatFileParseExceptionOccurs(){
        FlatFileParseException throwable = new FlatFileParseException("<msg>", "<input line>", 7);

            try (MockedStatic<Files> mockedStatic = Mockito.mockStatic(Files.class)) {
                skipListener.onSkipInRead(throwable);

                mockedStatic.verify(()->Files.writeString(skipListener.skippedItemsFile,
                    this.getSkippedLine(throwable), StandardOpenOption.APPEND, StandardOpenOption.CREATE));
        }
    }


    @Test
    void WhenIOExceptionOccurs(){
        FlatFileParseException throwable = new FlatFileParseException("<msg>", "<input line>", 7);

        try (MockedStatic<Files> mockedStatic = Mockito.mockStatic(Files.class)) {
            mockedStatic.when(()->Files.writeString(skipListener.skippedItemsFile,
                    this.getSkippedLine(throwable), StandardOpenOption.APPEND, StandardOpenOption.CREATE))
                    .thenThrow(IOException.class);

            assertThatThrownBy(()->skipListener.onSkipInRead(throwable)).isInstanceOf(RuntimeException.class);

            mockedStatic.verify(()->Files.writeString(skipListener.skippedItemsFile,
                    this.getSkippedLine(throwable), StandardOpenOption.APPEND, StandardOpenOption.CREATE));
        }
    }



}