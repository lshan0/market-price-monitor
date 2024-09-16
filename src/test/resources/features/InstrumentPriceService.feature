Feature: InstrumentPriceService Tests

    Scenario: Successfully starting a new batch
        Given I have not started a batch
        When I start a new batch
        Then a batch with random UUID should be created
        And the batch status should be "STARTED"

    Scenario: Uploading requests to an active batch
        Given I start a new batch
        And I have the following update requests:
            | id  | instrument | instrumentType | value | currency | requestTime          |
            | 1 | instrument1| STOCK          | 100   | USD      | 2024-09-15T10:00:00 |
        When I upload these requests to batch
        Then the requests should be uploaded successfully
        And the batch status should be "IN_PROGRESS"

    Scenario: Uploading requests to a non-existent batch
        Given a batch with ID "batch999" does not exist
        And I have the following update requests:
            | id  | instrument | instrumentType | value | currency | requestTime |
            | 2 | instrument2| BOND         | 100   | USD      | 2024-09-15T10:00:00 |
        When I try to upload these requests to batch "batch999"
        Then an exception should be thrown with the message "Batch run with ID batch999 does not exist."

    Scenario: Completing a batch successfully
        Given a batch with ID is started and valid records have been uploaded
        When I complete the batch
        Then I wait for 1 second
        Then the batch status should be "COMPLETED"

    Scenario: Completing a batch with errors
        Given a batch is started and invalid records have been uploaded
        When I complete the batch
        Then I wait for 1 second
        Then the batch status should be "PROCESSED_WITH_ERRORS"

    Scenario: Completing a batch that is not in progress
        Given I start a new batch
        When I try to complete that batch
        Then an exception should be thrown where the message contains "cannot be processed. Batch is not in progress."

    Scenario: Cancelling a STARTED batch
        Given I start a new batch
        When I cancel that batch
        Then the batch status should be "CANCELLED"

    Scenario: Cancelling an IN_PROGRESS batch
        Given a batch with ID is started and valid records have been uploaded
        When I cancel that batch
        Then the batch status should be "CANCELLED"