package api.poja.app.service;

import api.poja.app.exception.ApiException;
import api.poja.app.mapper.StudentGroupPeriodMapper;
import api.poja.app.model.StudentGroupPeriod;
import api.poja.app.repository.JGroupRepository;
import api.poja.app.repository.JStudentGroupPeriodRepository;
import api.poja.app.repository.JStudentRepository;
import api.poja.app.repository.model.JGroup;
import api.poja.app.repository.model.JStudentGroupPeriod;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GroupsService {

  private final JStudentRepository studentRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupPeriodRepository periodRepository;
  private final StudentGroupPeriodMapper periodMapper;

  public api.poja.app.endpoint.rest.model.response.StudentGroupPeriod assign(
      UUID studentId, StudentGroupPeriod input) {
    var student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    var group =
        groupRepository
            .findById(input.groupId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
    if (input.startDate() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Effective date is required");
    }
    var lastPeriod = periodRepository.findFirstByStudentIdOrderByStartDateDesc(studentId);
    lastPeriod.ifPresent(
        period -> {
          if (!input.startDate().isAfter(period.getStartDate())) {
            throw new ApiException(
                HttpStatus.CONFLICT, "Effective date must be after the last period start date");
          }
          if (period.getEndDate() == null) {
            period.setEndDate(input.startDate().minusDays(1));
            periodRepository.save(period);
          }
        });
    var saved =
        periodRepository.save(
            JStudentGroupPeriod.builder()
                .student(student)
                .group(group)
                .startDate(input.startDate())
                .build());
    return periodMapper.toRest(periodMapper.toDomain(saved), group.getReference());
  }

  public List<api.poja.app.endpoint.rest.model.response.StudentGroupPeriod> getHistory(
      UUID studentId, Authentication authentication) {
    ensureOwnerOrAdmin(studentId, authentication);
    studentRepository
        .findById(studentId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
    return toRestPeriods(periodRepository.findByStudentIdOrderByStartDateAsc(studentId));
  }

  private void ensureOwnerOrAdmin(UUID studentId, Authentication authentication) {
    var authorities =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    if (authorities.contains("ROLE_STUDENT") && !authorities.contains("ROLE_ADMIN")) {
      var owner =
          studentRepository
              .findByUsername(authentication.getName())
              .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied"));
      if (!owner.getId().equals(studentId)) {
        throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
      }
    }
  }

  private List<api.poja.app.endpoint.rest.model.response.StudentGroupPeriod> toRestPeriods(
      List<JStudentGroupPeriod> periods) {
    if (periods.isEmpty()) {
      return List.of();
    }
    var references =
        groupRepository
            .findAllById(
                periods.stream().map(period -> period.getGroup().getId()).distinct().toList())
            .stream()
            .collect(Collectors.toMap(JGroup::getId, JGroup::getReference));
    return periods.stream()
        .map(
            period ->
                periodMapper.toRest(
                    periodMapper.toDomain(period), references.get(period.getGroup().getId())))
        .toList();
  }
}
