TaskQueue system built using Java, Spring Boot, Postgres, Kafka and react for frontend. The architecture uses OutBox pattern. the project can current handle three tasks - 

1. Sending email
2. Generating a pdf
3. Delivering webhooks

All the above tasks are being handled asynchronously by Kafka. There is an implementation of DLQ (Dead Letter Queue) as well. If some issue occures while executing the tasks and the task fails, the system has an exponential back-off of 5s, 15s and 45s - after which the specific task would go to the dlq from where it can be retried by the admin if a fix gets found. For storing the generated PDFs, I am using my device memory though I plan on to use S3 eventually.

I have also implemented jwt authentication using Spring Security. As of no I have implemented it for the admin routes, though I plan on implementing it for the user routes as well. 

The frontend part has been completed. The frontend is able to talk to the backend, token is properly being stored in the local storage and the authentication is working fine. The frontend is also usable, the admin is able to view task status, task data(paginated), dlq data and the pdfs generated with all the necessary stats. 


