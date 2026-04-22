package com.vlast.scheduler.schedule.controller;

import com.vlast.scheduler.schedule.dto.*;
import com.vlast.scheduler.schedule.entity.EditScope;
import com.vlast.scheduler.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ===== QUERY =====

    @GetMapping
    public List<ScheduleResponse> getMonthlySchedules(
            @RequestParam int year, @RequestParam int month) {
        return scheduleService.getMonthlySchedules(year, month);
    }

    @GetMapping("/{id}")
    public ScheduleResponse getDetail(@PathVariable Long id) {
        return scheduleService.getScheduleDetail(id);
    }

    @GetMapping("/recurring/{groupId}/{date}")
    public ScheduleResponse getRecurringInstanceDetail(
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.getRecurringInstanceDetail(groupId, date);
    }

    // ===== CREATE =====

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@Valid @RequestBody ScheduleCreateRequest request) {
        return scheduleService.createSchedule(request);
    }

    // ===== UPDATE =====

    /** Update a one-time (non-recurring) schedule */
    @PutMapping("/{id}")
    public ScheduleResponse updateOneTime(
            @PathVariable Long id,
            @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.updateOneTimeSchedule(id, request);
    }

    /** Update a recurring schedule instance with scope */
    @PutMapping("/recurring/{groupId}/{date}")
    public ScheduleResponse updateRecurring(
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "THIS") EditScope scope,
            @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.updateRecurringInstance(groupId, date, scope, request);
    }

    // ===== DELETE =====

    /** Delete a one-time schedule */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOneTime(@PathVariable Long id) {
        scheduleService.deleteOneTimeSchedule(id);
    }

    /** Delete a recurring schedule instance with scope */
    @DeleteMapping("/recurring/{groupId}/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurring(
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "THIS") EditScope scope) {
        scheduleService.deleteRecurringInstance(groupId, date, scope);
    }

    // ===== CONVERT =====

    /** Convert a one-time schedule to recurring */
    @PostMapping("/{id}/convert-to-recurring")
    public ScheduleResponse convertToRecurring(
            @PathVariable Long id,
            @Valid @RequestBody ConvertToRecurringRequest request) {
        return scheduleService.convertToRecurring(id, request);
    }
}
