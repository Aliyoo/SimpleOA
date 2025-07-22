package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Holiday;
import com.example.simpleoa.repository.HolidayRepository;
import com.example.simpleoa.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 节假日服务实现类
 */
@Service
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Override
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateDesc();
    }

    @Override
    public List<Holiday> getHolidaysByMonth(int year, int month) {
        return holidayRepository.findByYearAndMonth(year, month);
    }

    @Override
    public List<Holiday> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return holidayRepository.findByDateBetweenOrderByDate(start, end);
    }

    @Override
    public Optional<Holiday> getHolidayById(Long id) {
        return holidayRepository.findById(id);
    }

    @Override
    public Optional<Holiday> getHolidayByDate(Date date) {
        return holidayRepository.findByDate(date);
    }

    @Override
    public Holiday saveHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    @Override
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    @Override
    public List<Holiday> getHolidaysByType(String type) {
        return holidayRepository.findByTypeOrderByDate(type);
    }

    @Override
    public List<Holiday> searchHolidaysByName(String name) {
        return holidayRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        Date javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return holidayRepository.existsByDate(javaDate);
    }

    @Override
    public List<Holiday> getHolidaysByYear(int year) {
        return holidayRepository.findByYear(year);
    }
}
