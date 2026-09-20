package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateTransactionsReport(String username, List<TransactionResponseDto> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Paragraph header = new Paragraph("Crypto Exchange - Transaction History")
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(new DeviceRgb(41, 128, 185))
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);

            Paragraph userInfo = new Paragraph("User: " + username + " | Total Records: " + transactions.size())
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(userInfo);

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 2, 2, 2, 2, 3}))
                    .useAllAvailableWidth();

            String[] headers = {"ID", "Type", "Direction", "Amount", "Status", "Wallet Address", "Date (UTC)"};
            for (String h : headers) {
                table.addHeaderCell(new Paragraph(h)
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(ColorConstants.WHITE)
                        .setBackgroundColor(new DeviceRgb(52, 73, 94))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            for (TransactionResponseDto tx : transactions) {
                table.addCell(new Paragraph(String.valueOf(tx.id())).setFontSize(8));
                table.addCell(new Paragraph(tx.type() != null ? tx.type().name() : "-").setFontSize(8));
                table.addCell(new Paragraph(tx.direction() != null ? tx.direction().name() : "-").setFontSize(8));

                String amountStr = (tx.fromAmount() != null ? tx.fromAmount().stripTrailingZeros().toPlainString() : "0")
                        + " " + (tx.fromCryptoSymbol() != null ? tx.fromCryptoSymbol() : "");
                table.addCell(new Paragraph(amountStr).setFontSize(8));

                table.addCell(new Paragraph(tx.status() != null ? tx.status().name() : "-").setFontSize(8));

                String wallet = tx.toWalletAddress() != null ? tx.toWalletAddress() :
                        (tx.fromWalletAddress() != null ? tx.fromWalletAddress() : "-");
                table.addCell(new Paragraph(wallet).setFontSize(8));

                String dateStr = tx.createdAt() != null ? tx.createdAt().format(DATE_FORMATTER) : "-";
                table.addCell(new Paragraph(dateStr).setFontSize(8));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error during PDF report generation", e);
        }

        return out.toByteArray();
    }
}