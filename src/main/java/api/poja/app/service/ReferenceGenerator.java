package api.poja.app.service;

public final class ReferenceGenerator {
  public static final String STUDENT_PREFIX = "STD";
  public static final String TEACHER_PREFIX = "TCH";

  private ReferenceGenerator() {}

  public static String studentReference(int year, int sequence) {
    return reference(STUDENT_PREFIX, year, sequence);
  }

  public static String teacherReference(int year, int sequence) {
    return reference(TEACHER_PREFIX, year, sequence);
  }

  public static String reference(String prefix, int year, int sequence) {
    return prefix + "%02d%03d".formatted(year % 100, sequence);
  }
}
