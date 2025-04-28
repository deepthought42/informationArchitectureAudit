# Information Architecture Accessibility Audit

This project is designed to perform an accessibility audit on the information architecture of web applications. It helps identify and analyze potential accessibility issues within the structure and organization of information, ensuring that the content is easily navigable and understandable for all users, including those with disabilities.

## Purpose

The Information Architecture Accessibility Audit tool aims to:

1. Evaluate the overall structure and organization of web content
2. Assess the navigability and findability of information
3. Analyze the labeling and naming conventions used throughout the site
4. Identify potential barriers in the information flow for users with disabilities
5. Provide recommendations for improving the accessibility of the information architecture

## Configuration

The project can be configured using two main property files:

### application.properties

This file contains general application settings. To configure:

1. Locate the `application.properties` file in the `src/main/resources` directory
2. Adjust the following properties as needed:
   - `server.port`: The port on which the application will run
   - `spring.datasource.*`: Database connection settings
   - `logging.level.*`: Logging configuration for different packages

## Deployment to GCP

To deploy this project's Docker container to Google Cloud Platform (GCP), follow these steps:

1. Ensure you have the Google Cloud SDK installed and configured on your local machine.

2. Build the Docker image:
   ```
   docker build -t gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 .
   ```

3. Push the Docker image to Google Container Registry:
   ```
   docker push gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1
   ```

4. Deploy the container to Google Cloud Run:
   ```
   gcloud run deploy ia-accessibility-audit \
     --image gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 \
     --platform managed \
     --region [REGION] \
     --allow-unauthenticated
   ```

   Replace `[PROJECT-ID]` with your GCP project ID and `[REGION]` with your desired region.

5. Once deployed, Google Cloud Run will provide a URL for accessing your application.

Remember to set up any necessary environment variables or secrets in the Google Cloud Run configuration to match your `application.properties` settings.

## Getting Started

[Add instructions for running the project locally, performing an audit, and interpreting results]

## Contributing

[Add guidelines for contributing to the project]

## License

[Specify the license under which this project is released]
