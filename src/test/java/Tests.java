import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Tests {
    @Test
    public void addition() {
        // Given
        int a = 1;
        int b = 1;

        // When
        int actual = a + b;

        // Then
        int expected = 2;
        assertEquals(expected, actual);
    }
}
