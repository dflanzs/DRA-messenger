package com.tfg.backend.Message;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.SignalEnvelope.SignalEnvelope;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeController;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeService;
import com.tfg.backend.User.User;

@WebMvcTest(SignalEnvelopeController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SignalEnvelopeService signalEnvelopeService;

    @Test
    void getById_returnsEnvelope_whenExists() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);
        byte[] cypherText = "Hola".getBytes();
        Short cypherTextType = 1;
        SignalEnvelope envelope = buildSignalEnvelope(10L, sender, receiver, chat, cypherText, cypherTextType);

        org.mockito.Mockito.when(signalEnvelopeService.getById(10L)).thenReturn(envelope);

        mockMvc.perform(get("/api/messages/10"))
                .andExpect(status().isOk());
    }

    @Test
    void get_returnsNotFound_whenMessageDoesNotExist() throws Exception {
        org.mockito.Mockito.when(signalEnvelopeService.getById(99L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/messages/99"))
                .andExpect(status().isNotFound());
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail(email);
        user.setPassword("StrongP@ss1");
        user.setPublicKey("public-key-" + id);
        return user;
    }

    private SignalEnvelope buildSignalEnvelope(Long id, User sender, User receiver, OneToOneChat chat, byte[] cypherText, Short cypherTextType) {
        SignalEnvelope envelope = new SignalEnvelope(sender, receiver, chat, cypherText, cypherTextType);
        try {
            java.lang.reflect.Field idField = SignalEnvelope.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(envelope, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return envelope;
    }
}