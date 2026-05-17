package com.tfg.backend.Cypher;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Base64.Decoder;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.backend.Cypher.Repositories.SignalAccountRepository;
import com.tfg.backend.Cypher.Repositories.SignalKyberPreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalOneTimePreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalSignedPreKeyRepository;
import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;
import com.tfg.backend.Cypher.dto.SignalBundleResponseDto;
import com.tfg.backend.Cypher.dto.SignalOneTimePreKeyDto;
import com.tfg.backend.Cypher.dto.SignalRefillRequestDto;
import com.tfg.backend.Cypher.dto.SignalRefillResponseDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import com.tfg.backend.Cypher.Entity.SignalSignedPreKey;
import com.tfg.backend.Cypher.Entity.SignalAccount;
import com.tfg.backend.Cypher.Entity.SignalKyberPreKey;
import com.tfg.backend.Cypher.Entity.SignalOneTimePreKey;

import jakarta.transaction.Transactional;

@Service
public class SignalService {

    private final UserService userService;

    private final SignalAccountRepository signalAccountRepository;
    private final SignalKyberPreKeyRepository signalKyberPreKeyRepository;
    private final SignalSignedPreKeyRepository signalSignedPreKeyRepository;
    private final SignalOneTimePreKeyRepository signalOneTimePreKeyRepository;

    public SignalService(
            UserService userService,
            SignalAccountRepository signalAccountRepository,
            SignalKyberPreKeyRepository signalKyberPreKeyRepository,
            SignalSignedPreKeyRepository signalSignedPreKeyRepository,
            SignalOneTimePreKeyRepository signalOneTimePreKeyRepository
            ) {
        this.userService = userService;
        this.signalAccountRepository = signalAccountRepository;
        this.signalKyberPreKeyRepository = signalKyberPreKeyRepository;
        this.signalSignedPreKeyRepository = signalSignedPreKeyRepository;
        this.signalOneTimePreKeyRepository = signalOneTimePreKeyRepository;
    }

    @Transactional
    public SignalBootstrapResponseDto replaceKeysBootstrap(Long userId, SignalBootstrapRequestDto request) {
        // Get user account
        boolean accountAlreadyExists = true;
        SignalAccount userAccount = signalAccountRepository.getByUserId(userId);
        if (userAccount == null) {
            accountAlreadyExists = false;
            userAccount = new SignalAccount();
            User user = userService.getById(userId);
            userAccount.SetUser(user);
        }

        // If the account is already bootstrapped (has active keys), do not recreate them.
        // This makes the endpoint idempotent: re-login does not regenerate Signal keys.
        if (accountAlreadyExists
                && userAccount.GetActiveSignedPreKey() != null
                && userAccount.GetActiveKyberPreKey() != null
                && userAccount.GetActiveIdentityKeyPublic() != null) {
            int existingOneTimeCount = 0;
            for (SignalOneTimePreKey existingOneTimePreKey : signalOneTimePreKeyRepository.getByUserId(userId)) {
                if (existingOneTimePreKey != null && existingOneTimePreKey.GetConsumedAt() == null) {
                    existingOneTimeCount++;
                }
            }
            return new SignalBootstrapResponseDto(
                    userAccount.GetActiveSignedPreKey().GetPreKeyId(),
                    userAccount.GetActiveKyberPreKey().GetPreKeyId(),
                    existingOneTimeCount
            );
        }

        // Verify that all required fields are present in the request, if not throw an exception
        if (request.getRegistrationId() == null ||
            request.getIdentityKeyPublicB64() == null ||
            request.getSignedPreKeyPublicB64() == null  ||
            request.getSignedPreKeySignatureB64() == null ||
            request.getKyberPreKeyPublicB64() == null  ||
            request.getKyberPreKeySignatureB64() == null ||
            request.getOneTimePreKeys() == null
                ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields in the request");
        }

        int registrationId;
        byte[] identityKeyPublicBytes;
        byte[] signedPreKeyPublicBytes;
        byte[] signedPreKeySignatureBytes;
        byte[] kyberPreKeyPublicBytes;
        byte[] kyberPreKeySignatureBytes;
        List<SignalOneTimePreKeyDto> oneTimePreKeysPublicBytesList;        
        Decoder decoder = java.util.Base64.getDecoder();

        // Check if base64 keys are valid and decode them, if not throw an exception
        try {
            registrationId = request.getRegistrationId();

            identityKeyPublicBytes = decoder.decode(request.getIdentityKeyPublicB64());
            signedPreKeyPublicBytes = decoder.decode(request.getSignedPreKeyPublicB64());
            signedPreKeySignatureBytes = decoder.decode(request.getSignedPreKeySignatureB64());
            kyberPreKeyPublicBytes = decoder.decode(request.getKyberPreKeyPublicB64());
            kyberPreKeySignatureBytes = decoder.decode(request.getKyberPreKeySignatureB64());

            oneTimePreKeysPublicBytesList = request.getOneTimePreKeys();
            for (SignalOneTimePreKeyDto signalOneTimePreKeyDto : oneTimePreKeysPublicBytesList) {
                decoder.decode(signalOneTimePreKeyDto.getPublicKeyB64());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(HttpStatus.BAD_REQUEST + " Invalid base64 keys");
        }

        // Check for duplicate pre-key IDs in one-time pre-keys, if found throw an exception
        Set<Integer> oneTimePreKeyIds = new HashSet<>();
        for (SignalOneTimePreKeyDto oneTimePreKey : oneTimePreKeysPublicBytesList) {
            if (!oneTimePreKeyIds.add(oneTimePreKey.getPreKeyId())) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate pre-key ID in one-time pre-keys");
            }
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

        for (SignalOneTimePreKey userOneTimePreKey : signalOneTimePreKeyRepository.getByUserId(userId)) {
            // Mark existing one-time pre-keys as consumed
            if (userOneTimePreKey != null) {
                userOneTimePreKey.SetConsumedAt(LocalDateTime.now());
                signalOneTimePreKeyRepository.save(userOneTimePreKey);
            }
        }

        // Create new keys
        // Create and persist new signed pre-key and kyber pre-key, capture managed instances
        SignalSignedPreKey newSignedPreKey = new SignalSignedPreKey();
        newSignedPreKey.SetPreKeyId(request.getSignedPreKeyId());
        newSignedPreKey.SetPublicKey(signedPreKeyPublicBytes);
        newSignedPreKey.SetSignature(signedPreKeySignatureBytes);
        newSignedPreKey.SetUser(userAccount.GetUser());
        SignalSignedPreKey savedSignedPreKey = signalSignedPreKeyRepository.save(newSignedPreKey);

        SignalKyberPreKey newKyberPreKey = new SignalKyberPreKey();
        newKyberPreKey.SetPreKeyId(request.getKyberPreKeyId());
        newKyberPreKey.SetPublicKey(kyberPreKeyPublicBytes);
        newKyberPreKey.SetSignature(kyberPreKeySignatureBytes);
        newKyberPreKey.SetUser(userAccount.GetUser());
        SignalKyberPreKey savedKyberPreKey = signalKyberPreKeyRepository.save(newKyberPreKey);

        // Update account with new keys using the managed instances returned by save
        userAccount.SetRegistrationId(registrationId);
        userAccount.SetActiveSignedPreKey(savedSignedPreKey);
        userAccount.SetActiveKyberPreKey(savedKyberPreKey);
        userAccount.SetActiveIdentityKeyPublic(identityKeyPublicBytes);
        if (!accountAlreadyExists) {
            signalAccountRepository.save(userAccount);
        }

        int oneTimePreKeysCount = 0;

        for (SignalOneTimePreKeyDto signalOneTimePreKeyDto : oneTimePreKeysPublicBytesList) {
            byte[] oneTimePreKeyPublicBytes = decoder.decode(signalOneTimePreKeyDto.getPublicKeyB64());

            SignalOneTimePreKey newOneTimePreKey = new SignalOneTimePreKey();
            newOneTimePreKey.SetPreKeyId(signalOneTimePreKeyDto.getPreKeyId());
            newOneTimePreKey.SetPublicKey(oneTimePreKeyPublicBytes);
            newOneTimePreKey.SetUser(userAccount.GetUser());
            signalOneTimePreKeyRepository.save(newOneTimePreKey);

            oneTimePreKeysCount++;
        }

        SignalBootstrapResponseDto response = new SignalBootstrapResponseDto(
                newSignedPreKey.GetPreKeyId(),
                newKyberPreKey.GetPreKeyId(),
                oneTimePreKeysCount
                );

        return response;
    }

    @Transactional
    public SignalRefillResponseDto refillOneTimePreKeys(Long userId, SignalRefillRequestDto request){

        // Get user account
        SignalAccount userAccount = signalAccountRepository.getByUserId(userId);
        if (userAccount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found");
        }

        // Check that one-time pre-keys are present in the request, if not throw an exception
        List<SignalOneTimePreKeyDto> oneTimePreKeysPublicBytesList;

        if (request.getOneTimePreKeys() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing one-time pre-keys in the request");
        } else {
            oneTimePreKeysPublicBytesList = request.getOneTimePreKeys();
        }

        // Check for duplicate pre-key IDs in one-time pre-keys, if found throw an exception
        Set<Integer> oneTimePreKeyIds = new HashSet<>();
        for (SignalOneTimePreKeyDto oneTimePreKey : oneTimePreKeysPublicBytesList) {
            if (!oneTimePreKeyIds.add(oneTimePreKey.getPreKeyId())) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate pre-key ID in one-time pre-keys");
            }
        }

        // Check if base64 keys are valid and decode them, if not throw an exception
        Decoder decoder = java.util.Base64.getDecoder();
        int oneTimePreKeysCount = 0;

        for (SignalOneTimePreKeyDto oneTimePreKeyDto : oneTimePreKeysPublicBytesList) {
            byte[] oneTimePreKeyPublicBytes;
            try {
                oneTimePreKeyPublicBytes = decoder.decode(oneTimePreKeyDto.getPublicKeyB64());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(HttpStatus.BAD_REQUEST + " Invalid base64 keys");
            }

            SignalOneTimePreKey newOneTimePreKey = new SignalOneTimePreKey();
            newOneTimePreKey.SetPreKeyId(oneTimePreKeyDto.getPreKeyId());
            newOneTimePreKey.SetPublicKey(oneTimePreKeyPublicBytes);
            newOneTimePreKey.SetUser(userAccount.GetUser());
            signalOneTimePreKeyRepository.save(newOneTimePreKey);
            
            oneTimePreKeysCount++;
        }

        return new SignalRefillResponseDto(oneTimePreKeysCount, oneTimePreKeysPublicBytesList.size() - oneTimePreKeysCount);
    }

    @Transactional
    public SignalBundleResponseDto getUserBundle(Long userId) {
        // Get user user account
        SignalAccount userAccount = signalAccountRepository.getByUserId(userId);
        if (userAccount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found");
        }

        // Check that user has active keys, if not throw an exception
        if (userAccount.GetActiveIdentityKeyPublic() == null ||
            userAccount.GetActiveSignedPreKey() == null ||
            userAccount.GetActiveKyberPreKey() == null
        ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User keys not found");
        }

        SignalOneTimePreKey oneTimePreKey = signalOneTimePreKeyRepository.getUnconsumedPreKeyByUserId(userId);
        if (oneTimePreKey != null) {
            oneTimePreKey.SetConsumedAt(LocalDateTime.now());
            signalOneTimePreKeyRepository.save(oneTimePreKey);
        }

        String oneTimePreKeyString = java.util.Base64.getEncoder().encodeToString(oneTimePreKey.GetPublicKey());
        String activeSignedPreKeyString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveSignedPreKey().GetPublicKey());
        String activeSignedPreKeySignatureString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveSignedPreKey().GetSignature());
        String activeIdentityKeyString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveIdentityKeyPublic());
        String activeKyberPreKeyString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveKyberPreKey().GetPublicKey());
        String activeKyberPreKeySignatureString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveKyberPreKey().GetSignature());

        return new SignalBundleResponseDto(
                userAccount.GetRegistrationId(),
                oneTimePreKey != null ? userAccount.GetActiveKyberPreKey().GetPreKeyId(): -1,
                oneTimePreKeyString,
                userAccount.GetActiveSignedPreKey().GetPreKeyId(),
                activeSignedPreKeyString,
                activeSignedPreKeySignatureString,
                activeIdentityKeyString,
                userAccount.GetActiveKyberPreKey().GetPreKeyId(),
                activeKyberPreKeyString,
                activeKyberPreKeySignatureString
        );
    }
}
