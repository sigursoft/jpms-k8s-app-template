package com.sigursoft.jpms.k8s.app;

import java.util.logging.Logger;

public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.toString());

    public static void main(String[] args) {
        LOGGER.info("Hello From JPMS K8s sample application.");
        var result = 1 + 1;
        LOGGER.info(String.format("Result of the computation: %d", result));
    }

}
