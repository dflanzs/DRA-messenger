package com.tfg.backend.Cypher;

import com.tfg.backend.Cypher.Entity.SignalAccount;
import com.tfg.backend.Cypher.Entity.SignalKyberPreKey;
import com.tfg.backend.Cypher.Entity.SignalOneTimePreKey;
import com.tfg.backend.Cypher.Entity.SignalSignedPreKey;
import com.tfg.backend.Cypher.Repositories.SignalAccountRepository;
import com.tfg.backend.Cypher.Repositories.SignalKyberPreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalOneTimePreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalSignedPreKeyRepository;
import com.tfg.backend.Cypher.dto.SignalBundleResponseDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignalServiceTest {

    private static final Long USER_ID = 1L;
    private static final int KYBER_PRE_KEY_ID = 999;
    private static final int ONE_TIME_PRE_KEY_ID = 777;

    private final UserService userService = mock(UserService.class);
    private final SignalAccountRepository signalAccountRepository = mock(SignalAccountRepository.class);
    private final SignalKyberPreKeyRepository signalKyberPreKeyRepository = mock(SignalKyberPreKeyRepository.class);
    private final SignalSignedPreKeyRepository signalSignedPreKeyRepository = mock(SignalSignedPreKeyRepository.class);
    private final SignalOneTimePreKeyRepository signalOneTimePreKeyRepository = mock(SignalOneTimePreKeyRepository.class);

    private final SignalService signalService = new SignalService(
            userService,
            signalAccountRepository,
            signalKyberPreKeyRepository,
            signalSignedPreKeyRepository,
            signalOneTimePreKeyRepository
    );

    private SignalAccount accountWithActiveKeys() {
        User user = new User();
        SignalSignedPreKey signedPreKey = new SignalSignedPreKey(42, new byte[]{1}, new byte[]{2}, user);
        SignalKyberPreKey kyberPreKey = new SignalKyberPreKey(KYBER_PRE_KEY_ID, new byte[]{3}, new byte[]{4}, user);
        return new SignalAccount(signedPreKey, kyberPreKey, user, 100, new byte[]{5});
    }

    @Test
    void getUserBundle_returnsConsumedOneTimePreKeyId_notKyberId() {
        when(signalAccountRepository.getByUserId(USER_ID)).thenReturn(accountWithActiveKeys());

        SignalOneTimePreKey oneTimePreKey =
                new SignalOneTimePreKey(ONE_TIME_PRE_KEY_ID, new byte[]{6}, new User());
        when(signalOneTimePreKeyRepository.getUnconsumedPreKeyByUserId(eq(USER_ID), any())).thenReturn(oneTimePreKey);

        SignalBundleResponseDto bundle = signalService.getUserBundle(USER_ID);

        assertEquals(ONE_TIME_PRE_KEY_ID, bundle.getPreKeyId());
        assertEquals(KYBER_PRE_KEY_ID, bundle.getKyberPreKeyId());
    }

    @Test
    void getUserBundle_returnsDegradedBundle_whenNoOneTimePreKeyLeft() {
        when(signalAccountRepository.getByUserId(USER_ID)).thenReturn(accountWithActiveKeys());
        when(signalOneTimePreKeyRepository.getUnconsumedPreKeyByUserId(eq(USER_ID), any())).thenReturn(null);

        SignalBundleResponseDto bundle = signalService.getUserBundle(USER_ID);

        assertEquals(-1, bundle.getPreKeyId());
        assertNull(bundle.getPreKeyPublicB64());
        assertEquals(KYBER_PRE_KEY_ID, bundle.getKyberPreKeyId());
    }
}
