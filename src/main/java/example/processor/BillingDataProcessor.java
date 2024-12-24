package example.processor;

import example.billingjob.service.PricingService;
import example.model.BillingData;
import example.model.ReportingData;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

@lombok.RequiredArgsConstructor
public class BillingDataProcessor implements ItemProcessor<BillingData, ReportingData> {

    final PricingService pricingService;

    @Value("${cellular.plan.spending-threshold:150.0f}")
    private float spendingThreshold;

    @Override
    public ReportingData process(BillingData item) {

        double billingTotal =
                item.getDataUsage()     * pricingService.getDataPricing() +
                item.getCallDuration()  * pricingService.getCallPricing() +
                item.getSmsCount()      * pricingService.getSmsPricing()  ;

        if (billingTotal < spendingThreshold) {
            return null;
        }

        return new ReportingData(item, billingTotal);
    }
}
