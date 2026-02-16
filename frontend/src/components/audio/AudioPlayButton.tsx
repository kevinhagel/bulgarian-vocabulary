import { useState, useRef } from 'react';
import api from '@/lib/api';

// Module-level cache to avoid re-generating audio for the same text
const audioCache = new Map<string, string>();

interface AudioPlayButtonProps {
  text: string;
  className?: string;
}

/**
 * Button component that generates and plays audio on demand.
 * Caches generated audio filenames to avoid repeated API calls.
 *
 * @param text - The Bulgarian text to generate audio for
 * @param className - Optional CSS classes for the button
 */
export function AudioPlayButton({ text, className = '' }: AudioPlayButtonProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const handlePlay = async () => {
    try {
      setIsLoading(true);

      // Check cache first
      let audioUrl = audioCache.get(text);

      if (!audioUrl) {
        // Generate audio via POST /api/audio/generate
        const response = await api.post<{ filename: string }>('/audio/generate', { text });
        const filename = response.data.filename;
        audioUrl = `/api/audio/${filename}`;
        audioCache.set(text, audioUrl);
      }

      // Create or reuse audio element
      if (!audioRef.current) {
        audioRef.current = new Audio(audioUrl);
        audioRef.current.onplay = () => setIsPlaying(true);
        audioRef.current.onended = () => setIsPlaying(false);
        audioRef.current.onerror = () => {
          setIsPlaying(false);
          setIsLoading(false);
          console.error('Audio playback error');
        };
      } else {
        audioRef.current.src = audioUrl;
      }

      await audioRef.current.play();
    } catch (error) {
      console.error('Failed to generate or play audio:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <button
      onClick={handlePlay}
      disabled={isLoading || isPlaying}
      className={`inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors ${className}`}
      aria-label="Play audio"
    >
      {isLoading ? (
        <>
          <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
              fill="none"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          <span>Loading...</span>
        </>
      ) : isPlaying ? (
        <>
          <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z" />
          </svg>
          <span>Playing...</span>
        </>
      ) : (
        <>
          <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z" />
          </svg>
          <span>Play</span>
        </>
      )}
    </button>
  );
}
