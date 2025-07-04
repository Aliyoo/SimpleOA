package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.repository.LeaveRepository;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Override
    public LeaveRequest createLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest updateLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }

    @Override
    public void deleteLeave(Long id) {
        leaveRepository.deleteById(id);
    }

    @Override
    public LeaveRequest getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }

    @Override
    public List<LeaveRequest> getAllLeaves() {
        return leaveRepository.findAll();
    }

    @Override
    public List<LeaveRequest> getLeavesByUser(Long userId) {
        return leaveRepository.findByApplicantId(userId);
    }

    @Override
    public LeaveRequest approveLeave(Long id, String status, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest != null) {
            leaveRequest.setStatus(status);
            leaveRequest.setComment(comment);
            return leaveRepository.save(leaveRequest);
        }
        return null;
    }

    @Override
    public List<LeaveRequest> getApprovalList() {
        // In a real application, this should be more complex, 
        // for example, only return requests that need to be approved by the current user.
        return leaveRepository.findAll();
    }

    @Override
    public Object getLeaveStatistics(String startDate, String endDate) {
        // This is a placeholder. In a real application, you would need to implement the logic to calculate the statistics.
        return null;
    }

    @Override
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) {
        leaveRequest.setStatus("待审批");
        return leaveRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest rejectLeave(Long id, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest != null) {
            leaveRequest.setStatus("已拒绝");
            leaveRequest.setComment(comment);
            return leaveRepository.save(leaveRequest);
        }
        return null;
    }
}
