package com.smartcity.incident_management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CiDemoIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndFailsDemonstration() {
        // Ce test vérifie que le contexte Spring démarre bien
        assertNotNull(context, "Le contexte Spring devrait être chargé");

        // ECHEC VOLONTAIRE pour la démo CI
        // Correction : Pour corriger ce test, commentez ou supprimez la ligne
    }
}