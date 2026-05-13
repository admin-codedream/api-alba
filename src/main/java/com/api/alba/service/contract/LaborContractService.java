package com.api.alba.service.contract;

import com.api.alba.domain.contract.LaborContract;
import com.api.alba.domain.auth.User;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.contract.LaborContractCreateRequest;
import com.api.alba.dto.contract.LaborContractRejectRequest;
import com.api.alba.dto.push.StaffReminderTarget;
import com.api.alba.exception.ApiException;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.contract.LaborContractMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.push.PushTokenMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.CONTRACT_ACCESS_DENIED;
import static com.api.alba.exception.ExceptionMessages.CONTRACT_ALREADY_SENT;
import static com.api.alba.exception.ExceptionMessages.CONTRACT_CANNOT_BE_CANCELLED;
import static com.api.alba.exception.ExceptionMessages.CONTRACT_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.CONTRACT_NOT_SENT;
import static com.api.alba.exception.ExceptionMessages.MEMBER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.OWNER_ACCESS_ONLY;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class LaborContractService {

    private final LaborContractMapper laborContractMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final UserMapper userMapper;
    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;

    @Transactional
    public LaborContract createContract(Long ownerUserId, Long workplaceId, LaborContractCreateRequest request) {
        ensureOwner(workplaceId, ownerUserId);

        WorkplaceMember employeeMember = workplaceMemberMapper.findActiveMember(workplaceId, request.getEmployeeUserId());
        if (employeeMember == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }

        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (workplace == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, WORKPLACE_NOT_FOUND);
        }

        User owner = userMapper.findById(ownerUserId);
        User employee = userMapper.findById(request.getEmployeeUserId());

        LaborContract contract = new LaborContract();
        contract.setWorkplaceId(workplaceId);
        contract.setEmployeeUserId(request.getEmployeeUserId());
        contract.setStatus("DRAFT");
        contract.setContractStartDate(request.getContractStartDate());
        contract.setContractEndDate(request.getContractEndDate());
        contract.setWorkplaceName(workplace.getName());
        contract.setWorkplaceAddress(workplace.getAddress());
        contract.setOwnerName(owner.getName());
        contract.setEmployeeName(employee.getName());
        contract.setJobDescription(request.getJobDescription());
        contract.setWorkDays(request.getWorkDays());
        contract.setWorkStartTime(request.getWorkStartTime());
        contract.setWorkEndTime(request.getWorkEndTime());
        contract.setBreakMinutes(request.getBreakMinutes());
        contract.setHourlyWage(request.getHourlyWage());
        contract.setPaymentDay(request.getPaymentDay());
        contract.setUseNationalPension(Boolean.TRUE.equals(request.getUseNationalPension()));
        contract.setUseHealthInsurance(Boolean.TRUE.equals(request.getUseHealthInsurance()));
        contract.setUseEmpInsurance(Boolean.TRUE.equals(request.getUseEmpInsurance()));

        laborContractMapper.insert(contract);
        return laborContractMapper.findById(contract.getId());
    }

    @Transactional
    public void sendContract(Long ownerUserId, Long workplaceId, Long contractId) {
        ensureOwner(workplaceId, ownerUserId);

        LaborContract contract = findContractInWorkplace(contractId, workplaceId);

        LocalDateTime now = LocalDateTime.now();
        int updated = laborContractMapper.updateToSent(contractId, now, now);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, CONTRACT_ALREADY_SENT);
        }

        sendContractPushToEmployee(contract.getEmployeeUserId(), workplaceId, contract.getWorkplaceName(), contractId);
    }

    @Transactional
    public void cancelContract(Long ownerUserId, Long workplaceId, Long contractId) {
        ensureOwner(workplaceId, ownerUserId);
        findContractInWorkplace(contractId, workplaceId);

        int updated = laborContractMapper.updateToCancelled(contractId);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, CONTRACT_CANNOT_BE_CANCELLED);
        }
    }

    public List<LaborContract> getContracts(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        return laborContractMapper.findByWorkplaceId(workplaceId);
    }

    public LaborContract getContract(Long ownerUserId, Long workplaceId, Long contractId) {
        ensureOwner(workplaceId, ownerUserId);
        return findContractInWorkplace(contractId, workplaceId);
    }

    public List<LaborContract> getMyContracts(Long staffUserId) {
        return laborContractMapper.findByEmployeeUserId(staffUserId);
    }

    public LaborContract getMyContract(Long staffUserId, Long contractId) {
        LaborContract contract = laborContractMapper.findById(contractId);
        if (contract == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND);
        }
        if (!contract.getEmployeeUserId().equals(staffUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, CONTRACT_ACCESS_DENIED);
        }
        return contract;
    }

    @Transactional
    public void signContract(Long staffUserId, Long contractId) {
        LaborContract contract = getMyContract(staffUserId, contractId);

        int updated = laborContractMapper.updateToSigned(contractId, LocalDateTime.now());
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, CONTRACT_NOT_SENT);
        }
    }

    @Transactional
    public void rejectContract(Long staffUserId, Long contractId, LaborContractRejectRequest request) {
        LaborContract contract = getMyContract(staffUserId, contractId);

        int updated = laborContractMapper.updateToRejected(contractId, request.getReason());
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, CONTRACT_NOT_SENT);
        }
    }

    private void ensureOwner(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveOwnerMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
        }
    }

    private LaborContract findContractInWorkplace(Long contractId, Long workplaceId) {
        LaborContract contract = laborContractMapper.findById(contractId);
        if (contract == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, CONTRACT_NOT_FOUND);
        }
        if (!contract.getWorkplaceId().equals(workplaceId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, CONTRACT_ACCESS_DENIED);
        }
        return contract;
    }

    private void sendContractPushToEmployee(Long employeeUserId, Long workplaceId, String workplaceName, Long contractId) {
        List<StaffReminderTarget> tokens = pushTokenMapper.findStaffPushTokensByUserIdAndWorkplaceId(employeeUserId, workplaceId);
        if (tokens.isEmpty()) {
            return;
        }
        List<FcmDto> fcmList = tokens.stream()
                .map(t -> FcmDto.builder()
                        .pushSeq(contractId)
                        .pushToken(t.getToken())
                        .title("근로계약서가 도착했어요")
                        .content(workplaceName + "에서 근로계약서를 보냈어요. 확인해 주세요.")
                        .pushLink("contract/" + contractId)
                        .project(ProjectId.ALBAM.getMessage())
                        .build())
                .collect(Collectors.toList());
        fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmList);
    }
}