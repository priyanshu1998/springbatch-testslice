package example.model;

@lombok.Data
@lombok.RequiredArgsConstructor
@lombok.experimental.FieldNameConstants
public class BillingData {
    private int dataYear;
    private int dataMonth;
    private int accountId;
    private String phoneNumber;
    private float dataUsage;
    private int callDuration;
    private int smsCount;

    @lombok.Generated
    public float dataUsage(){
        return dataUsage;
    }

    @lombok.Generated
    public int callDuration(){
        return callDuration;
    }

    @lombok.Generated
    public int smsCount(){
        return smsCount;
    }
}
