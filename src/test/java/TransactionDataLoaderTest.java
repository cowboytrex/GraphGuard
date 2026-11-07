
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionDataLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadsOnlyTransactionsWithinStepRange() throws IOException {

        Path csvFile = tempDir.resolve("transactions.csv");

        String csv = """
                step,type,amount,nameOrig,oldbalanceOrg,newbalanceOrig,nameDest,oldbalanceDest,newbalanceDest,isFraud,isFlaggedFraud
                1,PAYMENT,100,A,1000,900,B,0,0,0,0
                5,TRANSFER,500,C,500,0,D,0,500,1,0
                10,CASH_OUT,200,E,200,0,F,0,200,0,0
                """;

        Files.writeString(csvFile, csv);

        TransactionDataLoader loader = new TransactionDataLoader();

        List<Transaction> transactions =
                loader.loadTransactionsByStep(csvFile.toString(), 5, 10);

        assertEquals(2, transactions.size());
        assertEquals(5, transactions.get(0).getStep());
        assertEquals(10, transactions.get(1).getStep());
        assertTrue(transactions.get(0).isFraud());
        assertFalse(transactions.get(1).isFraud());
    }

    @Test
    void testInvalidStepRangeThrowsException() {

        TransactionDataLoader loader = new TransactionDataLoader();

        assertThrows(IllegalArgumentException.class, () ->
                loader.loadTransactionsByStep("unused.csv", 10, 5)
        );
    }
}
