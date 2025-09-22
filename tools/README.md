E2E test helper

This small Node script runs a quick end-to-end test against a locally running backend on port 8080.

Setup:

```powershell
cd tools
npm install axios tough-cookie axios-cookiejar-support
```

Run:

```powershell
node e2e-test.js
```

It performs:
- login as `alice` (password `alicepass` seeded by `DataSeeder`)
- creates a booking in `Executive Boardroom` (roomId `1`)
- asserts the booking status is `PENDING`
- logs in as `admin` (password `adminpass`) and fetches analytics overview
- prints AI insights (structured or raw)
