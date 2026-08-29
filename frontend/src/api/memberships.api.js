import http from './http'

export const membershipsApi = {
  getGroupMemberships(params = {}) {
    return http.get('/memberships/groups', {
      params,
    })
  },

  addPersonToGroup(groupId, data) {
    return http.post(`/memberships/groups/${groupId}`, data)
  },

  updateGroupMembershipStatus(membershipId, data) {
    return http.put(
      `/memberships/groups/memberships/${membershipId}/status`,
      data,
    )
  },

  getSubjectMemberships(params = {}) {
    return http.get('/memberships/subjects', {
      params,
    })
  },

  getSubjectMembership(membershipId) {
    return http.get(
      `/memberships/subjects/memberships/${membershipId}`,
    )
  },

  addPersonToSubject(subjectId, data) {
    return http.post(`/memberships/subjects/${subjectId}`, data)
  },

  updateSubjectMembershipStatus(membershipId, data) {
    return http.put(
      `/memberships/subjects/memberships/${membershipId}/status`,
      data,
    )
  },
}
