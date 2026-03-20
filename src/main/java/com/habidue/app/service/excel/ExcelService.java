package com.habidue.app.service.excel;

import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.domain.tag.NoticeTag;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    public byte[] exportUserNoticesToExcel(List<UserNotice> userNotices) throws IOException {
        // 1. 데이터 정렬 (마감 임박 순)
        List<UserNotice> sortedNotices = userNotices.stream()
                .sorted(Comparator.comparing(
                        un -> un.getNotice().getDeadline(), 
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("HabiDue 관심 공고 리스트");
            
            // 헤더 고정
            sheet.createFreezePane(0, 1);

            // 2. 스타일 정의
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle linkStyle = createLinkStyle(workbook);
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 3. 헤더 생성
            String[] headers = {"No", "상태", "기관", "공고명 (클릭 시 원문 이동)", "남은기간", "주요 특징", "마감일", "내 메모"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 4. 데이터 삽입
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();
            int rowIdx = 1;

            for (UserNotice un : sortedNotices) {
                Row row = sheet.createRow(rowIdx++);
                
                // No
                row.createCell(0).setCellValue(rowIdx - 1);
                
                // 상태 (시스템 상태 태그 활용)
                String status = determineStatus(un);
                row.createCell(1).setCellValue(status);
                
                // 기관
                String source = un.getNotice().getSource();
                if ("PRIVATE".equalsIgnoreCase(source)) source = "민간임대";
                row.createCell(2).setCellValue(source);
                
                // 공고명 (하이퍼링크)
                Cell titleCell = row.createCell(3);
                titleCell.setCellValue(un.getNotice().getTitle());
                if (un.getNotice().getLink() != null) {
                    Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(un.getNotice().getLink());
                    titleCell.setHyperlink(link);
                    titleCell.setCellStyle(linkStyle);
                }

                // 남은기간 (D-Day)
                row.createCell(4).setCellValue(calculateDDay(un.getNotice().getDeadline(), today));
                
                // 주요 특징 (태그 통합)
                String tags = un.getNotice().getNoticeTags().stream()
                        .map(nt -> "#" + nt.getTag().getName())
                        .filter(t -> !t.contains("접수") && !t.contains("마감")) // 상태 태그 제외
                        .limit(5)
                        .collect(Collectors.joining(", "));
                row.createCell(5).setCellValue(tags);

                // 마감일
                row.createCell(6).setCellValue(un.getNotice().getDeadline() != null ? 
                        un.getNotice().getDeadline().format(formatter) : "-");
                
                // 내 메모
                row.createCell(7).setCellValue(un.getMemo() != null ? un.getMemo() : "");
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 공고명 컬럼은 너무 길어질 수 있으므로 최대 너비 제한
                if (i == 3 && sheet.getColumnWidth(i) > 15000) sheet.setColumnWidth(i, 15000);
            }
            
            // 필터 추가
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rowIdx - 1, 0, headers.length - 1));

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String determineStatus(UserNotice un) {
        return un.getNotice().getNoticeTags().stream()
                .map(nt -> nt.getTag().getName())
                .filter(name -> List.of("접수중", "마감", "결과발표", "안내").contains(name))
                .findFirst().orElse("-");
    }

    private String calculateDDay(LocalDateTime deadline, LocalDate today) {
        if (deadline == null) return "-";
        long days = ChronoUnit.DAYS.between(today, deadline.toLocalDate());
        if (days < 0) return "마감";
        if (days == 0) return "D-Day";
        return "D-" + days;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createLinkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);
        return style;
    }
}
