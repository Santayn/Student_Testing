package org.santayn.testing.service;

public class ActiveStudentGroupConflictException extends AuthConflictException {

    private final Integer currentGroupId;
    private final Integer currentMembershipId;

    public ActiveStudentGroupConflictException(Integer currentGroupId, Integer currentMembershipId) {
        super("Student already has an active group membership: groupId=" + currentGroupId);
        this.currentGroupId = currentGroupId;
        this.currentMembershipId = currentMembershipId;
    }

    public Integer getCurrentGroupId() {
        return currentGroupId;
    }

    public Integer getCurrentMembershipId() {
        return currentMembershipId;
    }
}
