package com.rungo.api.domain.users.service;

import com.rungo.api.domain.users.dto.CompleteProfileReq;
import com.rungo.api.domain.users.dto.MyProfileRes;
import com.rungo.api.domain.users.dto.UpdateMyProfileReq;
import com.rungo.api.domain.users.dto.UpdateMyProfileRes;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UserRepository usersRepository;

    public MyProfileRes getMyInfo(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new MyProfileRes(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getBirth(),
                user.getRole()
        );
    }

    @Transactional
    public UpdateMyProfileRes updateMyProfile(Long userId, UpdateMyProfileReq req) {
        Users user = usersRepository.findById(userId)
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newName = req.name() != null ? req.name() : user.getName();
        String newPhoneNumber = req.phoneNumber() != null ? req.phoneNumber() : user.getPhoneNumber();
        Gender newGender = req.gender() != null ? req.gender() : user.getGender();
        LocalDate newBirth = req.birth() != null ? req.birth() : user.getBirth();

        user.updateProfile(newName, newPhoneNumber, newGender, newBirth);

        return new UpdateMyProfileRes(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getBirth(),
                user.getRole()
        );



    }

    @Transactional
    public void completeMyProfile(Long userId, CompleteProfileReq req) {
        Users user = usersRepository.findById(userId)
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(
                req.name(),
                req.phoneNumber(),
                req.gender(),
                req.birth()
        );
    }

}