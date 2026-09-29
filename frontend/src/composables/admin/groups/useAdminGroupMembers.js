import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  membershipsApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const STUDENT_GROUP_ROLE = 1
const ACTIVE_MEMBERSHIP_STATUS = 1
const PAUSED_MEMBERSHIP_STATUS = 2
const REMOVED_MEMBERSHIP_STATUS = 3
const STUDENT_APP_ROLE = 'STUDENT'

export function useAdminGroupMembers() {
  const membersDrawerVisible = ref(false)
  const membersGroup = ref(null)
  const groupMemberships = ref([])
  const people = ref([])
  const students = ref([])
  const membersLoading = ref(false)
  const memberSearch = ref('')
  const addingPersonId = ref(null)

  const memberNotice = ref({
    type: 'info',
    message: '',
  })

  const memberRemoveTarget = ref(null)
  const memberRemoveConfirmVisible = ref(false)
  const removingMembershipId = ref(null)
  const memberRemoveError = ref('')

  let membersLoadSequence = 0

  const currentStudentMemberships = computed(() => {
    return groupMemberships.value
      .filter((membership) => (
        Number(membership.role) === STUDENT_GROUP_ROLE &&
        Number(membership.status) === ACTIVE_MEMBERSHIP_STATUS &&
        !membership.removedAtUtc
      ))
      .sort((left, right) =>
        personName(personById(left.personId)).localeCompare(
          personName(personById(right.personId)),
          'ru'
        )
      )
  })

  const currentStudentPersonIds = computed(() => {
    return new Set(
      currentStudentMemberships.value.map(
        (membership) => Number(membership.personId)
      )
    )
  })

  const availableStudents = computed(() => {
    return students.value
      .filter((person) =>
        !currentStudentPersonIds.value.has(Number(person.id))
      )
      .sort((left, right) =>
        personName(left).localeCompare(personName(right), 'ru')
      )
  })

  const normalizedMemberSearch = computed(() => {
    return memberSearch.value
      .trim()
      .toLocaleLowerCase('ru-RU')
  })

  const filteredCurrentStudentMemberships = computed(() => {
    const query = normalizedMemberSearch.value

    if (!query) {
      return currentStudentMemberships.value
    }

    return currentStudentMemberships.value.filter((membership) =>
      personMatchesSearch(personById(membership.personId), query)
    )
  })

  const filteredAvailableStudents = computed(() => {
    const query = normalizedMemberSearch.value

    if (!query) {
      return availableStudents.value
    }

    return availableStudents.value.filter((person) =>
      personMatchesSearch(person, query)
    )
  })

  const membersDrawerTitle = computed(() => {
    if (!membersGroup.value) {
      return 'Состав группы'
    }

    return `Состав группы ${membersGroup.value.code || membersGroup.value.name}`
  })

  const membersBusy = computed(() => (
    membersLoading.value ||
    addingPersonId.value !== null ||
    removingMembershipId.value !== null
  ))

  const memberResultText = computed(() => {
    return (
      `В группе: ${filteredCurrentStudentMemberships.value.length} из ${currentStudentMemberships.value.length}; ` +
      `доступно: ${filteredAvailableStudents.value.length} из ${availableStudents.value.length}`
    )
  })

  function personById(personId) {
    return people.value.find(
      (person) => Number(person.id) === Number(personId)
    ) ?? null
  }

  function personName(person) {
    if (!person) {
      return 'Профиль участника недоступен'
    }

    const name = [
      person.lastName,
      person.firstName,
    ]
      .filter(Boolean)
      .join(' ')
      .trim()

    return name || person.email || 'Профиль участника недоступен'
  }

  function personContact(person) {
    if (!person) {
      return ''
    }

    return [person.email, person.phone]
      .filter(Boolean)
      .join(' · ')
  }

  function personMatchesSearch(person, query) {
    if (!query) {
      return true
    }

    const haystack = [
      personName(person),
      person?.email,
      person?.phone,
    ]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  }

  function pausedStudentMembership(personId) {
    return groupMemberships.value.find((membership) => (
      Number(membership.personId) === Number(personId) &&
      Number(membership.role) === STUDENT_GROUP_ROLE &&
      Number(membership.status) === PAUSED_MEMBERSHIP_STATUS &&
      !membership.removedAtUtc
    )) ?? null
  }

  function availableStudentActionLabel(person) {
    return pausedStudentMembership(person?.id)
      ? 'Вернуть в группу'
      : 'Добавить'
  }

  function clearMemberNotice() {
    memberNotice.value.message = ''
  }

  function showMemberNotice(type, message) {
    memberNotice.value = {
      type,
      message,
    }
  }

  function resetMemberSearch() {
    memberSearch.value = ''
  }

  function isCurrentMemberGroup(groupId) {
    return Number(membersGroup.value?.id) === Number(groupId)
  }

  async function loadGroupMembers(groupId, { quiet = false } = {}) {
    const normalizedGroupId = Number(groupId)

    if (!Number.isFinite(normalizedGroupId) || normalizedGroupId <= 0) {
      return
    }

    const requestId = ++membersLoadSequence

    if (!quiet) {
      membersLoading.value = true
      clearMemberNotice()
    }

    try {
      const [
        membershipsResponse,
        peopleResponse,
        studentsResponse,
      ] = await Promise.all([
        membershipsApi.getGroupMemberships({
          groupId: normalizedGroupId,
          activeOnly: false,
        }),
        usersApi.getPeople(),
        usersApi.getPeople({
          role: STUDENT_APP_ROLE,
        }),
      ])

      if (
        requestId !== membersLoadSequence ||
        Number(membersGroup.value?.id) !== normalizedGroupId
      ) {
        return
      }

      groupMemberships.value = listFromResponse(membershipsResponse)
      people.value = listFromResponse(peopleResponse)
      students.value = listFromResponse(studentsResponse)
    } catch (error) {
      if (requestId !== membersLoadSequence) {
        return
      }

      showMemberNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить состав группы'
        )
      )
    } finally {
      if (requestId === membersLoadSequence) {
        membersLoading.value = false
      }
    }
  }

  async function openGroupMembers(group) {
    membersGroup.value = group
    memberSearch.value = ''
    groupMemberships.value = []
    people.value = []
    students.value = []
    clearMemberNotice()
    membersDrawerVisible.value = true

    await loadGroupMembers(group.id)
  }

  function resetMembersDrawer() {
    if (membersBusy.value) {
      return
    }

    membersLoadSequence += 1
    membersGroup.value = null
    groupMemberships.value = []
    people.value = []
    students.value = []
    memberSearch.value = ''
    clearMemberNotice()
  }

  async function addStudentToGroup(person) {
    const groupId = Number(membersGroup.value?.id)
    const personId = Number(person?.id)

    if (
      !Number.isFinite(groupId) || groupId <= 0 ||
      !Number.isFinite(personId) || personId <= 0 ||
      addingPersonId.value !== null
    ) {
      return
    }

    addingPersonId.value = personId
    clearMemberNotice()

    try {
      const pausedMembership = pausedStudentMembership(personId)

      if (pausedMembership) {
        await membershipsApi.updateGroupMembershipStatus(
          pausedMembership.id,
          { status: ACTIVE_MEMBERSHIP_STATUS }
        )
      } else {
        await membershipsApi.addPersonToGroup(
          groupId,
          {
            personId,
            role: STUDENT_GROUP_ROLE,
            notes: null,
          }
        )
      }

      await loadGroupMembers(groupId, { quiet: true })

      if (!isCurrentMemberGroup(groupId)) {
        return
      }

      showMemberNotice(
        'success',
        pausedMembership
          ? `${personName(person)} снова в составе группы.`
          : `${personName(person)} добавлен в группу.`
      )
    } catch (error) {
      if (!isCurrentMemberGroup(groupId)) {
        return
      }

      showMemberNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось добавить студента в группу'
        )
      )
    } finally {
      addingPersonId.value = null
    }
  }

  function requestRemoveMember(membership) {
    memberRemoveTarget.value = {
      membership,
      person: personById(membership.personId),
    }
    memberRemoveError.value = ''
    memberRemoveConfirmVisible.value = true
  }

  function closeMemberRemoveDialog() {
    if (removingMembershipId.value !== null) {
      return
    }

    memberRemoveConfirmVisible.value = false
    memberRemoveTarget.value = null
    memberRemoveError.value = ''
  }

  async function removeStudentFromGroup() {
    const target = memberRemoveTarget.value
    const membership = target?.membership
    const groupId = Number(membersGroup.value?.id)

    if (
      !membership ||
      !Number.isFinite(groupId) || groupId <= 0 ||
      removingMembershipId.value !== null
    ) {
      return
    }

    const targetMembershipId = Number(membership.id)
    const targetPersonName = personName(target.person)

    removingMembershipId.value = targetMembershipId
    memberRemoveError.value = ''

    try {
      await membershipsApi.updateGroupMembershipStatus(
        targetMembershipId,
        { status: REMOVED_MEMBERSHIP_STATUS }
      )

      await loadGroupMembers(groupId, { quiet: true })

      if (!isCurrentMemberGroup(groupId)) {
        return
      }

      if (
        Number(memberRemoveTarget.value?.membership?.id) ===
        targetMembershipId
      ) {
        memberRemoveConfirmVisible.value = false
        memberRemoveTarget.value = null
      }

      showMemberNotice(
        'success',
        `${targetPersonName} убран из группы.`
      )
    } catch (error) {
      if (!isCurrentMemberGroup(groupId)) {
        return
      }

      memberRemoveError.value = getApiErrorMessage(
        error,
        'Не удалось убрать студента из группы'
      )
    } finally {
      if (removingMembershipId.value === targetMembershipId) {
        removingMembershipId.value = null
      }
    }
  }

  return {
    membersDrawerVisible,
    membersGroup,
    groupMemberships,
    people,
    students,
    membersLoading,
    memberSearch,
    addingPersonId,
    memberNotice,
    memberRemoveTarget,
    memberRemoveConfirmVisible,
    removingMembershipId,
    memberRemoveError,
    currentStudentMemberships,
    availableStudents,
    filteredCurrentStudentMemberships,
    filteredAvailableStudents,
    membersDrawerTitle,
    membersBusy,
    memberResultText,
    personById,
    personName,
    personContact,
    pausedStudentMembership,
    availableStudentActionLabel,
    clearMemberNotice,
    resetMemberSearch,
    loadGroupMembers,
    openGroupMembers,
    resetMembersDrawer,
    addStudentToGroup,
    requestRemoveMember,
    closeMemberRemoveDialog,
    removeStudentFromGroup,
  }
}
