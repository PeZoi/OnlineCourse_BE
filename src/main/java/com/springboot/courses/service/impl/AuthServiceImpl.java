package com.springboot.courses.service.impl;

import com.springboot.courses.entity.MessageErrorCustomer;
import com.springboot.courses.entity.Role;
import com.springboot.courses.entity.User;
import com.springboot.courses.exception.BlogApiException;
import com.springboot.courses.exception.ResourceNotFoundException;
import com.springboot.courses.payload.MessageNotice;
import com.springboot.courses.payload.auth.JWTAuthResponse;
import com.springboot.courses.payload.auth.LoginDto;
import com.springboot.courses.payload.user.UserRequest;
import com.springboot.courses.payload.user.UserResponse;
import com.springboot.courses.payload.user.UserReturnJwt;
import com.springboot.courses.payload.validate.CheckValidateCustomerRequest;
import com.springboot.courses.payload.validate.CheckValidateCustomerResponse;
import com.springboot.courses.repository.RoleRepository;
import com.springboot.courses.repository.UserRepository;
import com.springboot.courses.security.JwtTokenProvider;
import com.springboot.courses.service.AuthService;
import com.springboot.courses.utils.AppConstants;
import com.springboot.courses.utils.Utils;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Transactional
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private AuthenticationManagerBuilder authenticationManagerBuilder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ModelMapper modelMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public JWTAuthResponse login(LoginDto loginDto) {

        try {
//            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
//                    loginDto.getEmail(), loginDto.getPassword()
//            );
//
//            Authentication authenticationTmp = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtTokenProvider.generateToken(authentication);

            JWTAuthResponse jwtAuthResponse = new JWTAuthResponse();
            jwtAuthResponse.setAccessToken(token);

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

            UserReturnJwt userReturnJwt = modelMapper.map(user, UserReturnJwt.class);
            userReturnJwt.setRoleName(user.getRole().getName());
            jwtAuthResponse.setUser(userReturnJwt);

            return jwtAuthResponse;
        }catch (BadCredentialsException e) {
            throw new BlogApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }catch (DisabledException d){
            throw new BlogApiException(HttpStatus.UNAUTHORIZED, "Vui lòng xác nhận tài khoản email để được đăng nhập!");
        }
    }

    @Override
    public List<CheckValidateCustomerResponse> checkInfoOfCustomer(CheckValidateCustomerRequest request) {
        List<CheckValidateCustomerResponse> listResponses = new ArrayList<>();
        if(userRepository.existsUserByEmail(request.getEmail())){
            listResponses.add(new CheckValidateCustomerResponse(MessageErrorCustomer.EMAIL.noteMessageError(), MessageErrorCustomer.EMAIL));
        }
        if(userRepository.existsUserByUsername(request.getUsername())){
            listResponses.add(new CheckValidateCustomerResponse(MessageErrorCustomer.USERNAME.noteMessageError(), MessageErrorCustomer.USERNAME));
        }
        if (userRepository.existsUserByPhoneNumber(request.getPhoneNumber())){
            listResponses.add(new CheckValidateCustomerResponse(MessageErrorCustomer.PHONE_NUMBER.noteMessageError(), MessageErrorCustomer.PHONE_NUMBER));
        }

        return listResponses;
    }

    @Override
    public UserResponse register(UserRequest userRequest) {
        Role role = roleRepository.findByName("ROLE_CUSTOMER").get();
        String randomCode = RandomString.make(64);

        User user = modelMapper.map(userRequest, User.class);

        user.setPhoto("https://res.cloudinary.com/dqnoopa0x/image/upload/v1712482876/ooozzfj7t7p1zokgonni.jpg");
        user.setVerificationCode(randomCode);
        user.setEnabled(false);
        user.setCreatedTime(new Date());
        user.setRole(role);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String verifyURL = AppConstants.LOCALHOST + "/auth/verify?code=" + user.getVerificationCode() + "&email=" + user.getEmail();

        Utils.sendEmail(verifyURL, AppConstants.SUBJECT_REGISTER, AppConstants.CONTENT_REGISTER, user);
        User savedUser = userRepository.save(user);

        UserResponse userResponse = modelMapper.map(savedUser, UserResponse.class);
        userResponse.setRoleName(role.getName());

        return userResponse;
    }

    @Override
    public void requestPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String token = RandomString.make(30);
        user.setResetPasswordToken(token);
        String url = AppConstants.LOCALHOST + "/auth/request-password?token=" + token;
        Utils.sendEmail(url, AppConstants.SUBJECT_RESET, AppConstants.CONTENT_RESET, user);
        userRepository.save(user);
    }

    @Override
    public MessageNotice verify(String verification, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", email));

        if(user.getVerificationCode() == null){
            return new MessageNotice(false, "Tài khoản đã được kích hoạt");
        }else{
            if(user.getVerificationCode().equals(verification)){
                userRepository.enable(user.getId());
                return new MessageNotice(true, "Tài khoản kích hoạt thành công");
            }else{
                return new MessageNotice(false, "Sai mã kích hoạt");
            }
        }
    }

    @Override
    public UserResponse findByResetPasswordToken(String token) {
        User user = userRepository.findUserByResetPasswordToken(token);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        userResponse.setRoleName(user.getRole().getName());
        return userResponse;
    }

    @Override
    public void updatePassword(String token, String password) {
        User user = userRepository.findUserByResetPasswordToken(token);
        if(user == null){
            throw new BlogApiException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setResetPasswordToken(null);

        userRepository.save(user);
    }
}
