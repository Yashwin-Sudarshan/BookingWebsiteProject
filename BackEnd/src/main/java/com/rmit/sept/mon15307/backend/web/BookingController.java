package com.rmit.sept.mon15307.backend.web;

import com.rmit.sept.mon15307.backend.exceptions.BookingException;
import com.rmit.sept.mon15307.backend.exceptions.UserNotAuthorisedException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

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
    private ProductService productService;
    @Autowired
    private MapValidationErrorService mapValidationErrorService;

    @PostMapping("")
    public ResponseEntity<?> createNewBooking(@Valid @RequestBody BookingRequest bookingRequest, BindingResult result,
            @AuthenticationPrincipal UserAccount user) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null)
            return errorMap;

        // entities must exist
        UserAccount customer = userService.findByUserId(bookingRequest.getCustomerId());
        Employee employee = employeeService.findByEmployeeId(bookingRequest.getEmployeeId());
        Product product = productService.findByProductId(bookingRequest.getProductId());

        // customer must be the same as the current user, unless current user is an
        // admin
        if (!user.getUserId().equals(customer.getUserId()) && !user.getAdmin()) {
            throw new UserNotAuthorisedException("User not authorised to create booking for this " + "customer");
        }

        Booking booking = bookingService.createBooking(customer, employee, product, bookingRequest);

        Map<String, String> response = new HashMap<>();
        response.put("booking_id", booking.getBookingId().toString());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId) {
        Booking booking = bookingService.findByBookingId(bookingId);
        return new ResponseEntity<>(booking, HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> listUserBookings(@RequestParam("status") String statusQuery,
            @AuthenticationPrincipal UserAccount user) {
        Collection<BookingStatus> bookingStatuses;
        try {
            bookingStatuses = Arrays.stream(statusQuery.split(",")).map(s -> BookingStatus.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BookingException("Invalid booking status param: " + statusQuery);
        }

        Iterable<Booking> bookings;
        if (user.getAdmin()) {
            // user is permitted to retrieve bookings for all customers
            bookings = bookingService.findAllBookingsByStatuses(bookingStatuses);
        } else if (user.getWorker()) {
            // user is permitted to retrieve bookings for all customers to which they are
            // assigned
            bookings = bookingService.findWorkerBookingsByStatuses(user, bookingStatuses);
        } else {
            // user is permitted to retrieve only their own bookings
            bookings = bookingService.findUserBookingsByStatuses(user, bookingStatuses);
        }

        return new ResponseEntity<>(new BookingsList(bookings), HttpStatus.OK);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<?> editBooking(@PathVariable @Min(1) @NotNull Long bookingId,
            @Valid @RequestBody BookingPatch bookingPatch, BindingResult result,
            @AuthenticationPrincipal UserAccount user) {
        ResponseEntity<?> errorMap = mapValidationErrorService.MapValidationService(result);
        if (errorMap != null)
            return errorMap;

        Booking booking = bookingService.findByBookingId(bookingId);
        Booking updatedBooking = bookingService.updateBooking(booking, bookingPatch, user);
        return new ResponseEntity<>(updatedBooking, HttpStatus.OK);
    }
}