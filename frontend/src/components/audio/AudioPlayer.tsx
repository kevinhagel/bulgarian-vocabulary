import { useRef, useState } from 'react';

interface AudioPlayerProps {
  audioUrl: string;
  label?: string;
  size?: 'sm' | 'md';
}

export default function AudioPlayer({ audioUrl, label, size = 'md' }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [hasError, setHasError] = useState(false);

  const handlePlay = () => {
    if (!audioRef.current) return;

    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      setIsLoading(true);
      audioRef.current.play()
        .catch((err) => {
          console.error('Audio playback failed:', err);
          setHasError(true);
          setIsLoading(false);
        });
    }
  };

  const handleAudioPlay = () => {
    setIsPlaying(true);
    setIsLoading(false);
  };

  const handleAudioPause = () => {
    setIsPlaying(false);
  };

  const handleAudioEnded = () => {
    setIsPlaying(false);
  };

  const handleAudioError = () => {
    setHasError(true);
    setIsLoading(false);
    setIsPlaying(false);
  };

  const buttonSize = size === 'sm' ? 'w-8 h-8 text-sm' : 'w-10 h-10 text-base';
  const iconSize = size === 'sm' ? 'w-4 h-4' : 'w-5 h-5';

  return (
    <div className="inline-flex items-center gap-2">
      <button
        onClick={handlePlay}
        disabled={isLoading || hasError}
        className={`${buttonSize} inline-flex items-center justify-center rounded-full bg-blue-500 text-white hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors`}
        aria-label={isPlaying ? 'Pause audio' : 'Play audio'}
      >
        {isLoading ? (
          <span className={iconSize}>⏳</span>
        ) : hasError ? (
          <span className={iconSize}>✕</span>
        ) : isPlaying ? (
          <svg className={iconSize} fill="currentColor" viewBox="0 0 16 16">
            <path d="M5.5 3.5A1.5 1.5 0 0 1 7 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5zm5 0A1.5 1.5 0 0 1 12 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5z"/>
          </svg>
        ) : (
          <svg className={iconSize} fill="currentColor" viewBox="0 0 16 16">
            <path d="M11.596 8.697l-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
          </svg>
        )}
      </button>

      {label && (
        <span className="text-sm text-gray-700" lang="bg">
          {label}
        </span>
      )}

      <audio
        ref={audioRef}
        src={audioUrl}
        preload="metadata"
        onPlay={handleAudioPlay}
        onPause={handleAudioPause}
        onEnded={handleAudioEnded}
        onError={handleAudioError}
      />
    </div>
  );
}
