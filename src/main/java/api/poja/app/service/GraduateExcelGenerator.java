package api.poja.app.service;

import api.poja.app.model.Graduate;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class GraduateExcelGenerator {

  @SneakyThrows
  public byte[] generate(List<Graduate> graduates) {
    try (var workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("Graduates");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("Rank");
      header.createCell(1).setCellValue("Reference");
      header.createCell(2).setCellValue("Last Name");
      header.createCell(3).setCellValue("First Name");
      header.createCell(4).setCellValue("General Average");
      for (int i = 0; i < graduates.size(); i++) {
        var graduate = graduates.get(i);
        var row = sheet.createRow(i + 1);
        row.createCell(0).setCellValue(graduate.rank());
        row.createCell(1).setCellValue(graduate.reference());
        row.createCell(2).setCellValue(graduate.lastName());
        row.createCell(3).setCellValue(graduate.firstName());
        row.createCell(4).setCellValue(graduate.generalAverage());
      }
      var out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }
}
