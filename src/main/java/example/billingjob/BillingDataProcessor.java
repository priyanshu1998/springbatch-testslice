package example.billingjob;

import example.billingjob.configprops.BillingDataProcessorProperties;
import example.billingjob.entity.BillingData;
import example.billingjob.entity.ReportingData;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@lombok.AllArgsConstructor
public class BillingDataProcessor implements ItemProcessor<BillingData, ReportingData> {

    BillingDataProcessorProperties billingDataProcessorProperties;

    @Override
    public ReportingData process(BillingData item) {
        var prop = billingDataProcessorProperties;
        float spendingThreshold = prop.getSpendingThreshold();

        double billingTotal =
                item.getDataUsage()     * prop.getDataRate() +
                item.getCallDuration()  * prop.getCallRate() +
                item.getSmsCount()      * prop.getSmsRate()  ;

        if (billingTotal < spendingThreshold) {
            return null;
        }

        return new ReportingData(item, billingTotal);
    }
}
