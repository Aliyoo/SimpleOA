package com.example.simpleoa.service;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress; // If merging cells, not used in this specific impl but good to have
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


// Assume WorkTimeService and ProjectService will be created or exist
// For now, let's add placeholder imports if they are in the same package.
// If they are in different packages, these imports would need to be adjusted.
// import com.example.simpleoa.service.WorkTimeService;
// import com.example.simpleoa.service.ProjectService;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    // Not using these services in this specific Excel generation logic as data is passed in.
    // Keep them if other methods in ExportService might need them.
    private final WorkTimeService workTimeService;
    private final ProjectService projectService;

    @Autowired
    public ExportService(WorkTimeService workTimeService, ProjectService projectService) {
        this.workTimeService = workTimeService;
        this.projectService = projectService;
    }

    private List<String> getDatesBetween(String startDateStr, String endDateStr) {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        try {
            LocalDate startDate = LocalDate.parse(startDateStr, formatter);
            LocalDate endDate = LocalDate.parse(endDateStr, formatter);
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dates.add(currentDate.format(formatter));
                currentDate = currentDate.plusDays(1);
            }
        } catch (DateTimeParseException e) {
            logger.error("Error parsing dates in getDatesBetween: startDateStr={}, endDateStr={}", startDateStr, endDateStr, e);
            // Return empty list or throw custom exception
            return Collections.emptyList();
        }
        return dates;
    }

    @SuppressWarnings("unchecked") // For casting hoursMap.get(dateStr) and member.get("hours")
    public ByteArrayInputStream generateBatchFillExcel(String startDateStr, String endDateStr, List<Map<String, Object>> projectsData) throws IOException {
        logger.info("Generating batch fill Excel for {} to {} with {} projects", startDateStr, endDateStr, projectsData.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Batch Fill Export");

            // Create Font for Header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Create CellStyle for Header
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Get date range
            List<String> dateColumns = getDatesBetween(startDateStr, endDateStr);
            if (dateColumns.isEmpty() && (startDateStr != null && endDateStr != null)) {
                // This case means date parsing failed in getDatesBetween
                logger.warn("Date range could not be generated for startDateStr: {}, endDateStr: {}. Excel will have no date columns.", startDateStr, endDateStr);
                // Potentially throw an error or handle as appropriate if dates are critical
            }


            // Create Header Row
            Row headerRow = sheet.createRow(0);
            int headerCellIdx = 0;
            Cell cell = headerRow.createCell(headerCellIdx++);
            cell.setCellValue("Project Name");
            cell.setCellStyle(headerCellStyle);

            cell = headerRow.createCell(headerCellIdx++);
            cell.setCellValue("Member Username");
            cell.setCellStyle(headerCellStyle);

            cell = headerRow.createCell(headerCellIdx++);
            cell.setCellValue("Member Real Name");
            cell.setCellStyle(headerCellStyle);

            for (String date : dateColumns) {
                cell = headerRow.createCell(headerCellIdx++);
                cell.setCellValue(date);
                cell.setCellStyle(headerCellStyle);
            }

            cell = headerRow.createCell(headerCellIdx++);
            cell.setCellValue("Total Hours");
            cell.setCellStyle(headerCellStyle);

            // Populate Data Rows
            int rowIdx = 1;
            if (projectsData != null) {
                for (Map<String, Object> project : projectsData) {
                    String projectName = project.getOrDefault("projectName", "N/A").toString();
                    List<Map<String, Object>> members = (List<Map<String, Object>>) project.get("members");

                    if (members != null) {
                        for (Map<String, Object> member : members) {
                            Row dataRow = sheet.createRow(rowIdx++);
                            int cellIdx = 0;
                            dataRow.createCell(cellIdx++).setCellValue(projectName);
                            dataRow.createCell(cellIdx++).setCellValue(member.getOrDefault("username", "N/A").toString());
                            dataRow.createCell(cellIdx++).setCellValue(member.getOrDefault("realName", "N/A").toString());

                            Map<String, Object> hoursMap = (Map<String, Object>) member.get("hours");
                            double totalHours = 0;

                            for (String dateStr : dateColumns) {
                                Object hoursObj = (hoursMap != null) ? hoursMap.get(dateStr) : null;
                                double hoursForDate = 0;
                                if (hoursObj instanceof Number) {
                                    hoursForDate = ((Number) hoursObj).doubleValue();
                                } else if (hoursObj instanceof String) {
                                    try {
                                        hoursForDate = Double.parseDouble((String) hoursObj);
                                    } catch (NumberFormatException e) {
                                        logger.warn("Could not parse hours string '{}' for date {} and member {}", hoursObj, dateStr, member.get("username"));
                                        hoursForDate = 0; // Default to 0 if parsing fails
                                    }
                                }
                                dataRow.createCell(cellIdx++).setCellValue(hoursForDate);
                                totalHours += hoursForDate;
                            }
                            dataRow.createCell(cellIdx++).setCellValue(totalHours);
                        }
                    }
                }
            }

            // Auto-size columns (do this after all data is populated)
            // Header has 3 fixed columns + number of date columns + 1 total hours column
            int numberOfColumns = 3 + dateColumns.size() + 1;
            for (int i = 0; i < numberOfColumns; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) { // Catch broader exceptions during Excel generation
            logger.error("Error generating batch fill Excel: {}", e.getMessage(), e);
            // Optionally, rethrow as IOException or a custom exception
            // For now, let's rethrow as IOException to match method signature
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to generate batch fill Excel: " + e.getMessage(), e);
        }
    }

    public ByteArrayInputStream generateStatisticalReportExcel(LocalDate startDate, LocalDate endDate) throws IOException {
        logger.info("Generating statistical report Excel for dates: {} to {}", startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Define Cell Styles
            CellStyle headerCellStyle = createHeaderCellStyle(workbook);
            CellStyle dataCellStyle = createDataCellStyle(workbook);
            CellStyle boldDataCellStyle = createBoldDataCellStyle(workbook); // For totals or key figures

            // Fetch overall statistics
            Map<String, Object> overallStatsData = workTimeService.getAllProjectsStats(startDate, endDate);
            if (overallStatsData == null) {
                overallStatsData = new HashMap<>(); // Ensure it's not null for safety
                logger.warn("Overall statistics data from workTimeService was null for dates: {} to {}", startDate, endDate);
            }


            // Create Sheets
            createOverallStatsSheet(workbook, "Overall Statistics", headerCellStyle, dataCellStyle, boldDataCellStyle, overallStatsData);
            createProjectStatsSheet(workbook, "Project Statistics", headerCellStyle, dataCellStyle, overallStatsData);
            createMemberStatsSheet(workbook, "Member Statistics", headerCellStyle, dataCellStyle, startDate, endDate, overallStatsData);

            // Auto-size columns for all sheets at the end
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                // Only auto-size if sheet has at least one row
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    Row firstRow = sheet.getRow(sheet.getFirstRowNum());
                    if (firstRow != null) {
                         for (int j = 0; j < firstRow.getLastCellNum(); j++) {
                            sheet.autoSizeColumn(j);
                        }
                    }
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            logger.error("Error generating statistical report Excel: {}", e.getMessage(), e);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to generate statistical report Excel: " + e.getMessage(), e);
        }
    }

    // --- Helper methods for Cell Styles ---
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(headerFont);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }
    
    private CellStyle createBoldDataCellStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    // --- Helper methods for creating sheets ---

    @SuppressWarnings("unchecked")
    private void createOverallStatsSheet(Workbook workbook, String sheetName, CellStyle headerStyle, CellStyle dataStyle, CellStyle boldStyle, Map<String, Object> statsData) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIdx = 0;

        // Header
        Row headerRow = sheet.createRow(rowIdx++);
        Cell cell = headerRow.createCell(0);
        cell.setCellValue("Statistic Name");
        cell.setCellStyle(headerStyle);
        cell = headerRow.createCell(1);
        cell.setCellValue("Value");
        cell.setCellStyle(headerStyle);

        // Data
        String[] statKeys = {"totalHours", "recordCount", "averageHoursPerRecord", "overtimeHours"};
        String[] statLabels = {"Total Hours", "Record Count", "Average Hours Per Record", "Overtime Hours"};
        
        DecimalFormat df = new DecimalFormat("#,##0.00");

        for (int i = 0; i < statKeys.length; i++) {
            Row dataRow = sheet.createRow(rowIdx++);
            Cell labelCell = dataRow.createCell(0);
            labelCell.setCellValue(statLabels[i]);
            labelCell.setCellStyle(boldStyle); // Make labels bold

            Cell valueCell = dataRow.createCell(1);
            Object rawValue = statsData.get(statKeys[i]);
            if (rawValue instanceof Number) {
                 if (statKeys[i].equals("recordCount")) {
                    valueCell.setCellValue(((Number) rawValue).intValue());
                } else {
                    valueCell.setCellValue(df.format(((Number) rawValue).doubleValue()));
                }
            } else if (rawValue != null) {
                valueCell.setCellValue(rawValue.toString());
            } else {
                valueCell.setCellValue("N/A");
            }
            valueCell.setCellStyle(dataStyle);
        }
    }

    @SuppressWarnings("unchecked")
    private void createProjectStatsSheet(Workbook workbook, String sheetName, CellStyle headerStyle, CellStyle dataStyle, Map<String, Object> overallStats) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIdx = 0;

        // Header
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Project ID", "Project Name", "Total Hours", "Percentage of Overall Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        Map<Long, Double> projectHoursMap = (Map<Long, Double>) overallStats.getOrDefault("projects", new HashMap<Long, Double>());
        Double overallTotalHours = ((Number) overallStats.getOrDefault("totalHours", 0.0)).doubleValue();
        if (overallTotalHours == null || overallTotalHours == 0) overallTotalHours = 1.0; // Avoid division by zero if no hours

        List<Project> allProjects = projectService.getAllProjects(); // Fetch all projects
        Map<Long, String> projectIdToNameMap = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (name1, name2) -> name1)); // Handle duplicate IDs if any
        
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pctDf = new DecimalFormat("0.00%");

        List<Map.Entry<Long, Double>> sortedProjects = projectHoursMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toList());

        for (Map.Entry<Long, Double> entry : sortedProjects) {
            Long projectId = entry.getKey();
            Double projectTotalHours = entry.getValue();
            if (projectTotalHours == null) projectTotalHours = 0.0;

            String projectName = projectIdToNameMap.getOrDefault(projectId, "Unknown Project");
            double percentage = (overallTotalHours > 0) ? (projectTotalHours / overallTotalHours) : 0.0;

            Row dataRow = sheet.createRow(rowIdx++);
            dataRow.createCell(0).setCellValue(projectId);
            dataRow.createCell(1).setCellValue(projectName);
            dataRow.createCell(2).setCellValue(df.format(projectTotalHours));
            dataRow.createCell(3).setCellValue(pctDf.format(percentage));
            for(int i=0; i<4; i++) dataRow.getCell(i).setCellStyle(dataStyle);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void createMemberStatsSheet(Workbook workbook, String sheetName, CellStyle headerStyle, CellStyle dataStyle, LocalDate startDate, LocalDate endDate, Map<String, Object> overallStats) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIdx = 0;

        // Header
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Project Name", "Member Username", "Member Real Name", "Total Hours in Project", "Record Count in Project", "Average Hours per Record (in Project)", "Percentage of Project Total Hours"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        Map<Long, Double> projectHoursMap = (Map<Long, Double>) overallStats.getOrDefault("projects", new HashMap<Long, Double>());
        List<Project> allProjects = projectService.getAllProjects();
        Map<Long, Project> projectIdToProjectMap = allProjects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (p1, p2) -> p1));

        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pctDf = new DecimalFormat("0.00%");

        List<Map<String, Object>> memberStatsList = new ArrayList<>();

        for (Map.Entry<Long, Double> projectEntry : projectHoursMap.entrySet()) {
            Long projectId = projectEntry.getKey();
            Double projectTotalHoursForCalc = projectEntry.getValue();
             if (projectTotalHoursForCalc == null || projectTotalHoursForCalc == 0) projectTotalHoursForCalc = 1.0; // Avoid division by zero

            Project project = projectIdToProjectMap.get(projectId);
            if (project == null) {
                logger.warn("Project with ID {} not found, skipping for member stats.", projectId);
                continue;
            }
            String projectName = project.getName();

            Set<User> members = projectService.getProjectMembers(projectId);
            if (members == null || members.isEmpty()) {
                continue;
            }

            for (User member : members) {
                List<WorkTimeRecord> records = workTimeService.getWorkTimeRecordsByUserAndDateRangeFiltered(
                        member, startDate, endDate, project, null); // Status null to get all

                if (records == null || records.isEmpty()) continue;

                double memberTotalHoursInProject = records.stream().mapToDouble(WorkTimeRecord::getHours).sum();
                long recordCountInProject = records.size();
                double avgHoursPerRecord = (recordCountInProject > 0) ? (memberTotalHoursInProject / recordCountInProject) : 0.0;
                double percentageOfProjectTotal = (projectTotalHoursForCalc > 0) ? (memberTotalHoursInProject / projectTotalHoursForCalc) : 0.0;

                Map<String, Object> stats = new HashMap<>();
                stats.put("projectName", projectName);
                stats.put("username", member.getUsername());
                stats.put("realName", member.getRealName() != null ? member.getRealName() : "N/A");
                stats.put("totalHours", memberTotalHoursInProject);
                stats.put("recordCount", recordCountInProject);
                stats.put("avgHours", avgHoursPerRecord);
                stats.put("percentage", percentageOfProjectTotal);
                memberStatsList.add(stats);
            }
        }
        
        // Sort member stats: by project name, then by total hours desc.
        memberStatsList.sort((s1, s2) -> {
            String p1 = (String) s1.get("projectName");
            String p2 = (String) s2.get("projectName");
            int projectCompare = p1.compareTo(p2);
            if (projectCompare != 0) {
                return projectCompare;
            }
            Double h1 = (Double) s1.get("totalHours");
            Double h2 = (Double) s2.get("totalHours");
            return h2.compareTo(h1); // Descending for hours
        });


        for (Map<String, Object> stats : memberStatsList) {
            Row dataRow = sheet.createRow(rowIdx++);
            dataRow.createCell(0).setCellValue((String) stats.get("projectName"));
            dataRow.createCell(1).setCellValue((String) stats.get("username"));
            dataRow.createCell(2).setCellValue((String) stats.get("realName"));
            dataRow.createCell(3).setCellValue(df.format((Double) stats.get("totalHours")));
            dataRow.createCell(4).setCellValue((Long) stats.get("recordCount"));
            dataRow.createCell(5).setCellValue(df.format((Double) stats.get("avgHours")));
            dataRow.createCell(6).setCellValue(pctDf.format((Double) stats.get("percentage")));
            for(int i=0; i<7; i++) dataRow.getCell(i).setCellStyle(dataStyle);
        }
    }
}
