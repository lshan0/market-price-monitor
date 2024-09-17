package com.spglobal.coding.steps;

import com.spglobal.coding.producers.InstrumentProducer;
import com.spglobal.coding.producers.model.PriceRecordUpdateRequestBatch;
import com.spglobal.coding.services.InstrumentPriceService;
import com.spglobal.coding.utils.ChunkProcessor;
import com.spglobal.coding.utils.dto.UpdatePriceRecordRequest;
import com.spglobal.coding.utils.enums.BatchStatus;
import com.spglobal.coding.utils.enums.Currency;
import com.spglobal.coding.utils.enums.InstrumentType;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;

public class InstrumentProducerSteps {

    private final InstrumentPriceService instrumentPriceService = new InstrumentPriceService();
    private final ChunkProcessor chunkProcessor = new ChunkProcessor(instrumentPriceService);
    private final InstrumentProducer producer = new InstrumentProducer(chunkProcessor);
    private final Map<String, PriceRecordUpdateRequestBatch> batchMap = InstrumentProducer.getBatchMap();
    private String batchId;
    private List<Map<String, String>> updateRequests;
    private Exception caughtException;

    @Given("I have not started a batch")
    public void i_have_not_started_a_batch() {
        if (!batchMap.isEmpty()) {
            throw new IllegalStateException("A batch with ID " + batchId + " already exists.");
        }
    }

    @Given("a batch with ID {string} does not exist")
    public void a_batch_with_id_does_not_exist(String invalidBatchId) {
        assertFalse(batchMap.containsKey(invalidBatchId));
    }

    @When("I start a new batch")
    public void i_start_a_new_batch() {
        batchId = producer.startNewBatch();
    }

    @Then("a batch with random UUID should be created")
    public void a_batch_with_ID_should_be_created() {
        assertEquals(1, batchMap.size());
        assertEquals(BatchStatus.STARTED, batchMap.get(batchId).getStatus());
    }

    @And("the batch status should be {string}")
    public void the_batch_status_should_be(String status) {
        BatchStatus batchStatus = BatchStatus.valueOf(status);
        assertEquals(batchStatus, batchMap.get(batchId).getStatus());
    }

    @And("I have the following update requests:")
    public void i_have_the_following_price_records(DataTable dataTable) {
        // Convert the DataTable into a list of maps (key: column header, value: row value)
        updateRequests = dataTable.asMaps(String.class, String.class);
    }

    @When("I upload these requests to batch")
    public void i_upload_these_requests_to_batch() {
        List<UpdatePriceRecordRequest> requests = updateRequests.stream().map(request -> {
            return new UpdatePriceRecordRequest.Builder()
                    .setId(Integer.parseInt(request.get("id")))
                    .setInstrument(request.get("instrument"))
                    .setInstrumentType(InstrumentType.valueOf(request.get("instrumentType")))
                    .setValue(new BigDecimal(request.get("value")))
                    .setCurrency(com.spglobal.coding.utils.enums.Currency.valueOf(request.get("currency")))
                    .setRequestTime(LocalDateTime.parse(request.get("requestTime")))
                    .build();
        }).toList();

        producer.uploadRequests(batchId, requests);
    }

    @Then("the requests should be uploaded successfully")
    public void the_requests_should_be_uploaded_successfully() {
        PriceRecordUpdateRequestBatch batch = batchMap.get(batchId);
        assertNotNull(batch);
        assertEquals(BatchStatus.IN_PROGRESS, batch.getStatus());
    }

    @When("I try to upload these requests to batch {string}")
    public void i_try_to_upload_these_requests_to_batch(String batchId) {
        List<UpdatePriceRecordRequest> requests = updateRequests.stream().map(request -> {
            return new UpdatePriceRecordRequest.Builder()
                    .setId(Integer.parseInt(request.get("id")))
                    .setInstrument(request.get("instrument"))
                    .setInstrumentType(InstrumentType.valueOf(request.get("instrumentType")))
                    .setValue(new BigDecimal(request.get("value")))
                    .setCurrency(Currency.valueOf(request.get("currency")))
                    .setRequestTime(LocalDateTime.parse(request.get("requestTime")))
                    .build();
        }).toList();

        try {
            producer.uploadRequests(batchId, requests);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            caughtException = e;
            assertTrue(e.getMessage().contains("does not exist."));
        }
    }

    @Then("an exception should be thrown with the message {string}")
    public void exceptionShouldBeThrownWithMessage(String expectedMessage) {
        assertNotNull("Expected an exception to be thrown, but none was caught", caughtException);
        assertEquals("Exception message does not match", expectedMessage, caughtException.getMessage());
    }

    @When("I complete the batch")
    public void i_complete_the_batch_with_ID() {
        producer.completeBatch(batchId);
    }

    @Then("I wait for {int} second")
    public void i_wait_for_seconds(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }

    @When("I try to cancel the batch with ID {string}")
    public void i_try_to_cancel_the_batch_with_ID(String batchId) {
        try {
            producer.cancelBatch(batchId);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("cannot be cancelled as it is not in progress."));
        }
    }

    @When("I retrieve the batch with ID {string}")
    public void i_retrieve_the_batch_with_ID(String batchId) {
        PriceRecordUpdateRequestBatch requestBatch = batchMap.get(batchId);
        assertNotNull(requestBatch);
    }

    @Given("a batch with ID is started and valid records have been uploaded")
    public void a_valid_batch_is_in_progress() {
        batchId = producer.startNewBatch();
        UpdatePriceRecordRequest request = new UpdatePriceRecordRequest.Builder()
                .setRequestTime(LocalDateTime.now())
                .setCurrency(Currency.INR)
                .setId(1)
                .setValue(BigDecimal.valueOf(100))
                .setInstrument("Silver")
                .setInstrumentType(InstrumentType.COMMODITIES)
                .build();

        producer.uploadRequests(batchId, List.of(request));
    }

    @Given("a batch is started and invalid records have been uploaded")
    public void an_invalid_batch_is_in_progress() {
        batchId = producer.startNewBatch();
        UpdatePriceRecordRequest request1 = new UpdatePriceRecordRequest.Builder()
                .setRequestTime(LocalDateTime.now())
                .setCurrency(Currency.INR)
                .setId(1)
                .setValue(BigDecimal.valueOf(100))
                .setInstrument("instrument1")
                .build();

        UpdatePriceRecordRequest request2 = new UpdatePriceRecordRequest.Builder()
                .setRequestTime(LocalDateTime.now())
                .setCurrency(null)
                .setId(2)
                .setValue(null)
                .setInstrument(null)
                .build();

        producer.uploadRequests(batchId, List.of(request1, request2));
    }

    @When("I try to complete that batch")
    public void i_try_to_complete_that_batch() {
        try{
            producer.completeBatch(batchId);
            fail("Expected an exception to be thrown");
        } catch (IllegalStateException e) {
            caughtException = e;
        }
    }

    @Then("an exception should be thrown where the message contains {string}")
    public void exception_should_contain(String message) {
        assertNotNull("Expected an exception to be thrown, but none was caught", caughtException);
        assertTrue(caughtException.getMessage().contains(message));
    }

    @When("I cancel that batch")
    public void i_cancel_that_batch() {
        producer.cancelBatch(batchId);
    }
}

