package com.rmit.sept.mon15307.backend.web;

import com.rmit.sept.mon15307.backend.exceptions.NotFoundException;
import com.rmit.sept.mon15307.backend.model.*;
import com.rmit.sept.mon15307.backend.model.enumeration.BookingStatus;
import com.rmit.sept.mon15307.backend.payload.BookingPatch;
import com.rmit.sept.mon15307.backend.payload.BookingRequest;
import com.rmit.sept.mon15307.backend.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @PostMapping("")
    public ResponseEntity<?> createNewBooking(
        @Valid
        @RequestBody
            BookingRequest bookingRequest,
        BindingResult result,
        @AuthenticationPrincipal
            UserAccount user
    ) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        Map<String, Map<String, String>> errorResponse = new HashMap<>();
        Map<String, String> errorMessage = new HashMap<>();

        // entities must exist
        UserAccount customer;
        Employee employee;
        Product product;
        try {
            customer = userService.findByUserId(bookingRequest.getCustomerId());
            employee = employeeService.findByEmployeeId(bookingRequest.getEmployeeId());
            product = productService.findByProductId(bookingRequest.getProductId());
        } catch (NotFoundException e) {
            errorMessage.put("message", e.getMessage());
            errorResponse.put("error", errorMessage);
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        // customer must be the same as the current user, unless current user is an
        // admin
        if (!user.getUserId().equals(customer.getUserId()) && !user.getAdmin()) {
            errorMessage.put("message", "Action not permitted for this user");
            errorResponse.put("error", errorMessage);
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        // employee must be scheduled for the selected date
        Schedule schedule =
            scheduleService.findByEmployeeAndDate(employee, bookingRequest.getDate());
        boolean employeeNotScheduled = schedule == null;

        // date must be in next 14 days (including today)
        LocalDate future = LocalDate.now().plusDays(14);
        boolean invalidDate = !bookingRequest.getDate().isBefore(future);

        // time must be one of the permitted options
        boolean unsupportedTime = !Booking.permittedTimes.contains(bookingRequest.getTimeSlot());

        // booking must not have already started
        LocalDateTime startTime = LocalDateTime.of(bookingRequest.getDate(),
                                                   LocalTime.parse(bookingRequest.getTimeSlot())
        );
        boolean invalidAppointmentStart = !startTime.isAfter(LocalDateTime.now());

        // employee must not have any preexisting overlapping bookings
        LocalDateTime endTime = startTime.plusMinutes(product.getDuration());
        boolean bookingConflict =
            bookingService.conflictsWithExisting(schedule, employee, startTime, endTime);

        if (employeeNotScheduled ||
            invalidDate ||
            unsupportedTime ||
            invalidAppointmentStart ||
            bookingConflict) {

            errorMessage.put("message", "Appointment time not available");
            errorResponse.put("error", errorMessage);
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        // now we can make the booking (and hopefully no new conflict has arisen in the
        // meantime)
        // TODO: handle race conditions (create, check conflicts, maybe rollback and
        // error)

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.pending);
        booking.setCustomer(customer);
        booking.setEmployee(employee);
        booking.setProduct(product);
        booking.setSchedule(schedule);
        booking.setTime(bookingRequest.getTimeSlot());
        bookingService.saveOrUpdateBooking(booking);

        Map<String, String> response = new HashMap<>();
        response.put("booking_id", booking.getBookingId().toString());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(
        @PathVariable
            Long bookingId
    ) {
        Booking booking = bookingService.findByBookingId(bookingId);
        return new ResponseEntity<>(booking, HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> listUserBookings(
        @RequestParam
            String status,
        @AuthenticationPrincipal
            UserAccount user, BindingResult result
    ) {

        Map<String, Map<String, String>> errorResponse = new HashMap<>();
        Map<String, String> errorMessage = new HashMap<>();

        BookingStatus bookingStatus;
        try {
            bookingStatus = BookingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            errorMessage.put("message", "invalid status: " + status);
            errorResponse.put("error", errorMessage);
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
        Iterable<Booking> bookings;
        if (user.getAdmin() || user.getWorker()) {
            // current authenticated user is permitted to retrieve bookings for all customers
            bookings = bookingService.findAllBookingsByStatus(bookingStatus);
        } else {
            // only bookings for current user
            bookings = bookingService.findUserBookingsByStatus(user, bookingStatus);
        }

        Map<String, Iterable<Booking>> response = new HashMap<>();
        response.put("bookings", bookings);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<?> editBooking(
        @PathVariable
        @Min(1) @NotNull Long bookingId,
        @Valid
        @RequestBody
            BookingPatch bookingPatch,
        BindingResult result,
        @AuthenticationPrincipal
            UserAccount user
    ) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null) return errorMap;

        Booking booking = bookingService.findByBookingId(bookingId);
        Booking updatedBooking = bookingService.updateBooking(booking, bookingPatch, user);
        return new ResponseEntity<>(updatedBooking, HttpStatus.OK);
    }
}