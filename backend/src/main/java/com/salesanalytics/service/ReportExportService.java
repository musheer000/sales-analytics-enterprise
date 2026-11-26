package com.salesanalytics.service;

public interface ReportExportService {
    byte[] exportSalesToCsv();
    byte[] exportCategoryRevenueToCsv();
    byte[] exportTopProductsToCsv();
}
