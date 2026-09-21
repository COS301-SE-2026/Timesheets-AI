package timesheets.service;

import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.User;
import timesheets.domain.UserMfa;
import timesheets.dto.response.MfaSetupResponse;
import timesheets.repository.UserMfaRepository;
import timesheets.repository.UserRepository;
import timesheets.util.TopUtils;

@Service
@RequiredArgsConstructor
public class MfaService {
    private final UserRepository userRepository;
    private final UserMfaRepository userMfaRepository;
    private final TotpUtils totpUtils; 

    @Transactional
    public MfaSetupResponse setup(UUID userId){
        User user= userRepository
            .findById(userId)
            .orElseThrow(
                ()-> new ResourceNotFoundException("User not found")
            );

        UserMfa existingMfa= userMfaRepository.findByUserId(userId).orElse(null);

        // when the mfa is already on then it should not get another secret
        if(existingMfa !=null && Boolean.TRUE.equals(existingMfa.getIsEnabled())){
            throw new StateConflictException("MFA is already enabled");
        }

        // create a fresh secret key when we starting up
        String secretKey= totpUtils.generateSecret();

        UserMfa userMfa;

        if(existingMfa == null){
            userMfa= UserMfa.builder()
                            .userId(userId)
                            .secretKey(secretKey)
                            .isEnabled(false)
                            .build();
        }else{
            existingMfa.setSecretKey(secretKey);
            existingMfa.setIsEnabled(false);
            userMfa= existingMfa;
        }

        userMfaRepository.save(userMfa);

        String qrCodeUrl=
            totpUtils.generateQrCodeUrl(
                secretKey,
                user.getEmail()
            );

        return MfaSetupResponse.builder()
            .secretKey(secretKey)
            .qrCodeUrl(qrCodeUrl)
            .message(
                "Scan the QR code with your authenticator app and enter the 6-digit code."
            )
            .build();
    }

    @Transactional
    public void verifySetup(UUID userId, String totpCode){
        UserMfa userMfa=userMfaRepository
                        .findByUserId(userId)
                        .orElseThrow(
                            ()-> new BadRequestException("MFA setup has not been started")
                        );

        if(Boolean.TRUE.equals(userMfa.getIsEnabled())){
            throw new StateConflictException("MFA is already enabled");
        }

        boolean valid= totpUtils.verifyCode(
            userMfa.getSecretKey(),
            totpCode
        );

        if(!valid){
            throw new BadRequestException(
                "Invalid authentication code"
            );
        }

        userMfa.setIsEnabled(true);
        userMfaRepository.save(userMfa);
    }

    @Transactional
    public void diasble(UUID userId, String password){
        User user= userRepository
                    .findById(userId)
                    .orElseThrow(
                        ()-> new ResourceNotFoundException("User not found")
                    );

        if(user.getPasswordHash()== null){
            throw new StateConflictException(
                "This account uses SSO. MFA cannot be disabled with a password."
            );
        }

        if(!passwordEncoder.matches(password, user.getPasswordHash())){
            throw new BadRequestException("Password is incorrect");
        }

        UserMfa userMfa= userMfaRepository
                        .findByUserId(userId)
                        .orElseThrow(
                            ()-> new BadRequestException("MFA is not configured")
                        );

        if(!Boolean.TRUE.equals(userMfa.getIsEnabled())){
            throw new StateConflictException("MFA is already disabled");
        }

        userMfa.setIsEnabled(false);
        userMfaRepository.save(userMfa);
    }
}
