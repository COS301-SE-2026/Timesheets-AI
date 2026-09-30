package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import timesheets.domain.User;
import timesheets.domain.UserMfa;
import timesheets.dto.response.MfaSetupResponse;
import timesheets.repository.UserMfaRepository;
import timesheets.repository.UserRepository;
import timesheets.util.TotpUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MfaService Unit Tests")
class MfaServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserMfaRepository userMfaRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private TotpUtils totpUtils;

  @InjectMocks private MfaService mfaService;

  private final UUID userId = UUID.randomUUID();
  private User user;

  private UserMfa mfa(boolean enabled) {
    return UserMfa.builder().userId(userId).secretKey("SECRET").isEnabled(enabled).build();
  }

  @BeforeEach
  void setUp() {
    user = mock(User.class);
    when(user.getEmail()).thenReturn("john@momentum.co.za");
    when(user.getPasswordHash()).thenReturn("hash");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.empty());
  }

  @Nested
  @DisplayName("setup")
  class SetupTests {

    @Test
    @DisplayName("should create a disabled mfa record and return the qr url")
    void createsNewMfa() {
      when(totpUtils.generateSecret()).thenReturn("NEWSECRET");
      when(totpUtils.generateQrCodeUrl("NEWSECRET", "john@momentum.co.za", "Timesheets AI"))
          .thenReturn("otpauth://totp/x");

      MfaSetupResponse response = mfaService.setup(userId);

      assertThat(response.getSecretKey()).isEqualTo("NEWSECRET");
      assertThat(response.getQrCodeUrl()).isEqualTo("otpauth://totp/x");
      verify(userMfaRepository).save(any(UserMfa.class));
    }

    @Test
    @DisplayName("should replace the secret of a disabled mfa record")
    void replacesSecretWhenDisabled() {
      UserMfa existing = mfa(false);
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
      when(totpUtils.generateSecret()).thenReturn("NEWSECRET");

      mfaService.setup(userId);

      assertThat(existing.getSecretKey()).isEqualTo("NEWSECRET");
      assertThat(existing.getIsEnabled()).isFalse();
      verify(userMfaRepository).save(existing);
    }

    @Test
    @DisplayName("should throw when mfa is already enabled")
    void alreadyEnabled() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(true)));

      assertThatThrownBy(() -> mfaService.setup(userId)).isInstanceOf(StateConflictException.class);
      verify(userMfaRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when the user does not exist")
    void userMissing() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> mfaService.setup(userId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("verifySetup")
  class VerifySetupTests {

    @Test
    @DisplayName("should enable mfa for a valid code")
    void enablesOnValidCode() {
      UserMfa existing = mfa(false);
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
      when(totpUtils.verifyCode("SECRET", "123456")).thenReturn(true);

      mfaService.verifySetup(userId, "123456");

      assertThat(existing.getIsEnabled()).isTrue();
      verify(userMfaRepository).save(existing);
    }

    @Test
    @DisplayName("should throw when setup was never started")
    void notStarted() {
      assertThatThrownBy(() -> mfaService.verifySetup(userId, "123456"))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("should throw when mfa is already enabled")
    void alreadyEnabled() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(true)));

      assertThatThrownBy(() -> mfaService.verifySetup(userId, "123456"))
          .isInstanceOf(StateConflictException.class);
    }

    @Test
    @DisplayName("should throw for an invalid code")
    void invalidCode() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(false)));
      when(totpUtils.verifyCode("SECRET", "000000")).thenReturn(false);

      assertThatThrownBy(() -> mfaService.verifySetup(userId, "000000"))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Invalid authentication code");
      verify(userMfaRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("disable")
  class DisableTests {

    @Test
    @DisplayName("should disable mfa with the correct password")
    void disables() {
      UserMfa existing = mfa(true);
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
      when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

      mfaService.disable(userId, "pw");

      assertThat(existing.getIsEnabled()).isFalse();
      verify(userMfaRepository).save(existing);
    }

    @Test
    @DisplayName("should throw for sso accounts without a password")
    void ssoAccount() {
      when(user.getPasswordHash()).thenReturn(null);

      assertThatThrownBy(() -> mfaService.disable(userId, "pw"))
          .isInstanceOf(StateConflictException.class)
          .hasMessageContaining("SSO");
    }

    @Test
    @DisplayName("should throw for a wrong password")
    void wrongPassword() {
      when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

      assertThatThrownBy(() -> mfaService.disable(userId, "bad"))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Password is incorrect");
    }

    @Test
    @DisplayName("should throw when mfa is not configured")
    void notConfigured() {
      when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

      assertThatThrownBy(() -> mfaService.disable(userId, "pw"))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("MFA is not configured");
    }

    @Test
    @DisplayName("should throw when mfa is already disabled")
    void alreadyDisabled() {
      when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(false)));

      assertThatThrownBy(() -> mfaService.disable(userId, "pw"))
          .isInstanceOf(StateConflictException.class);
    }

    @Test
    @DisplayName("should throw when the user does not exist")
    void userMissing() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> mfaService.disable(userId, "pw"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("verifyLogin")
  class VerifyLoginTests {

    @Test
    @DisplayName("should return the user for a valid code")
    void returnsUser() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(true)));
      when(totpUtils.verifyCode("SECRET", "123456")).thenReturn(true);

      assertThat(mfaService.verifyLogin(userId, "123456")).isSameAs(user);
    }

    @Test
    @DisplayName("should throw for an invalid code")
    void invalidCode() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(true)));
      when(totpUtils.verifyCode("SECRET", "000000")).thenReturn(false);

      assertThatThrownBy(() -> mfaService.verifyLogin(userId, "000000"))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("should throw when mfa is not enabled")
    void notEnabled() {
      when(userMfaRepository.findByUserId(userId)).thenReturn(Optional.of(mfa(false)));

      assertThatThrownBy(() -> mfaService.verifyLogin(userId, "123456"))
          .isInstanceOf(StateConflictException.class);
    }

    @Test
    @DisplayName("should throw when mfa is not configured")
    void notConfigured() {
      assertThatThrownBy(() -> mfaService.verifyLogin(userId, "123456"))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("should throw when the user does not exist")
    void userMissing() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> mfaService.verifyLogin(userId, "123456"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
