package example.blueprint.infrastructure.mapper;

import example.blueprint.infrastructure.data.ReportingData;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.lang.NonNull;

public class ReportingDataFieldSetMapper implements FieldExtractor<ReportingData> {
    BeanWrapperFieldExtractor<ReportingData> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();

    public ReportingDataFieldSetMapper(String... fields){
        beanWrapperFieldExtractor.setNames(fields);
        try {
            beanWrapperFieldExtractor.afterPropertiesSet();
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to initialize DelimitedLineAggregator", e);
        }
    }

    @Override
    public @NonNull Object[] extract(@NonNull ReportingData item) {
        return beanWrapperFieldExtractor.extract(item);
    }
}
