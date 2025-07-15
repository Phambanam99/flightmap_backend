#!/bin/bash

# Script để khởi chạy Flight & Vessel Simulator Server

echo "🚀 Starting Flight & Vessel Simulator Server..."

# Kiểm tra nếu Node.js đã được cài đặt
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    echo "Visit: https://nodejs.org/"
    exit 1
fi

# Kiểm tra nếu npm đã được cài đặt
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm first."
    exit 1
fi

# Di chuyển đến thư mục simulator
cd "$(dirname "$0")"

echo "📦 Installing dependencies..."
npm install

if [ $? -ne 0 ]; then
    echo "❌ Failed to install dependencies"
    exit 1
fi

echo "🎯 Starting simulator server..."
echo "📍 Server will be available at: http://localhost:3001"
echo "📊 Health check: http://localhost:3001/health"
echo "📈 API status: http://localhost:3001/api/status"
echo ""
echo "🔄 Simulating:"
echo "   ✈️  5 continuous flights"
echo "   🚢 5 vessel journeys"
echo "   📡 Data updates every 3 seconds"
echo ""
echo "🛑 Press Ctrl+C to stop the server"
echo ""

# Chạy server
npm start
