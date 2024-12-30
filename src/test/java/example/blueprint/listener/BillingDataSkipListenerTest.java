package example.blueprint.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private BillingDataSkipListener skipListener;

    @Mock
    private Path filePath;

    @Test
    void WhenFlatFileParseExceptionOccurs(){
        FlatFileParseException throwable = new FlatFileParseException("<msg>", "<input line>", 7);

            try (MockedStatic<Files> mockedStatic = Mockito.mockStatic(Files.class)) {
                skipListener.onSkipInRead(throwable);

                mockedStatic.verify(()->Files.writeString(filePath, skipListener.getSkippedLine(throwable),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE));
        }
    }


    @Test
    void WhenIOExceptionOccurs(){
        FlatFileParseException throwable = new FlatFileParseException("<msg>", "<input line>", 7);

        try (MockedStatic<Files> mockedStatic = Mockito.mockStatic(Files.class)) {
            mockedStatic.when(()->Files.writeString(filePath, skipListener.getSkippedLine(throwable),
                            StandardOpenOption.APPEND, StandardOpenOption.CREATE)).thenThrow(IOException.class);

            assertThatThrownBy(()->skipListener.onSkipInRead(throwable)).isInstanceOf(RuntimeException.class);

            mockedStatic.verify(()->Files.writeString(skipListener.skippedItemsFile,
                    skipListener.getSkippedLine(throwable), StandardOpenOption.APPEND, StandardOpenOption.CREATE));
        }
    }



}