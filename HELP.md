# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.phamnam.tracking-aircraft-aircraft' is invalid and this project uses 'com.phamnam.tracking_vessel_flight' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.4/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.4/gradle-plugin/packaging-oci-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.4.4/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [WebSocket](https://docs.spring.io/spring-boot/3.4.4/reference/messaging/websockets.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.4/reference/web/servlet.html)
* [Spring Data Reactive Redis](https://docs.spring.io/spring-boot/3.4.4/reference/data/nosql.html#data.nosql.redis)
* [Spring Security](https://docs.spring.io/spring-boot/3.4.4/reference/web/spring-security.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Using WebSocket to build an interactive web application](https://spring.io/guides/gs/messaging-stomp-websocket/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

# API WebSocket Documentation

## General Information

- **WebSocket Endpoint:** `/ws`  
  Clients must connect to this endpoint using a STOMP client with SockJS support.
- **Allowed Origins:** All origins are allowed (using an allowed origin pattern of `*`).

## Message Broker Configuration

- **Simple Broker Destinations:**  
  - `/topic` – broadcast messages to subscribers  
  - `/queue` – send point-to-point messages

- **Application Destination Prefix:**  
  - `/app` – all messages sent from the client should be prefixed with this

- **User Destination Prefix:**  
  - `/user` – targeted messages to a specific client (used with `convertAndSendToUser`)

## Endpoints

### Subscribe to Area

- **Destination:** `/app/subscribe-area`
- **Purpose:**  
  Register a client to receive aircraft updates within a geographic area.
- **Payload:**  
  An object matching the `AreaSubscriptionRequest` model containing:
  - `minLat` (double)
  - `maxLat` (double)
  - `minLon` (double)
  - `maxLon` (double)
- **Logs:**  
  Logs the client's session ID and requested coordinates.
- **Server Behavior:**  
  Adds subscriptions in Redis and sends a confirmation message to `/queue/subscriptions`.

---

### Unsubscribe from Area

- **Destination:** `/app/unsubscribe-area`
- **Purpose:**  
  Remove a client's subscription from a specified geographic area.
- **Payload:**  
  An object matching the `AreaSubscriptionRequest` model.
- **Logs:**  
  Logs the client session and coordinates.
- **Server Behavior:**  
  Removes subscriptions from Redis and sends a confirmation message to `/queue/subscriptions`.

---

### Subscribe to Aircraft

- **Destination:** `/app/subscribe-aircraft`
- **Purpose:**  
  Register a client to receive updates for a specific aircraft.
- **Payload:**  
  An object matching the `AircraftSubscriptionRequest` model with:  
  - `hexIdent` (String)
- **Logs:**  
  Logs the client session ID and the aircraft identifier.
- **Server Behavior:**  
  Adds the aircraft subscription in Redis and sends a confirmation message to `/queue/subscriptions`.

---

### Unsubscribe from Aircraft

- **Destination:** `/app/unsubscribe-aircraft`
- **Purpose:**  
  Remove a client's subscription for a specific aircraft.
- **Payload:**  
  An object matching the `AircraftSubscriptionRequest` model with:
  - `hexIdent` (String)
- **Logs:**  
  Logs the client session ID and the aircraft identifier.
- **Server Behavior:**  
  Removes the aircraft subscription from Redis and sends a confirmation message to `/queue/subscriptions`.

## Payload Models

### AreaSubscriptionRequest

```java
package com.phamnam.tracking_vessel_flight.dto.request;

public class AreaSubscriptionRequest {
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    // Getters and setters omitted for brevity
}