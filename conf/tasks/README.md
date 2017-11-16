# Mongo Tasks

The text files in this folder represent data for `MongoTask` jobs.

`MongoTask` instances are configured to run at startup of the service by the `MongoStartupRunner`

Tasks are versioned to ensure that each task is only run once so that database consistency is maintained.
This is a very basic form of DB version control to allow us to run arbitrary tasks gainst the database
without having to involve direct DB access.

## Current tasks

### AddEnvelopes

Convenience task for adding envelopeIds (back) in to our local mongo database.
EnvelopeIds represent what data is awaiting download from file-upload for processing.
Each `AddEnvelopes-xxx.txt` file should contain a series of rows with a single UUID envelopeId
on each line.

### RemoveEnvelopes

Convenience task for removing envelopeIds from our local mongo database.
This is generally used when a file getsstuck in a processing loop (should no longer happen).
Each `RemoveEnvelopes-xxx.txt` file should contain a series of rows with a single UUID envelopeId
on each line.

### AddTimestamps

No file data for this task.
Add timestamps to the envelope collection.
This should be a 1 off task and should not require iterations.