
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FraudDetectionServiceTest {

    private final FraudDetectionService service =
            new FraudDetectionService();

    @Test
    void testAccountDrainingTransactionIsHighRisk() {
        Transaction transaction = new Transaction(
                "TX001", "AccountA", "AccountB",
                50000.0, true, "TRANSFER",
                50000.0, 0.0, 1
        );

        assertEquals(75, service.calculateRiskScore(transaction));
        assertEquals("HIGH RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testSmallPaymentIsLowRisk() {
        Transaction transaction = new Transaction(
                "TX002", "AccountC", "AccountD",
                100.0, false, "PAYMENT",
                1000.0, 900.0, 1
        );

        assertEquals(0, service.calculateRiskScore(transaction));
        assertEquals("LOW RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testLargeTransferIsMediumRisk() {
        Transaction transaction = new Transaction(
                "TX003", "AccountE", "AccountF",
                20000.0, false, "TRANSFER",
                50000.0, 30000.0, 1
        );

        assertEquals(25, service.calculateRiskScore(transaction));
        assertEquals("MEDIUM RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testVeryLargeTransferWithoutAccountDrain() {
        Transaction transaction = new Transaction(
                "TX004", "AccountG", "AccountH",
                200000.0, false, "TRANSFER",
                500000.0, 300000.0, 1
        );

        assertEquals(30, service.calculateRiskScore(transaction));
        assertEquals("MEDIUM RISK", service.evaluateTransaction(transaction));
    }



    
    @Test
    void testExactlyTenThousandDoesNotTriggerLargeAmountRule() {
        Transaction transaction = new Transaction(
                "TX005", "AccountA", "AccountB",
                10000.0, false, "TRANSFER",
                50000.0, 40000.0, 1
        );

        assertEquals(15, service.calculateRiskScore(transaction));
        assertEquals("LOW RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testZeroBalanceDoesNotTriggerAccountDrain() {
        Transaction transaction = new Transaction(
                "TX006", "AccountA", "AccountB",
                50000.0, false, "CASH_OUT",
                0.0, 0.0, 1
        );

        assertEquals(25, service.calculateRiskScore(transaction));
        assertEquals("MEDIUM RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testPaymentDoesNotReceiveAccountDrainBonus() {
        Transaction transaction = new Transaction(
                "TX007", "AccountA", "AccountB",
                50000.0, false, "PAYMENT",
                50000.0, 0.0, 1
        );

        assertEquals(10, service.calculateRiskScore(transaction));
        assertEquals("LOW RISK", service.evaluateTransaction(transaction));
    }

    @Test
    void testExactlyOneHundredThousandDoesNotTriggerVeryLargeRule() {
        Transaction transaction = new Transaction(
                "TX008", "AccountA", "AccountB",
                100000.0, false, "TRANSFER",
                200000.0, 100000.0, 1
        );

        assertEquals(25, service.calculateRiskScore(transaction));
        assertEquals("MEDIUM RISK", service.evaluateTransaction(transaction));
    }

}
