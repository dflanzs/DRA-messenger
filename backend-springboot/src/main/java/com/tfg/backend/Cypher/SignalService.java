package com.tfg.backend.Cypher;

import java.util.List;
import java.util.Base64.Decoder;

import org.springframework.stereotype.Service;

import com.tfg.backend.Cypher.Repositories.SignalAccountRepository;
import com.tfg.backend.Cypher.Repositories.SignalKyberPreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalOneTimePreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalSignedPreKeyRepository;
import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;
import com.tfg.backend.Cypher.dto.SignalOneTimePreKeyDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import com.tfg.backend.Cypher.Entity.SignalSignedPreKey;
import com.tfg.backend.Cypher.Entity.SignalAccount;
import com.tfg.backend.Cypher.Entity.SignalKyberPreKey;
import com.tfg.backend.Cypher.Entity.SignalOneTimePreKey;

import jakarta.transaction.Transactional;

@Service
public class SignalService {

    private final SignalAccountRepository signalAccountRepository;
    private final SignalKyberPreKeyRepository signalKyberPreKeyRepository;
    private final SignalSignedPreKeyRepository signalSignedPreKeyRepository;
    private final SignalOneTimePreKeyRepository signalOneTimePreKeyRepository;
    private final UserService userService;

    public SignalService(
            SignalAccountRepository signalAccountRepository,
            SignalKyberPreKeyRepository signalKyberPreKeyRepository,
            SignalSignedPreKeyRepository signalSignedPreKeyRepository,
            SignalOneTimePreKeyRepository signalOneTimePreKeyRepository,
            UserService userService
            ) {
        this.signalAccountRepository = signalAccountRepository;
        this.signalKyberPreKeyRepository = signalKyberPreKeyRepository;
        this.signalSignedPreKeyRepository = signalSignedPreKeyRepository;
        this.signalOneTimePreKeyRepository = signalOneTimePreKeyRepository;
        this.userService = userService;
    }

    @Transactional
    public SignalBootstrapResponseDto replaceKeysBootstrap(Long userId, SignalBootstrapRequestDto request) {
        // Get user account
        SignalAccount userAccount = signalAccountRepository.getByUserId(userId);
        if (userAccount == null) {
            userAccount = new SignalAccount();
            User user = userService.getById(userId);
            userAccount.SetUser(user);
        }

        // Retire existing keys
        SignalSignedPreKey userSignedPreKey = signalSignedPreKeyRepository.getByUserId(userId);
        if (userSignedPreKey != null) {
            userSignedPreKey.Retire();
            signalSignedPreKeyRepository.save(userSignedPreKey);
        }

        SignalKyberPreKey userKyberPreKey = signalKyberPreKeyRepository.getByUserId(userId);
        if (userKyberPreKey != null) {
            userKyberPreKey.Retire();
            signalKyberPreKeyRepository.save(userKyberPreKey);
        }

        // Decode Base64 keys
        Decoder decoder = java.util.Base64.getDecoder();

        int registrationId = request.getRegistrationId();
        byte[] identityKeyPublicBytes = decoder.decode(request.getIdentityKeyPublicB64());
        byte[] signedPreKeyPublicBytes = decoder.decode(request.getSignedPreKeyPublicB64());
        byte[] signedPreKeySignatureBytes = decoder.decode(request.getSignedPreKeySignatureB64());
        byte[] kyberPreKeyPublicBytes = decoder.decode(request.getKyberPreKeyPublicB64());
        byte[] kyberPreKeySignatureBytes = decoder.decode(request.getKyberPreKeySignatureB64());
        List<SignalOneTimePreKeyDto> oneTimePreKeysPublicBytesList = request.getOneTimePreKeys();

        // Create new keys
        SignalSignedPreKey newSignedPreKey = new SignalSignedPreKey();
        newSignedPreKey.SetPublicKey(signedPreKeyPublicBytes);
        newSignedPreKey.SetSignature(signedPreKeySignatureBytes);
        newSignedPreKey.SetUser(userAccount.GetUser());
        signalSignedPreKeyRepository.save(newSignedPreKey);

        SignalKyberPreKey newKyberPreKey = new SignalKyberPreKey();
        newKyberPreKey.SetPublicKey(kyberPreKeyPublicBytes);
        newKyberPreKey.SetSignature(kyberPreKeySignatureBytes);
        newKyberPreKey.SetUser(userAccount.GetUser());
        signalKyberPreKeyRepository.save(newKyberPreKey);

        // Update account with new keys
        userAccount.SetRegistrationId(registrationId);
        userAccount.SetActiveSignedPreKey(newSignedPreKey);
        userAccount.SetActiveKyberPreKey(newKyberPreKey);
        userAccount.SetActiveIdentityKeyPublic(identityKeyPublicBytes);
        signalAccountRepository.save(userAccount);

        for (SignalOneTimePreKeyDto signalOneTimePreKeyDto : oneTimePreKeysPublicBytesList) {
            byte[] oneTimePreKeyPublicBytes = decoder.decode(signalOneTimePreKeyDto.getPublicKeyB64());

            SignalOneTimePreKey newOneTimePreKey = new SignalOneTimePreKey();
            newOneTimePreKey.SetPublicKey(oneTimePreKeyPublicBytes);
            newOneTimePreKey.SetUser(userAccount.GetUser());
            signalOneTimePreKeyRepository.save(newOneTimePreKey);
        }

        SignalBootstrapResponseDto response = new SignalBootstrapResponseDto(
                newKyberPreKey.GetPreKeyId(),
                newSignedPreKey.GetPreKeyId(),
                oneTimePreKeysPublicBytesList.size()
                );

        return response;
    }
}
