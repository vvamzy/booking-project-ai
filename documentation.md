# Smart Meeting Room System Documentation

## System Overview
The Smart Meeting Room System is a full-stack application that manages meeting room bookings with AI-powered approval workflow.

## Backend Documentation

### Database Schema
```sql
// From V1__create_tables.sql
```

### Controllers

#### Booking Controller
Endpoint: `/api/bookings`

Methods:
1. **Create Booking**
   - Endpoint: POST `/api/bookings`
   - Description: Creates a new booking request
   - Test Cases:
     - Successful booking creation
     - Booking with invalid time range
     - Booking with non-existent room
     - Booking with time conflict

2. **Get All Bookings**
   - Endpoint: GET `/api/bookings`
   - Description: Retrieves all bookings
   - Test Cases:
     - Get all bookings
     - Filter by date range
     - Filter by room

3. **Get Booking by ID**
   - Endpoint: GET `/api/bookings/{id}`
   - Description: Retrieves a specific booking
   - Test Cases:
     - Get existing booking
     - Get non-existent booking

4. **Update Booking**
   - Endpoint: PUT `/api/bookings/{id}`
   - Description: Updates an existing booking
   - Test Cases:
     - Update existing booking
     - Update with invalid data
     - Update non-existent booking

5. **Delete Booking**
   - Endpoint: DELETE `/api/bookings/{id}`
   - Description: Cancels a booking
   - Test Cases:
     - Delete existing booking
     - Delete non-existent booking

#### Room Controller
Endpoint: `/api/rooms`

Methods:
1. **Get All Rooms**
   - Endpoint: GET `/api/rooms`
   - Description: Lists all available rooms
   - Test Cases:
     - Get all rooms
     - Filter by capacity
     - Filter by amenities

2. **Get Room by ID**
   - Endpoint: GET `/api/rooms/{id}`
   - Description: Gets details of a specific room
   - Test Cases:
     - Get existing room
     - Get non-existent room

#### Approval Controller
Endpoint: `/api/approvals`

Methods:
1. **Get Pending Approvals**
   - Endpoint: GET `/api/approvals/pending`
   - Description: Lists all pending booking approvals
   - Test Cases:
     - Get pending approvals
     - Filter by date

2. **Approve Booking**
   - Endpoint: POST `/api/approvals/{id}/approve`
   - Description: Approves a booking request
   - Test Cases:
     - Approve pending booking
     - Approve already approved booking
     - Approve rejected booking

3. **Reject Booking**
   - Endpoint: POST `/api/approvals/{id}/reject`
   - Description: Rejects a booking request
   - Test Cases:
     - Reject pending booking
     - Reject already approved booking
     - Reject already rejected booking

### Services

#### Booking Service
Methods:
1. `createBooking(BookingRequest request)`
   - Validates booking request
   - Checks room availability
   - Creates new booking entry
   - Triggers approval workflow

2. `getBookings(SearchCriteria criteria)`
   - Retrieves bookings based on search criteria
   - Handles pagination and sorting

3. `updateBooking(Long id, BookingRequest request)`
   - Validates update request
   - Updates booking if allowed
   - Handles approval status changes

4. `deleteBooking(Long id)`
   - Cancels booking if allowed
   - Updates room availability

#### AI Decision Service
Methods:
1. `evaluateBooking(Booking booking)`
   - Analyzes booking request
   - Considers room utilization
   - Applies booking policies
   - Provides approval recommendation

#### Approval Service
Methods:
1. `processApproval(Long bookingId, ApprovalDecision decision)`
   - Validates approval request
   - Updates booking status
   - Notifies relevant parties

## Frontend Documentation

### Components

#### BookingForm Component
Purpose: Handles new booking creation

Props:
- `roomId`: ID of selected room
- `onSubmit`: Callback for form submission

State:
- Form field values
- Validation errors
- Submission status

Test Cases:
1. Form submission with valid data
2. Form validation errors
3. Server error handling
4. Date/time selection validation

#### RoomCard Component
Purpose: Displays room information

Props:
- `roomName`: Name of the room
- `capacity`: Room capacity
- `amenities`: Available amenities
- `onBook`: Booking callback

Test Cases:
1. Render with all props
2. Click booking button
3. Display amenities
4. Handle missing props

#### ApprovalQueue Component
Purpose: Manages booking approvals

Features:
1. Lists pending approvals
2. Shows approval details
3. Handles approve/reject actions
4. Displays AI recommendations

Test Cases:
1. Load pending approvals
2. Approve booking
3. Reject booking
4. Handle server errors
5. View approval logs

### Pages

#### Dashboard Page
Features:
1. Room listing
2. Approval queue
3. Booking statistics

Flow:
1. Load available rooms
2. Display room cards
3. Show pending approvals
4. Update on actions

#### BookingPage
Features:
1. Room selection
2. Booking form
3. Confirmation flow

Flow:
1. Select room
2. Fill booking details
3. Submit request
4. Show confirmation

#### AdminPanel
Features:
1. Approval management
2. Room management
3. User management

Flow:
1. View pending approvals
2. Process approvals
3. Manage rooms
4. Handle user permissions

## Testing Flows

### Main User Flows

1. **Room Booking Flow**
   - Browse available rooms
   - Select room
   - Fill booking details
   - Submit booking
   - Receive confirmation
   - Check booking status

2. **Approval Flow**
   - Receive booking request
   - Review AI recommendation
   - Check room availability
   - Make approval decision
   - Send notification

3. **Room Management Flow**
   - Add new room
   - Update room details
   - Check room availability
   - Manage room status

4. **User Management Flow**
   - Register user
   - Assign roles
   - Manage permissions
   - Handle authentication

### Error Flows

1. **Booking Conflicts**
   - Double booking attempt
   - Invalid time selection
   - Room unavailability

2. **Approval Errors**
   - Invalid approval attempt
   - Missing permissions
   - System unavailability

3. **Authentication Errors**
   - Invalid credentials
   - Session expiration
   - Permission denied

## Integration Tests

1. **End-to-End Booking**
   - Complete booking flow
   - Approval process
   - Notification delivery

2. **User Authentication**
   - Login flow
   - Session management
   - Permission checks

3. **Room Availability**
   - Real-time updates
   - Conflict handling
   - Calendar synchronization

## Performance Considerations

1. **Backend**
   - Database query optimization
   - Caching strategy
   - API response times

2. **Frontend**
   - Component loading
   - State management
   - UI responsiveness

## Security Testing

1. **Authentication**
   - Login security
   - Session management
   - Token handling

2. **Authorization**
   - Role-based access
   - API endpoints
   - Data privacy

3. **Data Protection**
   - Input validation
   - SQL injection prevention
   - XSS prevention
