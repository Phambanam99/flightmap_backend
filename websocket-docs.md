# WebSocket API Documentation

This project provides WebSocket endpoints for real-time aircraft tracking. The API is documented using AsyncAPI specification.

## Available Endpoints

### Area Subscriptions

- **Subscribe to Area**: `/app/subscribe-area`

  - Subscribe to aircraft updates within a geographic area
  - Payload: `AreaSubscriptionRequest`

- **Unsubscribe from Area**: `/app/unsubscribe-area`
  - Unsubscribe from aircraft updates within a geographic area
  - Payload: `AreaSubscriptionRequest`

### Aircraft Subscriptions

- **Subscribe to Aircraft**: `/app/subscribe-aircraft`

  - Subscribe to updates for a specific aircraft
  - Payload: `AircraftSubscriptionRequest`

- **Unsubscribe from Aircraft**: `/app/unsubscribe-aircraft`
  - Unsubscribe from updates for a specific aircraft
  - Payload: `AircraftSubscriptionRequest`

## Message Formats

### AreaSubscriptionRequest

```json
{
  "minLat": 10.5,
  "maxLat": 15.3,
  "minLon": 106.2,
  "maxLon": 110.8
}
```

### AircraftSubscriptionRequest

```json
{
  "hexIdent": "AB1234"
}
```

### Response Format

Responses will be sent to `/user/queue/subscriptions` with the following format:

```json
{
  "type": "area", // or "aircraft"
  "status": "subscribed", // or "unsubscribed"
  "key": "area_10.5_15.3_106.2_110.8" // or hexIdent for aircraft
}
```

## Viewing the Documentation

1. AsyncAPI UI: Navigate to `/asyncapi-ui` in your browser
2. Raw AsyncAPI Specification: `/asyncapi`
3. The AsyncAPI documentation is also integrated with Swagger UI at `/swagger-ui.html`

## Client-Side Connection Example

```javascript
// Using STOMP over WebSocket
const client = Stomp.over(new WebSocket("ws://localhost:9090/ws"));
client.connect({}, (frame) => {
  console.log("Connected: " + frame);

  // Subscribe to user-specific responses
  client.subscribe("/user/queue/subscriptions", (response) => {
    const data = JSON.parse(response.body);
    console.log("Subscription status:", data);
  });

  // Subscribe to an area
  client.send(
    "/app/subscribe-area",
    {},
    JSON.stringify({
      minLat: 10.5,
      maxLat: 15.3,
      minLon: 106.2,
      maxLon: 110.8,
    })
  );

  // Subscribe to a specific aircraft
  client.send(
    "/app/subscribe-aircraft",
    {},
    JSON.stringify({
      hexIdent: "AB1234",
    })
  );
});
```
