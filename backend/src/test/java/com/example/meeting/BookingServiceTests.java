package com.example.meeting;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.meeting.model.Booking;
import com.example.meeting.repository.BookingRepository;
import com.example.meeting.repository.ApprovalLogRepository;
import com.example.meeting.repository.BookingHistoryRepository;
import com.example.meeting.service.BookingService;
import com.example.meeting.service.AiDecisionService;

import java.time.LocalDateTime;

class BookingServiceTests {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AiDecisionService aiDecisionService;

    @Mock
    private ApprovalLogRepository approvalLogRepository;

    @Mock
    private BookingHistoryRepository bookingHistoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    // Provide a default decision from the AI service to avoid nulls during tests
    when(aiDecisionService.decide(any(Booking.class), anyList()))
        .thenReturn(new AiDecisionService.Decision(AiDecisionService.Action.AUTO_APPROVE, 0.9,
            java.util.Collections.singletonList("test-auto-approve")));
    }

    @Test
    void testCreateBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setRoomId(101L);
        booking.setStartTime(LocalDateTime.now().plusHours(1)); // Start 1 hour from now
        booking.setEndTime(LocalDateTime.now().plusHours(2));   // End 2 hours from now
        booking.setPurpose("Test meeting for unit tests");
        booking.setUserId(1L);
        booking.setAttendeesCount(5);
        booking.setStatus("PENDING");

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking createdBooking = bookingService.createBooking(booking);

        assertNotNull(createdBooking);
        assertEquals(1L, createdBooking.getId());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testGetBookingById() {
        Booking booking = new Booking();
        booking.setId(1L);
        
    when(bookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

    java.util.Optional<Booking> foundBooking = bookingService.getBookingById(1L);

    assertTrue(foundBooking.isPresent());
    assertEquals(1L, foundBooking.get().getId());
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void testDeleteBooking() {
        Long bookingId = 1L;

        bookingService.deleteBooking(bookingId);

        verify(bookingRepository, times(1)).deleteById(bookingId);
    }
}