package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final LocalDate MIN_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);

    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<User> findUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUser(Integer id) {
        return userRepository.findWithSecurityById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Person> findPeople() {
        return findPeople(null);
    }

    @Transactional(readOnly = true)
    public List<Person> findPeople(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return personRepository.findAll();
        }

        String normalizedRoleName = roleName.trim().toUpperCase(Locale.ROOT);
        LinkedHashMap<Integer, Person> peopleById = new LinkedHashMap<>();
        userRepository.findByRoles_NameIgnoreCaseAndActiveTrue(normalizedRoleName)
                .stream()
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .forEach(person -> peopleById.put(person.getId(), person));
        return List.copyOf(peopleById.values());
    }

    @Transactional(readOnly = true)
    public Person getPerson(Integer id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));
    }

    @Transactional
    public Person createPerson(String firstName,
                               String lastName,
                               LocalDate dateOfBirth,
                               String email,
                               String phone) {
        String normalizedEmail = FacultyService.requireText(email, "Email").toLowerCase(Locale.ROOT);
        if (personRepository.existsByEmail(normalizedEmail)) {
            throw new AuthConflictException("Person email already exists: " + normalizedEmail);
        }

        Person person = new Person();
        person.setFirstName(FacultyService.requireText(firstName, "FirstName"));
        person.setLastName(FacultyService.requireText(lastName, "LastName"));
        person.setDateOfBirth(normalizeDateOfBirth(dateOfBirth));
        person.setEmail(normalizedEmail);
        person.setPhone(phone == null ? "" : phone.trim());
        return personRepository.save(person);
    }

    @Transactional
    public Person updatePerson(Integer personId,
                               String firstName,
                               String lastName,
                               LocalDate dateOfBirth,
                               String email,
                               String phone) {
        Person person = getPerson(personId);
        if (firstName != null) {
            person.setFirstName(FacultyService.requireText(firstName, "FirstName"));
        }
        if (lastName != null) {
            person.setLastName(FacultyService.requireText(lastName, "LastName"));
        }
        if (dateOfBirth != null) {
            person.setDateOfBirth(normalizeDateOfBirth(dateOfBirth));
        }
        if (email != null) {
            String normalizedEmail = FacultyService.requireText(email, "Email").toLowerCase(Locale.ROOT);
            if (!normalizedEmail.equals(person.getEmail()) && personRepository.existsByEmail(normalizedEmail)) {
                throw new AuthConflictException("Person email already exists: " + normalizedEmail);
            }
            person.setEmail(normalizedEmail);
        }
        person.setPhone(phone == null ? "" : phone.trim());
        return person;
    }

    @Transactional
    public User setActive(Integer userId, boolean active) {
        User user = getUser(userId);
        user.setActive(active);
        return user;
    }

    private static LocalDate normalizeDateOfBirth(LocalDate dateOfBirth) {
        LocalDate actual = dateOfBirth == null ? MIN_DATE_OF_BIRTH : dateOfBirth;
        if (actual.isBefore(MIN_DATE_OF_BIRTH)) {
            throw new IllegalArgumentException("DateOfBirth must be greater than or equal to 1900-01-01.");
        }
        return actual;
    }
}
