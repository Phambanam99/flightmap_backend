const SockJS = require('sockjs-client');
const Stomp = require('@stomp/stompjs');

console.log('Testing WebSocket connection with session ID fix...');

// Create SockJS connection
const socket = new SockJS('http://localhost:9090/ws');

// Create STOMP client
const stompClient = new Stomp.Client({
  webSocketFactory: () => socket,
  debug: (msg) => {
    if (msg.includes('CONNECT') || msg.includes('session') || msg.includes('Session')) {
      console.log('STOMP Debug:', msg);
    }
  },
  reconnectDelay: 0,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

// Connection handler
stompClient.onConnect = (frame) => {
  console.log('✅ Connected successfully!');
  console.log('📋 Connection frame headers:', JSON.stringify(frame.headers, null, 2));
  
  // Check if session ID is available
  const sessionId = frame.headers?.session || frame.headers?.sessionId || frame.headers?.sessionid;
  if (sessionId) {
    console.log('✅ Session ID found:', sessionId);
  } else {
    console.log('❌ Session ID not found in headers');
  }
  
  // Test subscription to see if it works
  console.log('Testing subscription...');
  const subscription = stompClient.subscribe('/user/queue/subscriptions', (message) => {
    console.log('📨 Received subscription response:', JSON.parse(message.body));
  });
  
  // Send a test subscription request
  stompClient.publish({
    destination: '/app/subscribe-area',
    body: JSON.stringify({
      minLat: 10.5,
      maxLat: 15.3,
      minLon: 106.2,
      maxLon: 110.8
    })
  });
  
  // Disconnect after 5 seconds
  setTimeout(() => {
    console.log('🔌 Disconnecting...');
    stompClient.deactivate();
  }, 5000);
};

// Error handlers
stompClient.onStompError = (frame) => {
  console.error('❌ STOMP Error:', frame);
};

stompClient.onWebSocketClose = (event) => {
  console.log('🔌 WebSocket closed');
  process.exit(0);
};

stompClient.onWebSocketError = (event) => {
  console.error('❌ WebSocket Error:', event);
  process.exit(1);
};

// Connect
console.log('🔄 Connecting to WebSocket...');
stompClient.activate(); 