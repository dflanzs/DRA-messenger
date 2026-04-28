package com.tfg.backend.Cypher;

import java.beans.Encoder;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Base64.Decoder;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.backend.Cypher.Repositories.SignalAccountRepository;
import com.tfg.backend.Cypher.Repositories.SignalKyberPreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalOneTimePreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalSignedPreKeyRepository;
import com.tfg.backend.Cypher.Repositories.SignalEnvelopeRepository;
import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;
import com.tfg.backend.Cypher.dto.SignalBundleResponseDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageResponseDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageWSDto;
import com.tfg.backend.Cypher.dto.SignalOneTimePreKeyDto;
import com.tfg.backend.Cypher.dto.SignalRefillRequestDto;
import com.tfg.backend.Cypher.dto.SignalRefillResponseDto;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import com.tfg.backend.Cypher.Entity.SignalSignedPreKey;
import com.tfg.backend.Cypher.Entity.SignalAccount;
import com.tfg.backend.Cypher.Entity.SignalEnvelope;
import com.tfg.backend.Cypher.Entity.SignalKyberPreKey;
import com.tfg.backend.Cypher.Entity.SignalOneTimePreKey;

import jakarta.transaction.Transactional;

@Service
public class SignalService {

    private final UserService userService;
    private final OneToOneChat oneToOneChat;

    private final SignalAccountRepository signalAccountRepository;
    private final SignalKyberPreKeyRepository signalKyberPreKeyRepository;
    private final SignalSignedPreKeyRepository signalSignedPreKeyRepository;
    private final SignalOneTimePreKeyRepository signalOneTimePreKeyRepository;
    private final SignalEnvelopeRepository signalEnvelopeRepository;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public SignalService(
            UserService userService,
            OneToOneChat oneToOneChat,
            SignalAccountRepository signalAccountRepository,
            SignalKyberPreKeyRepository signalKyberPreKeyRepository,
            SignalSignedPreKeyRepository signalSignedPreKeyRepository,
            SignalOneTimePreKeyRepository signalOneTimePreKeyRepository,
            SignalEnvelopeRepository signalEnvelopeRepository,
            OneToOneChatRepository oneToOneChatRepository,
            SimpMessagingTemplate simpMessagingTemplate
            ) {
        this.userService = userService;
        this.oneToOneChat = oneToOneChat;
        this.signalAccountRepository = signalAccountRepository;
        this.signalKyberPreKeyRepository = signalKyberPreKeyRepository;
        this.signalSignedPreKeyRepository = signalSignedPreKeyRepository;
        this.signalOneTimePreKeyRepository = signalOneTimePreKeyRepository;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
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
            userOneTimePreKey.SetConsumedAt(LocalDateTime.now());
            signalOneTimePreKeyRepository.save(userOneTimePreKey);
            
            if (userOneTimePreKey != null) {
                userOneTimePreKey.SetConsumedAt(LocalDateTime.now());
                signalOneTimePreKeyRepository.save(userOneTimePreKey);
            }
        }

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

        int oneTimePreKeysCount = 0;

        for (SignalOneTimePreKeyDto signalOneTimePreKeyDto : oneTimePreKeysPublicBytesList) {
            byte[] oneTimePreKeyPublicBytes = decoder.decode(signalOneTimePreKeyDto.getPublicKeyB64());

            SignalOneTimePreKey newOneTimePreKey = new SignalOneTimePreKey();
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
        String activeKyberPreKeySignatureString = java.util.Base64.getEncoder().encodeToString(userAccount.GetActiveKyberPreKey().GetSignature();

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

    @Transactional
    public SignalDirectMessageResponseDto storeDirectMessage(Long senderUserId, SignalDirectMessageRequestDto request) {
        // Check if recipient exists
        User recipient = userService.getById(request.getRecipientUserId());
        if (recipient == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Recipient user does not exist");
        }

        // Check if conversationId corresponds to a valid one-to-one chat between sender and recipient
        OneToOneChat oneToOneChat = oneToOneChatRepository.findChatBetweenUsers(senderUserId, request.getRecipientUserId());
        if (request.getConversationId() != oneToOneChat.getId()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid conversation ID");
        }

        User sender = userService.getById(senderUserId);

        Decoder decoder = java.util.Base64.getDecoder();
        SignalEnvelope signalEnvelope = new SignalEnvelope(
                sender,
                recipient,
                oneToOneChat,
                decoder.decode(request.getCypherTextB64()),
                request.getCypherTextType()
                );
        
        if (signalEnvelope != null && signalEnvelope.IsConversationConsistent()) {
            signalEnvelopeRepository.save(signalEnvelope);
        }

        // Send message through WebSocket to recipient
        SignalDirectMessageWSDto wsMessage = new SignalDirectMessageWSDto(
                    signalEnvelope.getId(),
                    signalEnvelope,
                    signalEnvelope.getSender().getId(),
                    signalEnvelope.getReceiver().getId(),
                    signalEnvelope.getConversationType(),
                    request.getCypherTextType(),
                    Base64.getEncoder().encodeToString(signalEnvelope.getCypherText()),
                    signalEnvelope.getCreatedAt()
                );
        
        simpMessagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/signal-messages", wsMessage);

        return new SignalDirectMessageResponseDto(signalEnvelope.getId(), signalEnvelope.getCreatedAt());
    }
}
