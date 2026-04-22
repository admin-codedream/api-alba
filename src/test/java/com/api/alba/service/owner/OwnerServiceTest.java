package com.api.alba.service.owner;

import com.api.alba.domain.auth.User;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.owner.AttendancePushSettingResponse;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.settings.WorkplaceBreakPolicyMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {
    @Mock
    private WorkplaceMapper workplaceMapper;
    @Mock
    private WorkplaceMemberMapper workplaceMemberMapper;
    @Mock
    private WorkplaceSettingMapper workplaceSettingMapper;
    @Mock
    private WorkplaceBreakPolicyMapper workplaceBreakPolicyMapper;
    @Mock
    private AttendanceRecordMapper attendanceRecordMapper;
    @Mock
    private AttendanceRequestMapper attendanceRequestMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OwnerService ownerService;

    @Test
    void getAttendancePushSettingIncludesSalaryCalcUnit() {
        Workplace workplace = new Workplace();
        workplace.setId(10L);
        workplace.setName("Store");
        workplace.setUseLocationRestriction(false);

        WorkplaceSetting setting = new WorkplaceSetting();
        setting.setWorkplaceId(10L);
        setting.setDefaultHourlyWage(new BigDecimal("11000"));
        setting.setSalaryCalcUnit("10MIN");

        WorkplaceMember ownerMember = new WorkplaceMember();
        ownerMember.setReceiveAttendancePush(true);

        when(workplaceMemberMapper.findActiveOwnerMember(10L, 7L)).thenReturn(ownerMember);
        when(workplaceMapper.findById(10L)).thenReturn(workplace);
        when(workplaceSettingMapper.findByWorkplaceId(10L)).thenReturn(setting);

        AttendancePushSettingResponse response = ownerService.getAttendancePushSetting(7L, 10L);

        assertThat(response.getSalaryCalcUnit()).isEqualTo("10MIN");
    }

    @Test
    void updateSalaryCalcUnitPersistsNewValue() {
        when(workplaceMemberMapper.findActiveOwnerMember(10L, 7L)).thenReturn(new WorkplaceMember());
        when(workplaceSettingMapper.findByWorkplaceId(10L)).thenReturn(new WorkplaceSetting());

        ownerService.updateSalaryCalcUnit(7L, 10L, "HOUR");

        verify(workplaceSettingMapper).updateSalaryCalcUnit(10L, "HOUR");
    }

    @Test
    void updateSalaryCalcUnitRequiresExistingSetting() {
        when(workplaceMemberMapper.findActiveOwnerMember(10L, 7L)).thenReturn(new WorkplaceMember());
        when(workplaceSettingMapper.findByWorkplaceId(10L)).thenReturn(null);

        assertThatThrownBy(() -> ownerService.updateSalaryCalcUnit(7L, 10L, "10MIN"))
                .isInstanceOf(ApiException.class);

        verify(workplaceSettingMapper, never()).updateSalaryCalcUnit(any(), any());
    }

    @Test
    void createWorkplaceStoresTenMinuteSalaryCalcUnitByDefault() {
        when(userMapper.findById(7L)).thenReturn(ownerUser());
        doAnswer(invocation -> {
            Workplace workplace = invocation.getArgument(0);
            workplace.setId(10L);
            return 1;
        }).when(workplaceMapper).insert(any(Workplace.class));

        Workplace savedWorkplace = new Workplace();
        savedWorkplace.setId(10L);
        when(workplaceMapper.findById(10L)).thenReturn(savedWorkplace);

        ownerService.createWorkplace(7L, createWorkplaceRequest());

        ArgumentCaptor<WorkplaceSetting> settingCaptor = ArgumentCaptor.forClass(WorkplaceSetting.class);
        verify(workplaceSettingMapper).insert(settingCaptor.capture());
        assertThat(settingCaptor.getValue().getSalaryCalcUnit()).isEqualTo("10MIN");
    }

    private User ownerUser() {
        User user = new User();
        user.setId(7L);
        user.setUserType("OWNER");
        return user;
    }

    private CreateWorkplaceRequest createWorkplaceRequest() {
        CreateWorkplaceRequest request = new CreateWorkplaceRequest();
        request.setName("Store");
        request.setAddress("Seoul");
        request.setHourlyWage(new BigDecimal("11000"));
        request.setUseLocationRestriction(false);
        return request;
    }
}
