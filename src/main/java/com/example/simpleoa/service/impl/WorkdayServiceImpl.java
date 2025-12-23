package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Workday;
import com.example.simpleoa.repository.HolidayRepository;
import com.example.simpleoa.repository.WorkdayRepository;
import com.example.simpleoa.service.WorkdayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工作日服务实现类
 */
@Service
public class WorkdayServiceImpl implements WorkdayService {

    @Autowired
    private WorkdayRepository workdayRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Override
    public List<Workday> getAllWorkdays() {
        return workdayRepository.findAllByOrderByDateDesc();
    }

    @Override
    public List<Workday> getWorkdaysByMonth(int year, int month) {
        return workdayRepository.findByYearAndMonth(year, month);
    }

    @Override
    public List<Workday> getWorkdaysByYear(int year) {
        return workdayRepository.findByYear(year);
    }

    @Override
    public List<Workday> getWorkdaysByDateRange(LocalDate startDate, LocalDate endDate) {
        return workdayRepository.findByDateBetweenOrderByDate(startDate, endDate);
    }

    @Override
    public Optional<Workday> getWorkdayById(Long id) {
        return workdayRepository.findById(id);
    }

    @Override
    public Optional<Workday> getWorkdayByDate(LocalDate date) {
        return workdayRepository.findByDate(date);
    }

    @Override
    public Workday saveWorkday(Workday workday) {
        return workdayRepository.save(workday);
    }

    @Override
    public void deleteWorkday(Long id) {
        workdayRepository.deleteById(id);
    }

    @Override
    public List<Workday> generateWorkdaysForMonth(int year, int month, String description) {
        List<LocalDate> workdayDates = generateWorkdayDatesForMonth(year, month);
        List<Workday> workdays = workdayDates.stream().map(date -> {
            Workday workday = new Workday();
            workday.setDate(date);
            workday.setDescription(description);
            workday.setWorkType("NORMAL");
            return workday;
        }).collect(Collectors.toList());
        return workdayRepository.saveAll(workdays);
    }

    @Override
    public List<Workday> generateWorkdaysForRange(int startYear, int startMonth, int endYear, int endMonth, String description) {
        List<Workday> workdays = Stream.iterate(LocalDate.of(startYear, startMonth, 1), date -> date.plusMonths(1))
                .limit(endYear * 12 + endMonth - (startYear * 12 + startMonth) + 1)
                .flatMap(date -> generateWorkdayDatesForMonth(date.getYear(), date.getMonthValue()).stream())
                .map(date -> {
                    Workday workday = new Workday();
                    workday.setDate(date);
                    workday.setDescription(description);
                    workday.setWorkType("NORMAL");
                    return workday;
                })
                .collect(Collectors.toList());
        return workdayRepository.saveAll(workdays);
    }

    @Override
    public boolean workdayExistsForMonth(int year, int month) {
        return workdayRepository.existsByYearAndMonth(year, month);
    }

    @Override
    public boolean isWorkday(LocalDate date) {
        return !holidayRepository.existsByDate(date) &&
               (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY ||
                workdayRepository.existsByDate(date));
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByDate(date);
    }

    @Override
    public List<LocalDate> getWorkdayDatesForMonth(int year, int month) {
        return workdayRepository.findByYearAndMonth(year, month)
                .stream()
                .map(Workday::getDate)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocalDate> generateWorkdayDatesForMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return start.datesUntil(end.plusDays(1))
                .filter(this::isWorkday)
                .collect(Collectors.toList());
    }

    @Override
    public LocalDate adjustToWorkday(LocalDate date) {
        while (!isWorkday(date)) {
            date = date.minusDays(1);
        }
        return date;
    }

    @Override
    public LocalDate getLastWorkdayOfMonth(int year, int month) {
        LocalDate lastDay = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());
        return adjustToWorkday(lastDay);
    }

    @Override
    public List<Workday> getWorkdaysByType(String workType) {
        return workdayRepository.findByWorkTypeOrderByDate(workType);
    }
}
