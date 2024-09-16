package com.spglobal.coding.runner;

import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.spglobal.coding.steps" // Package where step definitions are located
)
public class CucumberTestRunner {
}

