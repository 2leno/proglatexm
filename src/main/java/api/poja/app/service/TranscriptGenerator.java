package api.poja.app.service;

import api.poja.app.repository.model.JGrade;
import api.poja.app.repository.model.JStudent;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranscriptGenerator {

  public byte[] generate(JStudent student, Integer year, List<JGrade> grades) {
    var document = new Document();
    var output = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(document, output);
      document.open();
      document.add(
          new Paragraph(
              "Academic Transcript - Year " + year,
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
      document.add(new Paragraph(""));
      document.add(
          new Paragraph(
              student.getFirstName() + " " + student.getLastName(),
              FontFactory.getFont(FontFactory.HELVETICA, 12)));
      document.add(
          new Paragraph(
              "Reference: " + student.getReference() + " - Parcours: " + student.getParcours(),
              FontFactory.getFont(FontFactory.HELVETICA, 12)));
      document.add(new Paragraph(""));
      var table = new PdfPTable(5);
      addHeader(table, "Course");
      addHeader(table, "Credits");
      addHeader(table, "Coefficient");
      addHeader(table, "Exam");
      addHeader(table, "Grade");
      for (var grade : grades) {
        table.addCell(grade.getExam().getCourse().getReference());
        table.addCell(String.valueOf(grade.getExam().getCourse().getCredits()));
        table.addCell(String.valueOf(grade.getExam().getCoefficient()));
        table.addCell(grade.getExam().getName());
        table.addCell(String.valueOf(grade.getValue()));
      }
      document.add(table);
      document.add(new Paragraph(""));
      document.add(
          new Paragraph(
              "Weighted average: "
                  + weightedAverage(grades)
                  + " - Total credits: "
                  + creditsOf(grades),
              FontFactory.getFont(FontFactory.HELVETICA, 12)));
      document.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return output.toByteArray();
  }

  private void addHeader(PdfPTable table, String text) {
    table.addCell(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
  }

  private double weightedAverage(List<JGrade> grades) {
    if (grades.isEmpty()) {
      return 0.0;
    }
    var totalCoefficient =
        grades.stream().mapToDouble(grade -> grade.getExam().getCoefficient()).sum();
    if (totalCoefficient == 0.0) {
      return 0.0;
    }
    var weightedSum =
        grades.stream()
            .mapToDouble(grade -> grade.getValue() * grade.getExam().getCoefficient())
            .sum();
    return weightedSum / totalCoefficient;
  }

  private int creditsOf(List<JGrade> grades) {
    return grades.stream()
        .map(grade -> grade.getExam().getCourse().getCredits())
        .distinct()
        .mapToInt(Integer::intValue)
        .sum();
  }
}
