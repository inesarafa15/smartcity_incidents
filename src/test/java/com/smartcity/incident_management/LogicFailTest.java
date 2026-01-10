package com.smartcity.incident_management;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicFailTest {

    @Test
    void intentionalFailure_ForCIDemo() {
        // Given
        int expected = 100;
        int actual = 50 +50;// Fait 100

        // When & Then
        // FIX: Changer 49 en 50 pour que le test passe
        assertEquals(expected, actual, "ERREUR VOLONTAIRE : 99 n'est pas égal à 100. La CI doit échouer ici.");
    }
}
