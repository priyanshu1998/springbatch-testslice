package example.blueprint.processor;

import example.billingjob.service.PricingService;
import example.blueprint.infrastructure.data.BillingData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BillingDataProcessorTest {

    private final BillingDataProcessor billingDataProcessor;
    private final PricingService pricingService;

    BillingDataProcessorTest(@Mock PricingService pricingService){
        this.pricingService = pricingService;
        this.billingDataProcessor = new BillingDataProcessor(pricingService, 150.0f);
    }

    @Test
    void thresholdExceeded(){
        Mockito.when(pricingService.getDataPricing()).thenReturn(0.01f);
        Mockito.when(pricingService.getCallPricing()).thenReturn(0.5f);
        Mockito.when(pricingService.getSmsPricing()).thenReturn(0.1f);

//        69.87,289,77

        BillingData billingData = new BillingData(2023,1,101,"404-555-1001",69.87f,289,77);
        assertNotNull(billingDataProcessor.process(billingData));
    }

    @Test
    void threshHoldIsNotExceeded(){
        Mockito.when(pricingService.getDataPricing()).thenReturn(0.01f);
        Mockito.when(pricingService.getCallPricing()).thenReturn(0.5f);
        Mockito.when(pricingService.getSmsPricing()).thenReturn(0.1f);

        BillingData billingData = new BillingData(2023,1,101,"404-555-1001",69.87f,100,77);
        assertNull(billingDataProcessor.process(billingData));
    }

}