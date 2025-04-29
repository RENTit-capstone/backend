package com.capstone.rentit.config;

import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DummyDataLoader {

    @Bean
    public CommandLineRunner loadDummyStudents(MemberRepository memberRepository,
                                               PasswordEncoder passwordEncoder) {
        return args -> {
            List<Student> dummyStudents = List.of(
                    Student.builder()
                            .email("dummy1@student.com")
                            .password(passwordEncoder.encode("password1"))
                            .name("Dummy Student One")
                            .nickname("dummynick1")
                            .university("Dummy University")
                            .studentId("DUMMY1001")
                            .gender("M")
                            .phone("010-1111-2222")
                            .role(MemberRoleEnum.STUDENT)
                            .createdAt(LocalDate.now())
                            .locked(false)
                            .build(),
                    Student.builder()
                            .email("dummy2@student.com")
                            .password(passwordEncoder.encode("password2"))
                            .name("Dummy Student Two")
                            .nickname("dummynick2")
                            .university("Dummy University")
                            .studentId("DUMMY1002")
                            .gender("F")
                            .phone("010-3333-4444")
                            .role(MemberRoleEnum.STUDENT)
                            .createdAt(LocalDate.now())
                            .locked(false)
                            .build(),
                    Student.builder()
                            .email("dummy3@student.com")
                            .password(passwordEncoder.encode("password3"))
                            .name("Dummy Student Three")
                            .nickname("dummynick3")
                            .university("Dummy University")
                            .studentId("DUMMY1003")
                            .gender("M")
                            .phone("010-5555-6666")
                            .role(MemberRoleEnum.STUDENT)
                            .createdAt(LocalDate.now())
                            .locked(false)
                            .build()
            );

            dummyStudents.forEach(student -> {
                memberRepository.save(student);
                System.out.printf("Inserted dummy student: %s%n", student.getEmail());
            });
        };
    }
}

