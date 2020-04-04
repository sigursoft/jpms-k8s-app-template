package com.sigursoft.jpms.k8s.app;

import org.joda.money.Money;

import java.util.logging.Logger;

public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.toString());

    public static void main(String[] args) {
        Money salary = Money.parse("USD 10000");
        LOGGER.info("Hello From JPMS K8s sample application.");
        LOGGER.info(String.format("Here is another log message. %s", salary.toString()));
    }

}
