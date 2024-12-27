package example.processor;

import example.billingjob.service.PricingService;
import example.model.BillingData;
import example.model.ReportingData;
import jakarta.annotation.PostConstruct;
import org.springframework.batch.item.ItemProcessor;

@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class BillingDataProcessor implements ItemProcessor<BillingData, ReportingData> {

    private final PricingService pricingService;
    private final float spendingThreshold;

    @Override
    public ReportingData process(BillingData item) {

        double billingTotal =
                item.dataUsage()     * pricingService.getDataPricing() +
                item.callDuration()  * pricingService.getCallPricing() +
                item.smsCount()      * pricingService.getSmsPricing()  ;

        if (billingTotal < spendingThreshold) {
            return null;
        }

        return new ReportingData(item, billingTotal);
    }

    @PostConstruct
    void init(){
        log.debug("processor: BillingDataProcessor, spendingThreshold: {}", spendingThreshold);
    }
}
