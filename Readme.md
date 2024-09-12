# Instrument Price Tracker

## Overview

**Instrument Price Tracker** is a robust and scalable system designed for managing and tracking the latest prices of various financial instruments. The system supports batch processing of price records, retrieval of the latest price information, and efficient management of price records based on instrument types and durations.

## Features

- **Batch Processing**: Efficiently processes batches of price records, updating the latest prices for each instrument.
- **Price Retrieval**: Retrieve the latest price record for an instrument by its ID or name.
- **Instrument Type Support**: Handle price records for different instrument types with the ability to filter by type.
- **Historical Price Lookup**: Fetch price records within a specified duration.
- **Price Clearing**: Clear price records either for specific instruments or all records.

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven for dependency management
- SLF4J for logging

### Run the Application

You can run the application using your IDE or via the command line. For command-line execution:

```bash
mvn exec:java -Dexec.mainClass="com.spglobal.coding.Main"