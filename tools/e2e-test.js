// E2E test script for smart-meeting-room-system
// Usage:
// 1) cd tools
// 2) npm install axios tough-cookie axios-cookiejar-support
// 3) node e2e-test.js

const axios = require('axios');
const { CookieJar } = require('tough-cookie');

const BASE = process.env.API_BASE || 'http://localhost:8080';

function createClient() {
  const jar = new CookieJar();
  const client = axios.create({ baseURL: BASE, validateStatus: null });
  client.jar = jar;
  return client;
}

async function doPostWithCookies(client, path, body) {
  const cookieHeader = await client.jar.getCookieString(BASE);
  const resp = await client.post(path, body, { headers: cookieHeader ? { Cookie: cookieHeader } : {} });
  const setCookie = resp.headers && (resp.headers['set-cookie'] || resp.headers['Set-Cookie']);
  if (setCookie) {
    // set-cookie can be array
    const arr = Array.isArray(setCookie) ? setCookie : [setCookie];
    for (const sc of arr) {
      try { await client.jar.setCookie(sc, BASE); } catch (e) { /* swallow */ }
    }
  }
  return resp;
}

async function doGetWithCookies(client, path) {
  const cookieHeader = await client.jar.getCookieString(BASE);
  const resp = await client.get(path, { headers: cookieHeader ? { Cookie: cookieHeader } : {} });
  return resp;
}

async function loginAndGetClient(username, password) {
  const client = createClient();
  const resp = await doPostWithCookies(client, '/api/auth/login', { username, password });
  if (resp.status >= 400) throw new Error(`Login failed: ${resp.status} ${JSON.stringify(resp.data)}`);
  console.log(`Logged in as ${username}:`, resp.data);
  return client;
}

function isoLocalFromParts(dateStr, startTime) {
  // dateStr like 2025-10-01 and startTime like 09:00
  return `${dateStr}T${startTime}:00`;
}

async function main() {
  try {
    // 1) Login as Alice (regular user)
    const alice = await loginAndGetClient('alice', 'alicepass');

    // 2) Create a booking in Executive Boardroom (roomId:1 from DataSeeder)
    const date = new Date();
    // schedule for tomorrow at 10:00-11:00 local
    const tomorrow = new Date(date.getTime() + 24*60*60*1000);
    const yyyy = tomorrow.getFullYear();
    const mm = String(tomorrow.getMonth()+1).padStart(2,'0');
    const dd = String(tomorrow.getDate()).padStart(2,'0');
    const dateStr = `${yyyy}-${mm}-${dd}`;
    const startIso = isoLocalFromParts(dateStr, '10:00');
    const endIso = isoLocalFromParts(dateStr, '11:00');

    const bookingPayload = {
      roomId: 1,
      startTime: startIso,
      endTime: endIso,
      purpose: 'Quarterly executive strategy review',
      attendeesCount: 5,
      priority: 3,
      notes: 'E2E test booking'
    };

  console.log('Creating booking for Executive Boardroom (roomId=1)...');
  const createResp = await doPostWithCookies(alice, '/api/bookings', bookingPayload);
    console.log('Create response:', createResp.data);
    const created = createResp.data;

    if (!created || !created.id) {
      console.error('Booking creation failed or returned no id');
      process.exit(2);
    }

    // 3) As the same user, fetch bookings and check the created booking status is PENDING
  const allBookings = await doGetWithCookies(alice, '/api/bookings');
  const userBookings = allBookings.data || allBookings; // axios returns in .data when using default axios
    const found = (Array.isArray(userBookings) ? userBookings : []).find(b => b.id === created.id || (b.roomId === 1 && b.startTime && b.startTime.includes(`${dateStr}T10:00`)));
    if (!found) {
      console.error('Created booking not found in user bookings');
      console.log('User bookings:', userBookings.slice(0,10));
      process.exit(3);
    }

    console.log('Found booking:', found);
    if (!found.status || !found.status.toUpperCase().startsWith('PENDING')) {
      console.error('Booking status is not PENDING as expected. Status:', found.status);
      process.exit(4);
    }
    console.log('Booking status correctly PENDING for Executive room.');

    // 4) Login as admin and fetch analytics
    const admin = await loginAndGetClient('admin', 'adminpass');
  const analyticsResp = await doGetWithCookies(admin, '/api/admin/analytics/overview');
  const analytics = analyticsResp.data || analyticsResp;
    console.log('Admin analytics overview retrieved. Top-level keys:', Object.keys(analytics));
    if (analytics.aiInsights) {
      console.log('Structured AI Insights:', JSON.stringify(analytics.aiInsights, null, 2));
    } else if (analytics.aiInsightsRaw) {
      console.log('AI Insights (raw):', analytics.aiInsightsRaw);
    } else {
      console.log('No AI insights present in analytics response.');
    }

    console.log('E2E test completed successfully.');
    process.exit(0);
  } catch (err) {
    console.error('E2E test failed:', err && err.response ? (err.response.data || err.response.statusText) : err.message || err);
    process.exit(20);
  }
}

main();
