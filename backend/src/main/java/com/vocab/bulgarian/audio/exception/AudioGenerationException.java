package com.vocab.bulgarian.audio.exception;

/**
 * Custom runtime exception for TTS audio generation failures.
 * Thrown when edge-tts or audio storage operations fail.
 */
public class AudioGenerationException extends RuntimeException {

    public AudioGenerationException(String message) {
        super(message);
    }

    public AudioGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
