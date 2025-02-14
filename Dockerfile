FROM adoptopenjdk/openjdk14

COPY target/informationArchitectureAudit-0.0.1.jar look-see.jar
COPY GCP-MyFirstProject-1c31159db52c.json GCP-MyFirstProject-1c31159db52c.json
COPY gmail_credentials.json /etc/creds/gmail_credentials.json
EXPOSE 443
EXPOSE 80
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms512M", "-ea","-jar", "look-see.jar"]
