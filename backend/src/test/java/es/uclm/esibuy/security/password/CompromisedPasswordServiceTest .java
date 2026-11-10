
package es.uclm.esibuy.security.password;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompromisedPasswordServiceTest {

        private static final String PASSWORD = "password";
        private static final String PREFIX = "5BAA6";
        private static final String SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

        @Mock
        private HibpPasswordClient hibpPasswordClient;

        private CompromisedPasswordService service;

        @BeforeEach
        void setUp() {
                service = new CompromisedPasswordService(hibpPasswordClient);
        }

        @Test
        void shouldRejectNullPassword() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.validatePassword(null));

                verifyNoInteractions(hibpPasswordClient);
        }

        @Test
        void shouldRejectBlankPassword() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.validatePassword("   "));

                verifyNoInteractions(hibpPasswordClient);
        }

        @Test
        void shouldUseCorrectSha1Prefix() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of());

                service.validatePassword(PASSWORD);

                verify(hibpPasswordClient).findPasswordHashes(PREFIX);
        }

        @Test
        void shouldAcceptPasswordWhenHashIsNotFound() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(
                                                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:12",
                                                "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB:4"));

                assertDoesNotThrow(() -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectPasswordWhenHashHasPositiveCount() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX + ":1"));

                assertThrows(
                                CompromisedPasswordException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectPasswordWhenHashHasLargePositiveCount() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX + ":123456"));

                assertThrows(
                                CompromisedPasswordException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldAcceptPasswordWhenMatchingHashHasZeroCount() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX + ":0"));

                assertDoesNotThrow(() -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldIgnoreMatchingHashWithDifferentSuffix() {
                String differentSuffix = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(differentSuffix + ":100"));

                assertDoesNotThrow(() -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldAcceptLowercaseHashSuffix() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX.toLowerCase() + ":0"));

                assertDoesNotThrow(() -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectMalformedResponseEntry() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of("RESPUESTA_INVALIDA"));

                assertThrows(
                                PasswordCheckUnavailableException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectResponseWithInvalidSuffix() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of("INVALIDO:1"));

                assertThrows(
                                PasswordCheckUnavailableException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectResponseWithInvalidCount() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX + ":INVALIDO"));

                assertThrows(
                                PasswordCheckUnavailableException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldRejectResponseWithCountOutsideLongRange() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenReturn(List.of(SUFFIX + ":999999999999999999999999999999"));

                assertThrows(
                                PasswordCheckUnavailableException.class,
                                () -> service.validatePassword(PASSWORD));
        }

        @Test
        void shouldPropagateUnavailableServiceException() {
                when(hibpPasswordClient.findPasswordHashes(PREFIX))
                                .thenThrow(new PasswordCheckUnavailableException());

                assertThrows(
                                PasswordCheckUnavailableException.class,
                                () -> service.validatePassword(PASSWORD));
        }
}
